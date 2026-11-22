package com.azkar

import com.azkar.data.config.DatabaseManager
import com.azkar.ui.notifications.DesktopPlatform
import com.azkar.ui.notifications.StartupRegistration
import dev.nucleusframework.autolaunch.AutoLaunch
import dev.nucleusframework.autolaunch.AutoLaunchState
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path

internal suspend fun verifyPackagedRuntime() {
    val profile = Path.of(checkNotNull(System.getProperty("azkar.packaging.smoke.profile")))
    check(Files.isSameFile(profile, Path.of(System.getProperty("user.home"))))
    check(Files.isRegularFile(profile.resolve(".packaging-smoke-test")))
    if (DesktopPlatform.current == DesktopPlatform.LINUX) {
        check(ModuleLayer.boot().findModule("jdk.security.auth").isPresent) {
            "The bundled Linux runtime needs jdk.security.auth for the D-Bus connection"
        }
    }
    if (DesktopPlatform.current == DesktopPlatform.WINDOWS) {
        check(StartupRegistration().state().getOrThrow() != AutoLaunchState.UNSUPPORTED) {
            "Windows startup registration is unavailable in the release runtime: ${AutoLaunch.diagnostic()}"
        }
    }
    DatabaseManager().use { it.warmUp() }
    KotlinLogging.logger {}.info { "AZKAR_PACKAGING_SMOKE_OK" }
}
