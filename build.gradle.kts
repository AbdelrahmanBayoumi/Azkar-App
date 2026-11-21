/*
 * The root project owns the sources that are shared by every module and therefore cannot live
 * inside any single one of them: release scripts and the log-capture helper in
 * `config/testing`, which `azkar-common` compiles into the test source set of all three modules.
 *
 * The formatting configuration is duplicated from that convention plugin rather than shared,
 * because Spotless refuses any target outside the project directory it is configured on: a module
 * cannot format `config/testing`, so the root project has to. Both sides rely on the repository
 * `.editorconfig` for the style itself (`ktlint_official`, 120 columns), so the two configurations
 * cannot drift in behaviour.
 *
 * `config/detekt` and `config/logback` are not listed here because they are XML, which Spotless
 * does not format.
 */
import com.diffplug.spotless.LineEnding

// Use the shared version catalog so root formatting matches the convention plugin.
plugins {
    // Load the Kotlin Gradle plugin once at the root so subprojects share its classloader.
    alias(libs.plugins.org.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.com.diffplug.spotless)
}

// Spotless resolves the ktlint CLI from here, exactly as `azkar-common` does for every module.
repositories { mavenCentral() }

spotless {
    // The repository stores LF and checks out LF — `.gitattributes` says so — so the formatter
    // asks for LF as well. Left to its default, Spotless takes the line endings from git's
    // configuration, which differs between a Linux and a Windows machine, and the two then
    // disagree about the same file.
    lineEndings = LineEnding.UNIX

    kotlin {
        target(
            "config/testing/**/*.kt",
            "scripts/**/*.main.kts",
            "build-logic/build.gradle.kts",
            "build-logic/settings.gradle.kts",
            "build-logic/src/main/kotlin/*.gradle.kts",
            "build-logic/src/main/kotlin/azkar/buildlogic/AzkarInstallerPlatform.kt",
            "build-logic/src/main/kotlin/azkar/**/*.kt",
            "build-logic/src/test/kotlin/**/*.kt",
        )
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
}

// Mirrors the local/CI fork in `azkar-common`: a local run rewrites, CI verifies.
tasks.build {
    val isCI = providers.environmentVariable("CI").isPresent
    if (isCI) {
        dependsOn(tasks.named("spotlessCheck"))
    } else {
        dependsOn(tasks.named("spotlessApply"))
    }
}

// Included-build tests are not part of the application's normal test task graph.
tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":test"))
}
