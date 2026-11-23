import azkar.buildlogic.AzkarInstallerPlatform
import azkar.buildlogic.macPackageVersion
import groovy.json.JsonOutput
import org.gradle.api.artifacts.ExternalModuleDependency
import java.util.jar.JarFile

plugins {
    id("azkar-app")
    alias(libs.plugins.io.github.takahirom.roborazzi)
}

// The release tag has to match this number exactly, without the `v`: the release workflow
// checks it and refuses to publish otherwise.
val defaultAppVersion = "0.1.1"
version = providers.gradleProperty("appVersion").orElse(defaultAppVersion).get()

// The desktop entry point, in one place.
//
// Three separate things have to agree on this string, and none of them can check
// the others:
//   - the launcher inside the package (`app.jvm.gui.main-class`),
//   - the `StartupWMClass` the Linux desktop entry declares, which the desktop
//     manager matches against the running window's `WM_CLASS`, and
//   - the `WM_CLASS` AWT reports, which it derives from the class that owns `main`.
//
// Nucleus writes the first two from the configured `mainClass` below, so
// what is left to keep true is the third, and `Main.kt` owns that. The test
// `DesktopWindowClassSuite` holds the two sides to that agreement.
val desktopMainClass = "com.azkar.MainKt"
val installerPlatform = AzkarInstallerPlatform.current()

kotlin {
    composeCompiler {
        includeSourceInformation = true
    }
    compilerOptions {
        // The Compose Styles API (`androidx.compose.foundation.style.Style`,
        // `Modifier.styleable`, `rememberUpdatedStyleState`) is still flagged experimental and
        // every use of it is an error until the module opts in.
        //
        // It is declared here rather than in `azkar-common` because `:data` and `:domain` have no
        // Compose dependency at all — the flag would name a package those modules cannot resolve.
        //
        // The skill's own prerequisites also ask for `compileSdk 37` / Compose BOM `2026.04.01`.
        // Those are Android-only and do not apply to this build; the desktop foundation artifact
        // (`foundation-desktop`) ships the API on its own, which is why only the opt-in is needed.
        freeCompilerArgs.add("-opt-in=androidx.compose.foundation.style.ExperimentalFoundationStyleApi")
    }
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    // Detekt runs its own Kotlin analysis rather than reusing KotlinCompile's arguments. Keep it
    // aligned with the module compiler opt-in so Compose Styles API usages resolve cleanly.
    optIn.add("androidx.compose.foundation.style.ExperimentalFoundationStyleApi")
}

java {
    // Skiko (the Compose Desktop renderer) is not a JPMS module and needs JNI
    // access to unpack its native runtime, so `:app` runs from the classpath.
    // `azkar-common` turns module-path inference on for every module; `:domain`
    // and `:data` keep their descriptors, `:app` opts out — which is why
    // `app/src/main/java/module-info.java` is deleted.
    modularity.inferModulePath = false
}

repositories {
    // Compose Multiplatform's desktop artifacts delegate part of their runtime
    // graph (`lifecycle-runtime-compose`, `savedstate-compose`) to the upstream
    // `androidx.*` coordinates, which are only published to Google's Maven.
    // Scoped by group so every other dependency still resolves from Maven Central.
    google {
        content {
            includeGroupByRegex("androidx\\..*")
            includeGroupByRegex("com\\.google\\.android.*")
        }
    }
}

