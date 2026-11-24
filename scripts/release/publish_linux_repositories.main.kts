#!/usr/bin/env -S kotlinc -script

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest

fun run(
    arguments: List<String>,
    workingDirectory: Path? = null,
    standardInput: String? = null,
    standardOutput: Path? = null,
): Int {
    val process =
        ProcessBuilder(arguments)
            .apply {
                workingDirectory?.let { directory(it.toFile()) }
                standardOutput?.let { redirectOutput(it.toFile()) }
                redirectErrorStream(standardOutput == null)
            }.start()
    process.outputStream.bufferedWriter().use { writer -> standardInput?.let(writer::write) }
    val output = process.inputStream.bufferedReader().use { it.readText() }
    if (output.isNotBlank()) print(output)
    return process.waitFor()
}

fun requireSuccess(
    description: String,
    vararg arguments: String,
    workingDirectory: Path? = null,
    standardInput: String? = null,
    standardOutput: Path? = null,
) {
    check(run(arguments.toList(), workingDirectory, standardInput, standardOutput) == 0) {
        "Command failed while $description: ${arguments.joinToString(" ")}"
    }
}

fun sign(
    keyId: String,
    passphrase: String,
    vararg arguments: String,
) = requireSuccess(
    "signing repository metadata",
    "gpg",
    "--batch",
    "--yes",
    "--pinentry-mode",
    "loopback",
    "--passphrase-fd",
    "0",
    "--local-user",
    keyId,
    *arguments,
    standardInput = "$passphrase\n",
)

fun copyDirectory(
    source: Path,
    destination: Path,
) {
    require(Files.isDirectory(source)) { "Missing website directory: $source" }
    Files.walk(source).use { paths ->
        paths.forEach { path ->
            val target = destination.resolve(source.relativize(path))
            if (Files.isDirectory(path)) {
                Files.createDirectories(target)
            } else if (Files.isRegularFile(path)) {
                Files.createDirectories(target.parent)
                Files.copy(path, target, REPLACE_EXISTING)
            }
        }
    }
}

data class ChannelPackages(
    val version: String,
    val deb: Path,
    val rpm: Path,
)

fun channelPackages(
    channel: String,
    assets: Path,
): ChannelPackages {
    require(channel in setOf("stable", "dev"))
    val files = Files.list(assets).use { it.filter(Files::isRegularFile).toList() }
    val deb = files.single { it.fileName.toString().endsWith(".deb") }
    val rpm = files.single { it.fileName.toString().endsWith(".rpm") }
    val version =
        deb.fileName
            .toString()
            .removePrefix("azkar-")
            .removeSuffix("-linux-x64.deb")
    require(Regex("\\d+\\.\\d+\\.\\d+(-beta\\.\\d+(\\.\\d+)*)?").matches(version)) { "Invalid package version" }
    require(("-beta." in version) == (channel == "dev")) { "Wrong channel for $version" }
    require(deb.fileName.toString() == "azkar-$version-linux-x64.deb")
    require(rpm.fileName.toString() == "azkar-$version-linux-x64.rpm") { "DEB/RPM versions differ" }
    val checksums = Files.readAllLines(assets.resolve("SHA256SUMS"))
    for (file in listOf(deb, rpm)) {
        val entry = checksums.single { it.substringAfter("  ") == file.fileName.toString() }
        val expected = entry.substringBefore("  ")
        require(expected.matches(Regex("[0-9a-f]{64}")))
        val hash = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use { stream ->
            val buffer = ByteArray(64 * 1024)
            var count = stream.read(buffer)
            while (count >= 0) {
                hash.update(buffer, 0, count)
                count = stream.read(buffer)
            }
        }
        require(hash.digest().joinToString("") { "%02x".format(it) } == expected) { "Checksum mismatch: $file" }
    }
    return ChannelPackages(version, deb, rpm)
}

