package com.azkar.ui.shell

import com.azkar.settings.SettingsStore
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}
private const val EXTENSION_UUID = "azkar-prayers@azkar.app"
private const val EXTENSION_DIRECTORY = "gnome-shell/extensions/$EXTENSION_UUID"
private const val BUNDLE_MARKER = ".azkar-bundle"
private const val METADATA = "metadata.json"
private val extensionFiles = listOf("extension.js", "stylesheet.css", "azkar.png", METADATA)

internal fun provisionGnomePrayerExtension() {
    runCatching {
        val systemDirectories =
            System.getenv("XDG_DATA_DIRS")?.takeIf { it.isNotBlank() } ?: "/usr/local/share:/usr/share"
        val systemInstalled =
            systemDirectories.split(':').any {
                Files.isRegularFile(Path.of(it, EXTENSION_DIRECTORY, METADATA))
            }
        val userDirectory = gnomeUserExtensionDirectory()
        if (!systemInstalled && !System.getenv("APPIMAGE").isNullOrBlank()) {
            installAppImageExtension(userDirectory)
        }
        if (systemInstalled || Files.isRegularFile(userDirectory.resolve(METADATA))) {
            enableBundledExtensionOnce()
        }
    }.onFailure {
        logger.debug(it) { "Could not set up the bundled GNOME prayer extension" }
    }
}

private fun gnomeUserExtensionDirectory(): Path {
    val dataHome =
        System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }?.let(Path::of)
            ?: Path.of(System.getProperty("user.home"), ".local", "share")
    return dataHome.resolve(EXTENSION_DIRECTORY)
}

private fun installAppImageExtension(destination: Path) {
    val resources = System.getProperty("compose.application.resources.dir") ?: return
    val source = Path.of(resources, "gnome", EXTENSION_UUID)
    val marker = destination.resolve(BUNDLE_MARKER)
    val managed = Files.exists(marker) || Files.notExists(destination.resolve(METADATA))
    if (!managed || extensionFiles.any { !Files.isRegularFile(source.resolve(it)) }) return
    Files.createDirectories(destination)
    Files.writeString(marker, "")
    // Publish metadata last so a new installation is discoverable only after its files are in place.
    extensionFiles.forEach { name ->
        val bundled = source.resolve(name)
        val installed = destination.resolve(name)
        if (Files.notExists(installed) || Files.mismatch(bundled, installed) != -1L) {
            val staged = Files.createTempFile(destination, ".azkar-", ".tmp")
            try {
                Files.copy(bundled, staged, REPLACE_EXISTING)
                Files.move(staged, installed, ATOMIC_MOVE, REPLACE_EXISTING)
            } finally {
                Files.deleteIfExists(staged)
            }
        }
    }
}

private fun enableBundledExtensionOnce() {
    val marker = SettingsStore.appDataDirectory().resolve("gnome-prayer-extension-enabled")
    // An explicit disable in GNOME takes precedence over Azkar's default.
    val disabled = runGnomeCommand("gsettings", "get", "org.gnome.shell", "disabled-extensions") ?: return
    if (Files.exists(marker) || "'$EXTENSION_UUID'" in disabled) return
    // A running shell may discover the files only after the next sign-in.
    if (runGnomeCommand("gnome-extensions", "enable", EXTENSION_UUID) != null) {
        Files.createDirectories(marker.parent)
        Files.writeString(marker, "")
    }
}

private fun runGnomeCommand(vararg command: String): String? {
    val process = ProcessBuilder(*command).redirectError(ProcessBuilder.Redirect.DISCARD).start()
    return try {
        if (process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0) {
            process.inputStream.bufferedReader().use { it.readText() }
        } else {
            null
        }
    } finally {
        process.destroyForcibly()
    }
}