dependencies {
    implementation(project(":data"))
    implementation(project(":domain"))

    // Compose Multiplatform (desktop only — the JVM target is the sole target)
    implementation(libs.org.jetbrains.compose.ui)
    implementation(libs.org.jetbrains.compose.foundation)
    // skiko-awt's Gradle metadata strictly aligns the native runtime with the renderer.
    runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-${installerPlatform.id}")
    implementation(libs.org.jetbrains.compose.components.resources)
    implementation(libs.org.jetbrains.compose.material3)
    implementation(libs.org.jetbrains.compose.material.icons.extended)
    // `@Preview` / tooling annotations. `debugImplementation` of ui-tooling would
    // add the live-reload devtools; the preview artifact itself is tiny and lets
    // the IDE render `@Preview` composables without a running app.
    implementation(libs.org.jetbrains.compose.ui.tooling.preview)
    // JavaFX Media and Swing bootstrap, including native libraries for this installer platform.
    val javafxClassifier =
        when (installerPlatform.id) {
            "linux-x64" -> "linux"
            "linux-arm64" -> "linux-aarch64"
            "windows-x64" -> "win"
            "macos-x64" -> "mac"
            "macos-arm64" -> "mac-aarch64"
            else -> error("JavaFX runtime is unavailable for ${installerPlatform.id}")
        }
    listOf("base", "graphics", "swing", "media").forEach { module ->
        implementation("org.openjfx:javafx-$module:27.0.1:$javafxClassifier")
    }

    // `kotlinx-coroutines-swing` below is what drives Swing-dispatched coroutines; `core` is
    // declared in its own right because `DatabaseWarmUp` reaches for `Dispatchers`, `Job` and
    // `CoroutineScope` directly rather than through the Swing extension.
    implementation(libs.org.jetbrains.kotlinx.kotlinx.serialization.json)
    implementation(libs.org.jetbrains.kotlinx.kotlinx.coroutines.core)
    implementation(libs.org.jetbrains.kotlinx.kotlinx.coroutines.swing)
    implementation(libs.io.ktor.ktor.client.core)
    implementation(libs.io.ktor.ktor.client.cio)
    implementation(libs.io.ktor.ktor.client.content.negotiation)
    implementation(libs.io.ktor.ktor.serialization.kotlinx.json)

    // The desktop tray. It is a Compose composable and so is declared in `Main.kt` rather than built
    // here. It is native per platform, which is the whole point of dropping the hand-rolled AWT tray:
    // the JDK draws the same menu everywhere. The tray is also the only thing this app hands the OS to
    // draw — a due reminder is announced by the app's own overlay window, so there is no notification
    // service dependency here.
    //
    // The tray reaches the OS through JNA and D-Bus rather than through the JDK, so the packaging
    // tasks below are the only thing that has to care: the Linux package ships a plain jar and a
    // JVM, and nothing here shrinks or rewrites bytecode. If an uber-jar task is ever added it
    // will need the library's documented keep rules (`com.sun.jna.**` and
    // `dev.nucleusframework.composenativetray.**`) -- JNA resolves its native functions by
    // reflection at call time, so a shrinking build would break the tray silently at runtime.
    implementation(libs.dev.nucleusframework.compose.native.tray)
    implementation(libs.dbus.java.core)
    runtimeOnly(libs.dbus.java.unix.socket)
    implementation(libs.dev.nucleusframework.autolaunch)
    implementation(libs.dev.nucleusframework.updater.runtime)

    // App settings (locale, notification switches). DataStore replaces `java.util.prefs`: it is
    // transactional, it survives a crash mid-write, and it exposes `Flow` reads, so the UI can
    // observe the stored value instead of each state holder hand-rolling a mirror of it.
    implementation(libs.androidx.datastore.preferences.core)

    // Navigation 3. The back stack replaces the `when (state.tab)` the shell used to switch
    // screens with: destinations are sealed `NavKey` values, so the history is a real list and a
    // destination added on top of another one comes back on pop. `navigation3-runtime` resolves
    // from the `google()` repository above and `navigation3-ui` from Maven Central, since AndroidX
    // never published a desktop variant of the latter.
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.org.jetbrains.androidx.navigation3.ui)

    // Dependency injection. This module owns the top of the Koin graph (`appModule`) and is
    // the only module that reads it back out of a composition, so it needs both the container
    // and the Compose bindings. `:data` declares the lower half against `koin-core` alone.
    implementation(libs.io.insert.koin.koin.core)
    implementation(libs.io.insert.koin.koin.compose)

    // Images and files. Coil draws the collection covers and FileKit asks for one: on Linux the
    // only real chooser is the XDG Desktop Portal, which is a D-Bus service with no JDK binding,
    // and FileKit ships the D-Bus client for it rather than this app shelling out to `zenity`
    // and dispatching on `os.name`. See `AzkarImageLoader` for where the two meet.
    implementation(libs.io.coil.kt.coil3.coil.compose)
    implementation(libs.io.github.vinceglb.filekit.dialogs)
    implementation(libs.io.github.vinceglb.filekit.coil)
    implementation(libs.jna.platform)

    // `runTest` supplies virtual time and a deterministic scheduler, which is what makes the
    // library's debounced search assertable without a real 180 ms wait.
    testImplementation(libs.org.jetbrains.kotlinx.kotlinx.coroutines.test)
    testImplementation(libs.io.github.takahirom.roborazzi.compose.desktop)
    testImplementation(libs.io.github.takahirom.roborazzi.compose.desktop.preview.scanner.support)
    testImplementation(libs.io.github.darkxanter.webp.imageio)
    // Keep the scanner core ABI compatible with Roborazzi's preview adapter.
    testImplementation(libs.io.github.sergio.sastre.composable.preview.scanner.android)
    testImplementation(libs.junit.junit)

    // Compose UI tests, for the controls. `runComposeUiTest` renders off-screen and drives input
    // through `mainClock`, so the recomposition cost of a hover can be counted a frame at a time
    // instead of being guessed at from a profiler attached to a live desktop session.
    testImplementation(libs.org.jetbrains.compose.ui.test)
    testImplementation(libs.dev.mttmcknn.dejavu)

    // `MockEngine` stands in for the CIO engine the app installs, so the geocoding client can be
    // driven through canned responses without a socket. `ktor-client-mock` is already in the
    // version catalog above the `ktor` line; this is where it becomes a test dependency.
    testImplementation(libs.io.ktor.ktor.client.mock)
}

