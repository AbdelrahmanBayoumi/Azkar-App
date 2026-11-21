package com.azkar.ui.notifications

import com.azkar.settings.FakePreferencesDataStore
import com.azkar.settings.SettingsStore
import de.infix.testBalloon.framework.core.testSuite
import dev.nucleusframework.autolaunch.AutoLaunchResult
import dev.nucleusframework.autolaunch.AutoLaunchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration.Companion.milliseconds

// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val StartupRegistrationSuite by testSuite("Native login registration") {
    test("successful enable verifies OS state and retires the legacy launcher") {
        withStartup { fixture ->
            fixture.writeLegacyRegistration()

            val result = fixture.registration.setEnabled(true)

            assertThat(result.getOrThrow()).isEqualTo(AutoLaunchState.ENABLED)
            assertThat(fixture.changes).containsExactly(true)
            assertThat(Files.exists(fixture.legacyPath)).isFalse()
        }
    }

    test("successful disable also removes a legacy registration") {
        withStartup { fixture ->
            fixture.state = AutoLaunchState.ENABLED
            fixture.writeLegacyRegistration()

            assertThat(fixture.registration.setEnabled(false).getOrThrow()).isEqualTo(AutoLaunchState.DISABLED)
            assertThat(fixture.changes).containsExactly(false)
            assertThat(Files.exists(fixture.legacyPath)).isFalse()
        }
    }

    for (result in listOf(
        AutoLaunchResult.BLOCKED_BY_USER,
        AutoLaunchResult.BLOCKED_BY_POLICY,
        AutoLaunchResult.UNSUPPORTED,
        AutoLaunchResult.ERROR,
    )) {
        test("a $result response fails without deleting the old launcher") {
            withStartup { fixture ->
                fixture.writeLegacyRegistration()
                fixture.result = result

                val failure = fixture.registration.setEnabled(true).exceptionOrNull()

                assertThat(failure).isInstanceOf(StartupRegistrationException::class.java)
                assertThat((failure as StartupRegistrationException).result).isEqualTo(result)
                assertThat(Files.exists(fixture.legacyPath)).isTrue()
            }
        }
    }

    test("a native success without the requested OS state is still a failure") {
        withStartup { fixture ->
            fixture.writeLegacyRegistration()
            fixture.result = AutoLaunchResult.OK

            assertThat(fixture.registration.setEnabled(true).isFailure).isTrue()
            assertThat(Files.exists(fixture.legacyPath)).isTrue()
        }
    }

    test("migration preserves an existing opt-in without leaving duplicate login entries") {
        withStartup { fixture ->
            fixture.writeLegacyRegistration()

            fixture.registration.migrateLegacyRegistration(wasEnabled = true).getOrThrow()

            assertThat(fixture.changes).containsExactly(true)
            assertThat(Files.exists(fixture.legacyPath)).isFalse()
        }
    }

    test("a stale saved preference alone never opts the user back in") {
        withStartup { fixture ->
            fixture.registration.migrateLegacyRegistration(wasEnabled = true).getOrThrow()

            assertThat(fixture.changes).isEmpty()
            assertThat(fixture.state).isEqualTo(AutoLaunchState.DISABLED)
        }
    }

    test("migration retires a stale launcher without opting in when the preference was off") {
        withStartup { fixture ->
            fixture.writeLegacyRegistration()

            fixture.registration.migrateLegacyRegistration(wasEnabled = false).getOrThrow()

            assertThat(fixture.changes).isEmpty()
            assertThat(Files.exists(fixture.legacyPath)).isFalse()
        }
    }

    test("migration respects an external user disable") {
        withStartup { fixture ->
            fixture.writeLegacyRegistration()
            fixture.state = AutoLaunchState.DISABLED_BY_USER

            fixture.registration.migrateLegacyRegistration(wasEnabled = true).getOrThrow()

            assertThat(fixture.changes).isEmpty()
            assertThat(fixture.state).isEqualTo(AutoLaunchState.DISABLED_BY_USER)
            assertThat(Files.exists(fixture.legacyPath)).isFalse()
        }
    }

    test("migration respects a Linux startup entry disabled outside the app") {
        withStartup { fixture ->
            fixture.writeLegacyRegistration()
            withContext(Dispatchers.IO) {
                Files.writeString(fixture.legacyPath, Files.readString(fixture.legacyPath) + "Hidden=true\n")
            }

            fixture.registration.migrateLegacyRegistration(wasEnabled = true).getOrThrow()

            assertThat(fixture.changes).isEmpty()
            assertThat(fixture.state).isEqualTo(AutoLaunchState.DISABLED)
        }
    }

    test("a failed migration keeps the working legacy launcher") {
        withStartup { fixture ->
            fixture.writeLegacyRegistration()
            fixture.result = AutoLaunchResult.ERROR

            assertThat(fixture.registration.migrateLegacyRegistration(wasEnabled = true).isFailure).isTrue()
            assertThat(Files.exists(fixture.legacyPath)).isTrue()
        }
    }

    test("cleanup leaves a file without the legacy launch marker alone") {
        withStartup { fixture ->
            withContext(Dispatchers.IO) { Files.writeString(fixture.legacyPath, "A user-managed startup entry") }

            fixture.registration.setEnabled(true).getOrThrow()

            val saved = withContext(Dispatchers.IO) { Files.readString(fixture.legacyPath) }
            assertThat(saved).isEqualTo("A user-managed startup entry")
        }
    }

    test("development launches cannot change login registration") {
        val registration =
            StartupRegistration(
                readState = { error("Must not query the developer's login manager") },
                changeEnabled = { error("Must not change the developer's login manager") },
                legacyPaths = emptyList(),
                packaged = false,
            )
        assertThat(registration.state().getOrThrow()).isEqualTo(AutoLaunchState.UNSUPPORTED)
        assertThat(registration.setEnabled(true).isFailure).isTrue()
        assertThat(registration.migrateLegacyRegistration(wasEnabled = true).isSuccess).isTrue()
    }

    test("settings display the OS state even when the saved preference disagrees") {
        withStartupEditor { fixture, preferences, editor ->
            preferences.setStartOnLogin(true)

            editor.refreshStartOnLogin()

            assertThat(editor.startOnLogin).isFalse()
            fixture.state = AutoLaunchState.ENABLED_BY_POLICY
            editor.refreshStartOnLogin()
            assertThat(editor.startOnLogin).isTrue()
            assertThat(editor.canChangeStartup).isFalse()
            assertThat(editor.notice).isEqualTo(StartupNotice.ManagedByPolicy)
        }
    }

    test("a blocked request reports system settings and leaves the saved preference unchanged") {
        withStartupEditor { fixture, preferences, editor ->
            fixture.state = AutoLaunchState.DISABLED_BY_USER
            fixture.result = AutoLaunchResult.BLOCKED_BY_USER

            editor.setStartOnLogin(true)
            awaitStartupChange(editor)

            assertThat(preferences.startOnLogin.value).isFalse()
            assertThat(editor.startOnLogin).isFalse()
            assertThat(editor.canChangeStartup).isFalse()
            assertThat(editor.notice).isEqualTo(StartupNotice.DisabledByUser)
        }
    }

    test("a successful request updates both the OS reading and the saved preference") {
        withStartupEditor { _, preferences, editor ->
            editor.setStartOnLogin(true)
            awaitStartupChange(editor)

            assertThat(preferences.startOnLogin.value).isTrue()
            assertThat(editor.startOnLogin).isTrue()
            assertThat(editor.notice).isNull()
        }
    }
}

