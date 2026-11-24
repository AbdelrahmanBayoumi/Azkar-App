import com.diffplug.spotless.LineEnding
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

repositories {
    mavenCentral()
}

plugins {
    id("com.diffplug.spotless")
    id("dev.detekt")
    id("org.jetbrains.kotlin.jvm")
    // `org.jetbrains.kotlin.jvm` already applies this; it is listed because the `sourceSets` and
    // `java` extension lookups below resolve through it rather than through the Kotlin plugin.
    id("java")
    id("jacoco")
}

// `config/testing` holds test helpers that belong to no single module and are compiled into
// every module's `test` compilation. `config/logback` holds the Logback configuration tests run
// under, wired the same way. Both are neutral siblings of `config/detekt` rather than `src`
// directories precisely so they can be shared; the Spotless and detekt targets below are
// pointed at `config/testing` explicitly, which is what keeps those files formatted and linted
// like any other.
val sharedTestingDirectory = rootProject.layout.projectDirectory.dir("config/testing")
val sharedLogbackDirectory = rootProject.layout.projectDirectory.dir("config/logback")

// `de.infix.testBalloon` installs a Kotlin *compiler* plugin, and it reaches for the `kotlin`
// extension while it is still being applied — so unlike every plugin above it cannot sit in the
// `plugins` block. The modules apply `org.jetbrains.kotlin.jvm` themselves, and this waits for
// that to happen first. Applying it to a module without Kotlin sources is harmless: the
// compiler plugin then reports itself as not applicable and does nothing.
plugins.withId("org.jetbrains.kotlin.jvm") {
    pluginManager.apply("de.infix.testBalloon")

    val testSourceSet = extensions.getByType<KotlinJvmProjectExtension>().sourceSets.getByName("test")
    testSourceSet.kotlin.srcDir(sharedTestingDirectory)
    // `logback-test.xml` is the one file Logback looks for ahead of `logback.xml`, so putting it
    // on every module's test runtime classpath is what stops the unit tests from inheriting the
    // shipped INFO-level configuration.
    testSourceSet.resources.srcDir(sharedLogbackDirectory)
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {

    // Logging for all modules. kotlin-logging is the facade every module logs through: it keeps
    // messages lazily evaluated and drops the `LoggerFactory.getLogger(Foo::class.java)`
    // boilerplate. It resolves to SLF4J at runtime, so Logback stays the backend.
    implementation(libs.findLibrary("io-github-oshai-kotlin-logging-jvm").get())
    // The `-jvm` artifact already depends on `slf4j-api`, but pinning it here keeps the facade
    // and the API it compiles against on the exact same version.
    implementation(libs.findLibrary("org-slf4j-slf4j-api").get())
    // Logback is the SLF4J provider, and it has to be declared here rather than in `:app`:
    // kotlin-logging only binds a logger factory, it does not bring a backend, so a module that
    // logs without one silently gets SLF4J's NOP logger. `:data` holds most of the log statements
    // in the repo, so leaving this to the application module muted all of them. `runtimeOnly` is
    // right because the backend is a runtime concern — the config lives in `logback.xml` and the
    // compiled code only ever touches the SLF4J API.
    runtimeOnly(libs.findLibrary("ch-qos-logback-logback-classic").get())
    // ...and on the test compile classpath as well, because `config/testing`'s log-capture helper
    // is written against `ListAppender`/`ILoggingEvent`. `runtimeOnly` covers the test *runtime*
    // (the Java plugin's `testRuntimeOnly` extends `runtimeOnly`) but not its compile classpath.
    testImplementation(libs.findLibrary("ch-qos-logback-logback-classic").get())

    // Testing (shared default)
    // testBalloon replaces JUnit entirely: it plugs its own TestEngine into the JUnit
    // Platform, so `tasks.test { useJUnitPlatform() }` below is all the wiring Gradle
    // needs. It also injects the launcher itself, but Gradle 9 no longer does that for
    // you, so the launcher stays declared here.
    testImplementation(libs.findLibrary("de-infix-test-balloon-framework-core").get())
    testRuntimeOnly(libs.findLibrary("org-junit-platform-junit-platform-launcher").get())
    testImplementation(libs.findLibrary("org-assertj-assertj-core").get())
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        // `RemembranceImpl` and `TagImpl` are data classes with a `private` constructor, because
        // their `init` blocks trim and validate a field that the generated `copy()` would otherwise
        // hand straight to `equals`/`hashCode` unchecked. Kotlin still published a public `copy()`,
        // which reopened the hole the private constructor exists to close. This flag derives
        // `copy()`'s visibility from the constructor's, so the invariant holds for `copy()` too.
        // Without it the compiler says so on every build and the warning is easy to ignore.
        freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
    }
}