val previewScannerEnabled =
    providers.gradleProperty("azkar.previewScanner").map(String::toBoolean).getOrElse(false)

if (previewScannerEnabled) {
    dependencies {
        testRuntimeOnly(libs.org.junit.vintage.junit.vintage.engine)
    }
}

roborazzi {
    outputDir.set(layout.projectDirectory.dir("src/previewImages"))
    @OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)
    generateComposePreviewDesktopTests {
        // Preview captures are committed for review, but are never compared as test goldens.
        enable = previewScannerEnabled
        packages = listOf("com.azkar.ui")
        includePrivatePreviews = true
        testerQualifiedClassName = "com.azkar.ui.preview.AzkarDesktopPreviewTester"
        useScanOptionParametersInTester = true
    }
}

tasks.matching { it.name.startsWith("verifyRoborazzi") }.configureEach {
    enabled = false
}

// Compose Multiplatform re-publishes part of the Compose runtime under
// `org.jetbrains.compose.runtime:<module>-desktop`, but those artifacts are empty
// stubs (or redirectors): the classes live in the `androidx.compose.runtime`
// coordinates, which the androidx lifecycle stack in the same graph depends on
// directly. Both land on the classpath under the same file name, so jar/dup
// checks complain. Two more deliberate drops: the material3 1.13-alpha skew
// pulls in `ui-skiko`, which depends on the aggregate `skiko-awt-runtime-all`
// (natives for every OS). The runtime dependency above supplies the host's native library.
//
// The rules are attached PER DECLARED DEPENDENCY rather than to each
// configuration. Configuration-level `exclude(...)` rules stay local to the
// configuration they're declared on, and the packaging plugin resolves the
// shipped classpath through its own detached configuration (which copies the
// declared dependencies but not the exclusions declared next to them), so
// config-level rules let the stubs and the aggregate jar slip back into the
// emitted package config. Scoped per dependency, the rules travel with the
// dependency into every resolution — including Nucleus packaging. The androidx
// runtime supplies the Compose runtime classes.
configurations.configureEach {
    dependencies.withType<ExternalModuleDependency>().configureEach {
        exclude(group = "org.jetbrains.compose.runtime", module = "runtime-desktop")
        exclude(group = "org.jetbrains.compose.runtime", module = "runtime-saveable-desktop")
        exclude(group = "org.jetbrains.skiko", module = "skiko-awt-runtime-all")
    }
}