private class StartupFixture {
    val directory: Path = Files.createTempDirectory("azkar-startup-test")
    val legacyPath: Path = directory.resolve("azkar.desktop")
    var state = AutoLaunchState.DISABLED
    var result: AutoLaunchResult? = null
    val changes = mutableListOf<Boolean>()
    val registration =
        StartupRegistration(
            readState = { state },
            changeEnabled = { enabled ->
                changes += enabled
                result ?: run {
                    state = if (enabled) AutoLaunchState.ENABLED else AutoLaunchState.DISABLED
                    AutoLaunchResult.OK
                }
            },
            legacyPaths = listOf(legacyPath),
            packaged = true,
        )

    fun writeLegacyRegistration() {
        Files.writeString(legacyPath, "[Desktop Entry]\nName=Azkar\nExec=\"/opt/azkar/bin/azkar\" --start-hidden\n")
    }
}

private suspend fun withStartup(block: suspend (StartupFixture) -> Unit) =
    withContext(Dispatchers.IO) {
        val fixture = StartupFixture()
        try {
            block(fixture)
        } finally {
            fixture.directory.toFile().deleteRecursively()
        }
    }

private suspend fun withStartupEditor(
    block: suspend (
        StartupFixture,
        NotificationPreferencesRepository,
        NotificationSettingsState.NotificationEditor,
    ) -> Unit,
) {
    withStartup { fixture ->
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val preferences = NotificationPreferencesRepository(SettingsStore(FakePreferencesDataStore(), scope))
            val state = NotificationSettingsState(scope, preferences, startupRegistration = fixture.registration)
            block(fixture, preferences, state.notifications)
        } finally {
            scope.cancel()
        }
    }
}

private suspend fun awaitStartupChange(editor: NotificationSettingsState.NotificationEditor) {
    withTimeout(5_000.milliseconds) {
        while (editor.isUpdatingStartup) delay(10.milliseconds)
    }
}