fun buildChannel(
    channel: String,
    packages: ChannelPackages,
    pages: Path,
    keyId: String,
    passphrase: String,
) {
    val apt = pages.resolve("apt")
    val aptPool = apt.resolve("pool/$channel/main/a/azkar")
    val aptBinary = apt.resolve("dists/$channel/main/binary-amd64")
    val rpm = pages.resolve("rpm/$channel")
    Files.createDirectories(aptPool)
    Files.createDirectories(aptBinary)
    Files.createDirectories(rpm)
    Files.copy(packages.deb, aptPool.resolve(packages.deb.fileName), REPLACE_EXISTING)
    Files.copy(packages.rpm, rpm.resolve(packages.rpm.fileName), REPLACE_EXISTING)

    val index = aptBinary.resolve("Packages")
    requireSuccess(
        "building APT package metadata",
        "apt-ftparchive",
        "packages",
        "pool/$channel",
        workingDirectory = apt,
        standardOutput = index,
    )
    requireSuccess("compressing APT package metadata", "gzip", "-9fk", index.toString())
    val release = apt.resolve("dists/$channel/Release")
    requireSuccess(
        "building the APT release index",
        "apt-ftparchive",
        "-o",
        "APT::FTPArchive::Release::Origin=Azkar",
        "-o",
        "APT::FTPArchive::Release::Label=Azkar",
        "-o",
        "APT::FTPArchive::Release::Suite=$channel",
        "-o",
        "APT::FTPArchive::Release::Codename=$channel",
        "-o",
        "APT::FTPArchive::Release::Architectures=amd64",
        "-o",
        "APT::FTPArchive::Release::Components=main",
        "release",
        "dists/$channel",
        workingDirectory = apt,
        standardOutput = release,
    )
    sign(
        keyId,
        passphrase,
        "--clearsign",
        "--output",
        apt.resolve("dists/$channel/InRelease").toString(),
        release.toString(),
    )
    sign(
        keyId,
        passphrase,
        "--armor",
        "--detach-sign",
        "--output",
        apt.resolve("dists/$channel/Release.gpg").toString(),
        release.toString(),
    )
    requireSuccess("building RPM repository metadata", "createrepo_c", ".", workingDirectory = rpm)
    val rpmMetadata = rpm.resolve("repodata/repomd.xml")
    val rpmSignature = rpm.resolve("repodata/repomd.xml.asc")
    sign(keyId, passphrase, "--armor", "--detach-sign", "--output", rpmSignature.toString(), rpmMetadata.toString())
    requireSuccess("verifying the APT signature", "gpg", "--verify", apt.resolve("dists/$channel/InRelease").toString())
    requireSuccess(
        "verifying the APT detached signature",
        "gpg",
        "--verify",
        apt.resolve("dists/$channel/Release.gpg").toString(),
        release.toString(),
    )
    requireSuccess("verifying the RPM signature", "gpg", "--verify", rpmSignature.toString(), rpmMetadata.toString())
    Files.writeString(pages.resolve("$channel-version.txt"), "${packages.version}\n")
}

fun copyWebsite(
    website: Path,
    pages: Path,
) {
    val required = listOf("download.html", "css/download.css", "js/theme.js", "js/download.js")
    required.forEach { asset ->
        require(
            Files.isRegularFile(website.resolve(asset)),
        ) { "Website asset missing: $asset. Run bun run build in site/." }
    }
    listOf("css", "js", "images").forEach { directory ->
        copyDirectory(website.resolve(directory), pages.resolve(directory))
    }
    val downloadPage = website.resolve("download.html")
    Files.copy(downloadPage, pages.resolve("download.html"), REPLACE_EXISTING)
    Files.copy(downloadPage, pages.resolve("index.html"), REPLACE_EXISTING)
}