// The Gradle `application` plugin is deliberately NOT applied: `org.jetbrains.compose`
// contributes its own `run` task and the two collide.
tasks.jar {
    archiveFileName.set("azkar-app.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest { attributes["Main-Class"] = desktopMainClass }

    // Include resources (bundles, fonts, etc.)
    from(sourceSets.main.get().output)
}

// The website gallery runs in its own executable source set.
val gallery =
    sourceSets.create("gallery") {
        compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
    }
configurations[gallery.implementationConfigurationName].extendsFrom(configurations.implementation.get())
kotlin {
    sourceSets.named("test") { kotlin.srcDir("src/previewShared/kotlin") }
    sourceSets.named("gallery") {
        kotlin.srcDirs("src/galleryFixtures/kotlin", "src/previewShared/kotlin")
    }
    target.compilations.named("gallery") { associateWith(target.compilations.getByName("main")) }
}

dependencies {
    add(gallery.implementationConfigurationName, libs.io.github.takahirom.roborazzi.compose.desktop)
    add(gallery.implementationConfigurationName, libs.io.github.darkxanter.webp.imageio)
    add(gallery.implementationConfigurationName, libs.org.jetbrains.compose.ui.test)
}

tasks.register<JavaExec>("captureDownloadScreenshots") {
    group = "documentation"
    description = "Render fresh light and dark images for the download page and README"
    mainClass.set("com.azkar.GalleryKt")
    classpath = gallery.runtimeClasspath
    dependsOn(tasks.named("galleryClasses"))
    args(rootProject.file("site/images").absolutePath)
    jvmArgs("--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow")
    systemProperty("roborazzi.test.record", "true")
    systemProperty(
        "roborazzi.result.dir",
        layout.buildDirectory
            .dir("gallery-results")
            .get()
            .asFile.absolutePath,
    )
}

tasks.withType<Test>().configureEach {
    // Match the desktop runtime flags so UI and persistence tests do not emit JDK access warnings.
    jvmArgs("--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow")
    // The Compose plugin's `mainClass` is the single source of truth for the packaged entry
    // point, and Nucleus derives the Linux `StartupWMClass` from it. The desktop-entry test
    // needs that exact value rather than a copy of it, so it is handed over instead of
    // being re-typed in the test.
    systemProperty("azkar.desktop.mainClass", desktopMainClass)
}

// ---------------------------------------------------------------------------
// Packaging. Nucleus replaces the packaging block while the Compose plugin keeps
// the existing UI/compiler and AWT window runtime. Keep the Linux desktop entry's
// StartupWMClass aligned with the class that initializes AWT in Main.kt.
// ---------------------------------------------------------------------------

// Compose Multiplatform's own resource system, which replaces the hand-rolled
// `ResourceBundle` + typed `Strings` facade (see `i18n/Strings.kt`).
//
// The generated `Res` class is public because background work — the reminder
// scheduler and the filtering service — has to resolve strings outside a
// composition, and the generated accessors are the only way to name a key.
compose.resources {
    publicResClass = true
    packageOfResClass = "com.azkar.generated.resources"
    generateResClass = always
}

// ServiceLoader providers that a rule elsewhere already keeps. Emitting them here
// would be a duplicate line, and a reader would have to open three files to find
// out which keep is actually load-bearing. Running :app:proguardReleaseJars with
// and without this generated file shows that only the dbus and Exposed providers
// are load-bearing; everything below is covered by a broader rule.
val alreadyKeptProviders =
    listOf(
        // app/proguard-rules.pro
        "ch.qos.logback.",
        "io.ktor.",
        "org.h2.",
        // default-compose-desktop-rules.pro, which carries `-keep class kotlin.** { *; }`
        "kotlin.reflect.",
        // ditto: it has an explicit `-keep class kotlinx.coroutines.swing.SwingDispatcherFactory`
        "kotlinx.coroutines.swing.SwingDispatcherFactory",
    )

val collectProguardRules =
    tasks.register("collectProguardRules") {
        description = "Collects ProGuard rules for runtime service providers."
        notCompatibleWithConfigurationCache("Reads and inspects runtime classpath files inside the task action")
        val runtimeClasspath = configurations.named("runtimeClasspath")
        val output = layout.buildDirectory.file("compose/proguard-generated.pro")
        inputs.files(runtimeClasspath)
        outputs.file(output)
        doLast {
            val providers = sortedSetOf<String>()
            runtimeClasspath.get().files.filter { it.name.endsWith(".jar") }.forEach { jar ->
                JarFile(jar).use { zip ->
                    zip
                        .entries()
                        .asSequence()
                        .filter { it.name.startsWith("META-INF/services/") }
                        .forEach { entry ->
                            zip.getInputStream(entry).bufferedReader().useLines { lines ->
                                lines.map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }.forEach {
                                    providers +=
                                        it
                                }
                            }
                        }
                }
            }
            val uncovered =
                providers.filterTo(sortedSetOf()) { provider ->
                    alreadyKeptProviders.none { provider.startsWith(it) }
                }
            output.get().asFile.apply {
                parentFile.mkdirs()
                writeText(
                    buildString {
                        appendLine("# Generated by :app:collectProguardRules - do not edit.")
                        appendLine()
                        appendLine("# Classes named in META-INF/services are loaded by name, so shrinking")
                        appendLine("# would delete them and the app would fail with `Provider ... not found`.")
                        appendLine("# Providers already held by a broader keep are left out; see")
                        appendLine("# `alreadyKeptProviders` in this build script.")
                        uncovered.forEach { appendLine("-keep class $it { *; }") }
                    },
                )
            }
            logger.lifecycle(
                "Collected ${uncovered.size} service providers, " +
                    "${providers.size - uncovered.size} already kept elsewhere",
            )
        }
    }