java {
    // The one place the bytecode level is decided, for every module. Without a toolchain each
    // module's `compileJava` inherits the version of whatever JVM happens to be running Gradle,
    // while Kotlin's `jvmTarget` stays pinned — and the two disagreeing is a hard build failure
    // ("Inconsistent JVM-target compatibility detected for tasks 'compileJava' (26) and
    // 'compileKotlin' (25)"). That is not hypothetical: IntelliJ runs Gradle on the *project* JDK,
    // so bumping the IDE's project SDK to 26 broke `./gradlew build` for everyone on the repo.
    // Pinning the toolchain instead decouples the build from that choice: whoever launches Gradle
    // (IntelliJ on 26, CI on 25, a shell on whatever sdkman is `current`) compiles against 25.
    // The Kotlin plugin derives `jvmTarget` from this toolchain, which is why the per-module
    // `jvmTarget` lines that used to live in each `build.gradle.kts` are gone — one setting, and
    // the two compilers cannot drift apart.
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }

    // Left at the default for any module that adds a descriptor, but every current module opts out.
    modularity.inferModulePath = true
}

spotless {
    val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion
    // The repository stores LF and checks out LF — `.gitattributes` says so — so the formatter
    // asks for LF as well. Left to its default, Spotless takes the line endings from git's
    // configuration, which differs between a Linux and a Windows machine, and the two then
    // disagree about the same file.
    lineEndings = LineEnding.UNIX

    // `sharedTestingDirectory` is deliberately absent: Spotless rejects any target outside the
    // project directory, and the shared helper sits at the repository root. The root project's
    // own `build.gradle.kts` formats it instead, reading the same `.editorconfig`.
    kotlin {
        target("src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(ktlintVersion)
    }

    kotlinGradle {
        target("*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(ktlintVersion)
    }
}

// detekt is the static-analysis gate, with type resolution wired up below so its type-aware
// rules are live rather than silently inert.
detekt {
    buildUponDefaultConfig = true
    config.setFrom(
        rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"),
        rootProject.layout.projectDirectory.file("config/detekt/detekt-compose.yml"),
    )
    source.setFrom(layout.projectDirectory.dir("src"), sharedTestingDirectory)
    // Detekt 2's parallel Analysis API can race and fail intermittently with
    // ArrayIndexOutOfBoundsException. Keep analysis serial until the upstream fix ships.
    // https://github.com/detekt/detekt/issues/9121
    parallel = false
    ignoreFailures = false
    basePath.set(rootProject.projectDir)
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    include("**/*.kt", "**/*.kts")
    exclude("**/build/**", "**/generated/**")

    // Type resolution. Detekt silently reports *nothing* for every resolution-dependent rule
    // without a compile classpath, so the default config's type-aware rules — `ForbiddenMethodCall`
    // among them — were all inert. This is the price of admitting they are: it surfaces the
    // backlog they had been hiding, which is the point.
    val analysisClasspaths =
        buildList {
            add(sourceSets["main"].compileClasspath)
            add(sourceSets["test"].compileClasspath)
        }
    classpath.setFrom(analysisClasspaths)
}

tasks.test {
    systemProperty("h2.bindAddress", "127.0.0.1")
    // testBalloon registers itself as a JUnit Platform TestEngine, so the Platform is still
    // what runs the tests — only the engine behind it changed.
    useJUnitPlatform()
    finalizedBy(tasks.named("jacocoTestReport"))
}

// JaCoCo — test coverage reporting
jacoco {
    toolVersion = "0.8.15"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    reports {
        xml.required = true // needed for CI integrations
        html.required = true // human-readable report
        csv.required = false
    }
}

// In CI: fail on formatting violations; locally: auto-fix before build
tasks.build {
    val isCI = providers.environmentVariable("CI").isPresent
    if (isCI) {
        dependsOn(tasks.named("spotlessCheck"))
    } else {
        dependsOn(tasks.spotlessApply)
    }
}
