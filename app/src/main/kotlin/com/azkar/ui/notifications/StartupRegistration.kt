package com.azkar.ui.notifications

import dev.nucleusframework.autolaunch.AutoLaunch
import dev.nucleusframework.autolaunch.AutoLaunchConfig
import dev.nucleusframework.autolaunch.AutoLaunchResult
import dev.nucleusframework.autolaunch.AutoLaunchState
import dev.nucleusframework.core.runtime.ExecutableRuntime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path

/** Adapts Nucleus's native login registration to the settings UI. */
class StartupRegistration internal constructor(
    private val readState: () -> AutoLaunchState = AutoLaunch::state,
    private val changeEnabled: (
        Boolean,
    ) -> AutoLaunchResult = { if (it) AutoLaunch.enable() else AutoLaunch.disable() },
    private val legacyPaths: List<Path> =
        if (System.getProperty("nucleus.app.id") == "com.azkar.app") legacyRegistrationPaths() else emptyList(),
    private val packaged: Boolean = configurePackagedLauncher(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()

    internal val isDevelopmentBuild: Boolean get() = !packaged

    suspend fun state(): Result<AutoLaunchState> =
        withContext(ioDispatcher) {
            mutex.withLock {
                runCatching { if (packaged) readState() else AutoLaunchState.UNSUPPORTED }
            }
        }

    suspend fun setEnabled(enabled: Boolean): Result<AutoLaunchState> =
        withContext(ioDispatcher) {
            mutex.withLock {
                runCatching {
                    if (!packaged) throw StartupRegistrationException(AutoLaunchResult.UNSUPPORTED)
                    val result = changeEnabled(enabled)
                    if (result != AutoLaunchResult.OK && result != AutoLaunchResult.UNCHANGED) {
                        throw StartupRegistrationException(result)
                    }
                    val state = readState()
                    check(state != AutoLaunchState.UNSUPPORTED && state.isStartupEnabled == enabled) {
                        "The login manager did not apply the startup setting."
                    }
                    removeLegacyRegistrations()
                    state
                }
            }
        }

    /** Keep an existing opt-in, removing old launch files only after the native backend succeeds. */
    suspend fun migrateLegacyRegistration(wasEnabled: Boolean): Result<Unit> =
        withContext(ioDispatcher) {
            mutex.withLock {
                runCatching {
                    if (!packaged) return@runCatching
                    val legacy = legacyPaths.filter(::isLegacyRegistration)
                    if (legacy.isEmpty()) return@runCatching
                    when (readState()) {
                        AutoLaunchState.DISABLED -> {
                            if (wasEnabled && legacy.any(::isLegacyEnabled)) {
                                val result = changeEnabled(true)
                                check(result == AutoLaunchResult.OK || result == AutoLaunchResult.UNCHANGED) {
                                    "Could not migrate login registration: $result"
                                }
                                check(
                                    readState().isStartupEnabled,
                                ) { "The migrated login registration is not enabled." }
                            }
                        }
                        AutoLaunchState.UNSUPPORTED ->
                            error(
                                "The native login manager is unavailable; keeping the old registration.",
                            )
                        // A system-level user or policy decision takes precedence over the old preference.
                        else -> Unit
                    }
                    removeLegacyRegistrations()
                }
            }
        }

    suspend fun openSystemSettings(): Boolean =
        withContext(ioDispatcher) { runCatching { AutoLaunch.openSystemSettings() }.getOrDefault(false) }

    private fun removeLegacyRegistrations() {
        legacyPaths.filter(::isLegacyRegistration).forEach(Files::deleteIfExists)
    }

    private fun isLegacyRegistration(path: Path): Boolean =
        Files.isRegularFile(path) && Files.readString(path).contains("--start-hidden")

    private fun isLegacyEnabled(path: Path): Boolean =
        Files.readString(path).lineSequence().none {
            it.trim() in setOf("Hidden=true", "X-GNOME-Autostart-enabled=false")
        }
}

private fun configurePackagedLauncher(): Boolean {
    // jpackage supplies the actual launcher path even for an app image without installer metadata.
    val launcher = System.getProperty("jpackage.app-path")
    if (launcher.isNullOrBlank()) return !ExecutableRuntime.isDev()
    if (DesktopPlatform.current == DesktopPlatform.WINDOWS) {
        AutoLaunchConfig.executablePath = launcher
    }
    return true
}

internal val AutoLaunchState.isStartupEnabled: Boolean
    get() = this == AutoLaunchState.ENABLED || this == AutoLaunchState.ENABLED_BY_POLICY

internal class StartupRegistrationException(
    val result: AutoLaunchResult,
) : IllegalStateException("Could not update login registration: $result")

/** Only used to retire registrations written by earlier Azkar versions. */
private fun legacyRegistrationPaths(): List<Path> =
    when (DesktopPlatform.current) {
        DesktopPlatform.WINDOWS ->
            listOfNotNull(System.getenv("APPDATA"), System.getenv("USER_APPDATA"))
                .filter(String::isNotBlank)
                .distinct()
                .map { Path.of(it, "Microsoft/Windows/Start Menu/Programs/Startup/Azkar.cmd") }
        DesktopPlatform.MACOS ->
            listOf(
                Path.of(System.getProperty("user.home"), "Library/LaunchAgents/com.azkar.app.plist"),
            )
        DesktopPlatform.LINUX -> listOf(Path.of(System.getProperty("user.home"), ".config/autostart/azkar.desktop"))
        DesktopPlatform.UNKNOWN -> emptyList()
    }