// Installer platform detection falls back to Linux for unknown hosts.
if (providers.systemProperty("os.name").orNull.equals("Linux", ignoreCase = true)) {
    tasks.register<Exec>("runGnomePanel") {
        val appRun = tasks.named<JavaExec>("run")
        group = "application"
        description = "Run Azkar with its extension in a nested GNOME Shell."
        dependsOn(provider { appRun.get().taskDependencies.getDependencies(appRun.get()) })
        doFirst {
            val run = appRun.get()
            val resources = tasks.named<Sync>("prepareAppResources").get().destinationDir
            environment(run.environment)
            environment("AZKAR_EXTENSION", resources.resolve("gnome/azkar-prayers@azkar.app"))
            workingDir(run.workingDir)
            commandLine(
                listOf(rootProject.file("scripts/dev/run-gnome-panel"), run.javaLauncher.get().executablePath) +
                    run.allJvmArgs +
                    listOf("-cp", run.classpath.asPath, desktopMainClass) +
                    run.args.orEmpty(),
            )
        }
    }
    tasks.register<Exec>("captureGnomePanelPreviews") {
        group = "verification"
        description = "Capture the GNOME prayer menus in an isolated Shell session."
        val resources = tasks.named<Sync>("prepareAppResources")
        dependsOn(resources)
        doFirst {
            commandLine(
                rootProject.file("scripts/dev/capture-gnome-panel"),
                resources.get().destinationDir.resolve("gnome/azkar-prayers@azkar.app"),
                layout.projectDirectory.dir("src/previewImages").asFile,
            )
        }
    }
    val linuxIcon = rootProject.layout.projectDirectory.file("assets/icons/azkar.png")
    val panelIcons = fileTree(rootProject.file("assets/icons/lucide")) {
        include("bell.svg", "bell-off.svg", "calendar.svg", "house.svg", "moon.svg", "sun.svg", "sunrise.svg", "sunset.svg")
    }
    val iconShape = Regex("<(path|line|rect|circle|polyline|polygon|ellipse) ")
    val shellExtension = layout.projectDirectory.dir("src/desktopResources/linux/gnome")
    val extensionVersion = version.toString()
    val extensionSource = layout.projectDirectory.dir("src/gnomeExtension")
    val extensionOutput = layout.buildDirectory.dir("generated/gnomeExtension")
    val extensionWorkspace = layout.buildDirectory.dir("gnomeExtension")
    val compileGnomeExtension = tasks.register<Exec>("compileGnomeExtension") {
        val source = extensionSource.asFile.absolutePath
        val workspace = extensionWorkspace.get().asFile.absolutePath
        val output = extensionOutput.get().asFile.absolutePath
        commandLine(
            "bash",
            "-ec",
            """
            rm -rf -- "$2" "$3"
            mkdir -p "$2"
            cp "$1"/*.ts "$1"/tsconfig.json "$1"/package.json "$1"/pnpm-lock.yaml "$2/"
            cd "$2"
            pnpm install --frozen-lockfile --ignore-scripts
            bun run build --outDir "$3"
            """.trimIndent(),
            "compile-gnome-extension",
            source,
            workspace,
            output,
        )
        inputs
            .files(fileTree(extensionSource) { include("*.ts", "tsconfig.json", "package.json", "pnpm-lock.yaml") })
            .withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.dir(extensionOutput)
        localState.register(extensionWorkspace)
    }
    tasks.withType<Sync>().configureEach {
        if (name == "prepareAppResources") {
            inputs.property("gnomeSymbolicIconFormat", "stroke-mask")
            inputs.property("gnomeExtensionVersion", extensionVersion)
            filesMatching("gnome/azkar-prayers@azkar.app/metadata.json") {
                val versionName = JsonOutput.toJson(extensionVersion)
                filter { line: String -> line.replace("\"version-name\": \"development\"", "\"version-name\": $versionName") }
            }
            from(panelIcons) {
                into("gnome/azkar-prayers@azkar.app")
                rename { it.removeSuffix(".svg") + "-symbolic.svg" }
                // GNOME recolors fills in symbolic SVGs. Preserve Lucide's strokes as a mask.
                filter { line: String ->
                    when (line.trim()) {
                        ">" -> ">\n  <mask id=\"outline\" maskUnits=\"userSpaceOnUse\">"
                        "</svg>" ->
                            """  </mask>
                            |  <rect width="24" height="24" style="stroke: none" mask="url(#outline)"/>
                            |</svg>""".trimMargin()
                        else -> iconShape.replace(line) { match ->
                            """${match.value}style="fill: none !important; stroke: #fff !important" """
                        }
                    }
                }
            }
            from(listOf(linuxIcon, compileGnomeExtension)) {
                into("gnome/azkar-prayers@azkar.app")
            }
        }
    }
    tasks.matching { it.name in setOf("createDistributable", "createReleaseDistributable") }.configureEach {
        // Nucleus 2.5.18 treats appResourcesDir as internal on the jpackage task.
        inputs.property("gnomeExtensionVersion", extensionVersion)
        inputs.dir(shellExtension).withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.files(compileGnomeExtension).withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.files(panelIcons).withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.file(linuxIcon).withPathSensitivity(PathSensitivity.NONE)
    }
}