fun publish(
    assets: Path,
    pages: Path,
    publicKey: Path,
    keyId: String,
    passphrase: String,
) {
    require(Files.isDirectory(assets)) { "Missing repository inputs: $assets" }
    require(!Files.exists(pages) || Files.list(pages).use { !it.findAny().isPresent }) { "Site output must be empty" }
    require(Files.isRegularFile(publicKey)) { "Missing exported repository public key" }
    require(keyId.matches(Regex("[A-Fa-f0-9]{16,}"))) { "Invalid repository signing key fingerprint" }
    val channels = listOf("stable", "dev").filter { Files.isDirectory(assets.resolve(it)) }
    require(channels.isNotEmpty()) { "No repository packages" }
    // Validate both channels before writing any output.
    val packages = channels.associateWith { channelPackages(it, assets.resolve(it)) }
    val workspace = System.getenv("GITHUB_WORKSPACE") ?: error("GITHUB_WORKSPACE is required")
    copyWebsite(Path.of(workspace, "site", "build"), pages)
    Files.createDirectories(pages.resolve("keys"))
    Files.copy(publicKey, pages.resolve("keys/azkar-repo.asc"), REPLACE_EXISTING)
    packages.forEach { (channel, files) -> buildChannel(channel, files, pages, keyId, passphrase) }
    Files.writeString(pages.resolve(".nojekyll"), "")
    val size = Files.walk(pages).use { it.filter(Files::isRegularFile).mapToLong(Files::size).sum() }
    require(size < 900L * 1024 * 1024) { "Repository site approaches the Pages 1 GB limit: $size bytes" }
    println("Built signed rolling repositories ($size bytes); no Git commits or network publication performed")
}

fun selfTest() {
    val root = Files.createTempDirectory("azkar-repository-test-")
    try {
        fun fixture(
            directory: String,
            version: String,
        ): Path {
            val assets = Files.createDirectories(root.resolve(directory))
            val sums =
                listOf("deb", "rpm").map { extension ->
                    val file = Files.writeString(assets.resolve("azkar-$version-linux-x64.$extension"), extension)
                    val hash =
                        MessageDigest
                            .getInstance(
                                "SHA-256",
                            ).digest(Files.readAllBytes(file))
                            .joinToString("") { "%02x".format(it) }
                    "$hash  ${file.fileName}"
                }
            Files.writeString(assets.resolve("SHA256SUMS"), sums.joinToString("\n", postfix = "\n"))
            return assets
        }
        val stable = fixture("stable", "1.2.3")
        val dev = fixture("dev", "1.2.4-beta.5.1")
        check(channelPackages("stable", stable).version == "1.2.3")
        check(channelPackages("dev", dev).version == "1.2.4-beta.5.1")
        check(runCatching { channelPackages("stable", dev) }.isFailure)
        Files.writeString(dev.resolve("azkar-1.2.4-beta.5.1-linux-x64.deb"), "tampered")
        check(runCatching { channelPackages("dev", dev) }.isFailure)
        Files.writeString(stable.resolve("azkar-1.2.2-linux-x64.deb"), "old package")
        check(runCatching { channelPackages("stable", stable) }.isFailure)
        val website = root.resolve("website")
        val pages = Files.createDirectories(root.resolve("pages"))
        val publicFiles =
            listOf("download.html", "css/download.css", "js/theme.js", "js/download.js", "images/preview.webp")
        (publicFiles + listOf("src/download.ts", "node_modules/dependency/index.js", "package.json")).forEach { name ->
            val file = website.resolve(name)
            Files.createDirectories(file.parent)
            Files.writeString(file, name)
        }
        copyWebsite(website, pages)
        check(publicFiles.all { Files.readString(pages.resolve(it)) == it })
        check(Files.readString(pages.resolve("index.html")) == "download.html")
        check(listOf("src", "node_modules", "package.json").none { Files.exists(pages.resolve(it)) })
        Files.delete(website.resolve("js/download.js"))
        check(runCatching { copyWebsite(website, pages) }.isFailure)
        println("Repository input self-tests passed")
    } finally {
        Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::delete) }
    }
}

if (args.contentEquals(arrayOf("self-test"))) {
    selfTest()
} else {
    require(
        args.size == 4,
    ) { "Usage: publish_linux_repositories.main.kts INPUTS SITE PUBLIC_KEY FINGERPRINT | self-test" }
    val passphrase = System.getenv("REPO_PASSPHRASE") ?: error("REPO_PASSPHRASE is required")
    publish(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]), args[3], passphrase)
}
