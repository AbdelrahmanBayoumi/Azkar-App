#!/usr/bin/env -S kotlinc -script

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

val requiredChecks = listOf("windowsX64", "macosX64", "macosArm64", "deb", "rpm", "appImageArch")
val distributionPaths =
    listOf(
        ".github/workflows",
        "scripts",
        "build-logic",
        "app/build.gradle.kts",
        "app/packaging",
        "build.gradle.kts",
        "settings.gradle.kts",
        "gradle",
        "assets",
        "app/src/main/kotlin/com/azkar/updates",
        "app/src/main/kotlin/com/azkar/Main.kt",
        "app/src/main/kotlin/com/azkar/PackagingSmokeTest.kt",
        "app/src/main/kotlin/com/azkar/ui/notifications/StartupRegistration.kt",
        "app/src/main/kotlin/com/azkar/ui/shell/ReminderTray.kt",
    )

fun git(
    root: Path,
    vararg arguments: String,
): Pair<Int, String> {
    val process = ProcessBuilder("git", *arguments).directory(root.toFile()).redirectErrorStream(true).start()
    val output =
        process.inputStream
            .bufferedReader()
            .use { it.readText() }
            .trim()
    return process.waitFor() to output
}

fun verifyEvidence(root: Path) {
    val evidence =
        Properties().apply {
            Files.newBufferedReader(root.resolve("docs/release-validation.properties")).use { load(it) }
        }
    val commit = evidence.getProperty("sourceCommit").orEmpty()
    require(commit.matches(Regex("[0-9a-f]{40}"))) {
        "Record the full manually tested source commit before Stable publication"
    }
    require(
        evidence.getProperty("artifactVersion").orEmpty().matches(Regex("\\d+\\.\\d+\\.\\d+(-beta\\.\\d+(\\.\\d+)*)?")),
    ) {
        "Record the tested installer version"
    }
    requiredChecks.forEach { key ->
        require(evidence.getProperty(key) == "passed") { "Missing installed-release validation: $key" }
    }
    require(evidence.getProperty("evidence").orEmpty().length >= 20) {
        "Link or describe the recorded install/update evidence"
    }
    require(git(root, "merge-base", "--is-ancestor", commit, "HEAD").first == 0) {
        "Tested commit is not an ancestor of this release"
    }
    require(git(root, "diff", "--quiet", commit, "HEAD", "--", *distributionPaths.toTypedArray()).first == 0) {
        "Packaging/updater sources changed after installed-release validation; repeat the checklist"
    }
    println("Installed-release evidence accepted for $commit (${evidence.getProperty("artifactVersion")})")
}

fun selfTest() {
    val root = Files.createTempDirectory("azkar-evidence-test-")

    fun checkedGit(vararg arguments: String): String {
        val (result, output) = git(root, *arguments)
        check(result == 0) { output }
        return output
    }
    try {
        checkedGit("init", "-q")
        checkedGit("config", "user.name", "Test")
        checkedGit("config", "user.email", "test@example.invalid")
        Files.createDirectories(root.resolve("docs"))
        val installHook = root.resolve("app/packaging/linux/gnomeExtension/scripts/gnome-extension-after-install.sh")
        Files.createDirectories(installHook.parent)
        Files.writeString(installHook, "tested")
        checkedGit("add", ".")
        checkedGit("commit", "-qm", "tested sources")
        val commit = checkedGit("rev-parse", "HEAD")
        val record = root.resolve("docs/release-validation.properties")
        val valid =
            "sourceCommit=$commit\nartifactVersion=1.2.3-beta.4.1\n" +
                "evidence=Recorded disposable-system install and update checks\n" +
                requiredChecks.joinToString("\n", postfix = "\n") { "$it=passed" }
        Files.writeString(record, valid)
        verifyEvidence(root)
        Files.writeString(record, valid.replace("deb=passed", "deb=pending"))
        check(runCatching { verifyEvidence(root) }.isFailure)
        Files.writeString(record, valid.replace(commit, "0".repeat(40)))
        check(runCatching { verifyEvidence(root) }.isFailure)
        Files.writeString(record, valid)
        Files.writeString(installHook, "changed")
        checkedGit("add", ".")
        checkedGit("commit", "-qm", "changed packaging")
        check(runCatching { verifyEvidence(root) }.isFailure)
        println("Installed-release evidence self-tests passed")
    } finally {
        Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::delete) }
    }
}

if (args.contentEquals(arrayOf("self-test"))) {
    selfTest()
} else {
    require(args.isEmpty()) { "Usage: check_release_validation.main.kts [self-test]" }
    verifyEvidence(Path.of(System.getProperty("user.dir")))
}