nucleus.application {
    mainClass = desktopMainClass
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "--sun-misc-unsafe-memory-access=allow",
        // AUTO_SERVER is local-only; avoid reverse DNS of a machine's network address during startup.
        "-Dh2.bindAddress=127.0.0.1",
        "-Dnucleus.app.id=com.azkar.app",
        "-Dnucleus.app.version=${project.version}",
    )
    buildTypes {
        release {
            proguard {
                isEnabled.set(true)
                optimize.set(false)
                obfuscate.set(false)
                configurationFiles.from(project.file("proguard-rules.pro"))
                configurationFiles.from(collectProguardRules)
            }
        }
    }
    nativeDistributions {
        appResourcesRootDir.set(layout.projectDirectory.dir("src/desktopResources"))
        modules("java.sql", "java.naming")
        if (installerPlatform.os == "linux") {
            // The session D-Bus transport reads the Unix UID for SASL EXTERNAL authentication.
            modules("jdk.security.auth")
        }
        val fullVersion = project.version.toString()
        val isDev = fullVersion.contains("-beta.")
        val nativePackageVersion = fullVersion.substringBefore("-beta")
        val formats =
            mutableListOf(
                dev.nucleusframework.desktop.application.dsl.TargetFormat.Nsis,
                dev.nucleusframework.desktop.application.dsl.TargetFormat.Dmg,
                dev.nucleusframework.desktop.application.dsl.TargetFormat.Deb,
                dev.nucleusframework.desktop.application.dsl.TargetFormat.Rpm,
                dev.nucleusframework.desktop.application.dsl.TargetFormat.AppImage,
            )
        // Nucleus models Zip as the current host OS, so request it only on macOS.
        if (installerPlatform.os == "macos") {
            formats += dev.nucleusframework.desktop.application.dsl.TargetFormat.Zip
        }
        targetFormats(
            *formats.toTypedArray(),
        )
        appName = "Azkar"
        packageName = "azkar"
        packageVersion = nativePackageVersion
        description = "Daily adhkar and prayer reminders"
        vendor = "Azkar"
        artifactName = $$"azkar-$$fullVersion-$${installerPlatform.id}.${ext}"
        linux {
            iconFile.set(project.file("../assets/icons/azkar.png"))
            packageName = "azkar"
            startupWMClass = desktopMainClass.replace('.', '-')
            debMaintainer = "Azkar <support@azkar.app>"
            debPackageVersion = fullVersion
            rpmPackageVersion = nativePackageVersion
            afterInstall.set(rootProject.file("packaging/linux/gnome-extension-after-install.sh"))
            afterRemove.set(rootProject.file("packaging/linux/gnome-extension-after-remove.sh"))
        }
        windows {
            iconFile.set(project.file("../assets/icons/azkar.ico"))
            packageName = "azkar"
            nsis {
                installerIcon.set(project.file("../assets/icons/azkar.ico"))
                uninstallerIcon.set(project.file("../assets/icons/azkar.ico"))
                shortcutName = "Azkar"
                createDesktopShortcut = true
                createStartMenuShortcut = true
            }
        }
        macOS {
            packageVersion = macPackageVersion(fullVersion)
            iconFile.set(project.file("../assets/icons/azkar.icns"))
            dmg {
                icon.set(project.file("../assets/icons/azkar.icns"))
            }
            bundleID = "com.azkar.app"
            minimumSystemVersion = "13.0"
        }
        publish {
            publishMode = dev.nucleusframework.desktop.application.dsl.PublishMode.Never
            github {
                enabled = true
                owner = "jamhour1g"
                repo = "Azkar-App"
                channel =
                    if (isDev) {
                        dev.nucleusframework.desktop.application.dsl.ReleaseChannel.Beta
                    } else {
                        dev.nucleusframework.desktop.application.dsl.ReleaseChannel.Latest
                    }
                releaseType =
                    if (isDev) {
                        dev.nucleusframework.desktop.application.dsl.ReleaseType.Prerelease
                    } else {
                        dev.nucleusframework.desktop.application.dsl.ReleaseType.Release
                    }
            }
        }
    }
}

tasks.matching { it.name == "proguardReleaseJars" }.configureEach {
    if (providers.environmentVariable("CI").map(String::toBoolean).getOrElse(false)) {
        // ProGuard sends its long informational notes to stdout; keep them available with --debug.
        logging.captureStandardOutput(LogLevel.DEBUG)
    }
}

// A release tag that disagrees with the declared version would ship an installer whose name lies.
// GitHub Actions identifies tags through GITHUB_REF; branch builds skip this check.
tasks.register("checkTagParity") {
    description = "Checks that a release tag matches the declared application version."
    val gitRef = providers.environmentVariable("GITHUB_REF")
    val version = providers.provider { project.version.toString() }
    doLast {
        val ref = gitRef.orNull
        if (ref == null || !ref.startsWith("refs/tags/")) {
            logger.lifecycle("Not building a tag; skipping the release tag check")
            return@doLast
        }
        val tagName = ref.removePrefix("refs/tags/")
        val declared = version.get()
        check(tagName.removePrefix("v") == declared && tagName.removePrefix("v") == defaultAppVersion) {
            "Tag $tagName must match the source version '$defaultAppVersion' and package version '$declared'"
        }
        logger.lifecycle("Tag version parity OK: $declared")
    }
}
