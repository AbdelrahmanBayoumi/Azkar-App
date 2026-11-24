package com.azkar

import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import java.lang.reflect.Modifier

/**
 * The agreement between the running window and the desktop entry that ships it.
 *
 * A Linux desktop manager matches a window to its `.desktop` entry by comparing the entry's
 * `StartupWMClass` with the window's `WM_CLASS`. The two strings are computed independently:
 *
 * - Nucleus writes `StartupWMClass` by taking the packaged entry point's class name and
 *   replacing its dots with hyphens (`com.azkar.MainKt` becomes `com-azkar-MainKt`),
 * - AWT reports `WM_CLASS` from the class name of the bottom stack frame of whichever thread
 *   first constructed the AWT toolkit, with the same dots-to-hyphens substitution.
 *
 * When they drift apart nothing crashes. The app runs perfectly and the dock shows a generic
 * Java icon next to a label that is the raw `WM_CLASS`. That is not hypothetical: the toolkit was
 * first touched on a coroutine worker, so the dock used to read
 * `kotlinx-coroutines-scheduling-CoroutineScheduler$Worker`.
 *
 * `azkar.desktop.mainClass` is the value the build script hands the packager, so these
 * assertions run against the string the package will carry rather than a copy of it.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val DesktopWindowClassSuite by testSuite("The Linux desktop entry") {
    test("the packaged entry point is the class the launcher starts") {
        val entryPoint = Class.forName(packagedMainClass())

        val main = entryPoint.getMethod("main", Array<String>::class.java)

        assertThat(Modifier.isStatic(main.modifiers))
            .describedAs("The JVM launcher only starts a static main")
            .isTrue()
        assertThat(main.returnType)
            .describedAs("The JVM launcher only starts a void main")
            .isEqualTo(Void.TYPE)
    }

    test("the window class the entry declares is one a desktop entry can express") {
        // AWT's `getCorrectXIDString` and the Nucleus Gradle plugin make the same
        // substitution, so this is the exact string the packaged desktop entry will carry.
        val startupWmClass = packagedMainClass().replace('.', '-')

        assertThat(startupWmClass)
            .describedAs("The StartupWMClass the packaged desktop entry will declare")
            .matches("[A-Za-z0-9_-]+")

        assertThat(packagedMainClass())
            .describedAs(
                "A nested entry class would put a `$` into the window class, which desktop " +
                    "entries and the shell that reads them both mangle",
            ).doesNotContain("$")
    }

    test("the AWT toolkit is initialised from the class that owns the entry point") {
        // AWT names the window after the class at the bottom of the stack that built the
        // toolkit, and the toolkit is a process-wide singleton: whoever gets there first wins.
        // `Main.kt` therefore touches it from `main`, on the main thread, before the startup
        // sequence suspends.
        //
        // The helper that does it has to live in the same class as `main`. Move it to another
        // file and AWT would report that file's class instead, the window would stop matching
        // the `StartupWMClass` the packager writes, and the dock would go back to the generic
        // Java icon -- with no test failing, which is why this one exists.
        val entryPoint = Class.forName(packagedMainClass())

        assertThat(entryPoint.declaredMethods.map { it.name })
            .describedAs(
                "The methods of the packaged entry class; the AWT toolkit has to be " +
                    "initialised from this class for the window class to match",
            ).contains(PINNING_METHOD)

        // The class file names the toolkit in its constant pool. A weaker check than the one
        // above -- the entry class has other reasons to touch AWT -- but it is independent of
        // any method name, so it still fails if the toolkit stops being this class's business
        // altogether.
        val classFile =
            requireNotNull(
                entryPoint.getResourceAsStream("/${packagedMainClass().replace('.', '/')}.class"),
            ) { "The compiled entry class is not on the test classpath" }

        assertThat(String(classFile.use { it.readBytes() }, Charsets.ISO_8859_1))
            .describedAs("The constant pool of the compiled entry class")
            .contains("java/awt/Toolkit")
    }
}

/**
 * The class the packager launches, read from the build script rather than re-typed here.
 *
 * The build script owns that string, and it owns it in one place for a reason: the launcher, the
 * desktop entry's `StartupWMClass` and the window's `WM_CLASS` all have to be derived from the
 * same class name, and a copy in a test is a copy that can drift.
 */
private fun packagedMainClass(): String =
    requireNotNull(System.getProperty(MAIN_CLASS_PROPERTY)) {
        "$MAIN_CLASS_PROPERTY is not set. The build script passes the packaged entry point to " +
            "the test JVM; see `desktopMainClass` in app/build.gradle.kts."
    }

/**
 * The method that initialises the AWT toolkit.
 *
 * It is the first statement of `main` in `Main.kt`, and it has to stay in the same class as
 * `main`: the window's class name is the class at the bottom of the stack that builds the
 * toolkit, so a helper in another file would rename the window. Renaming the helper therefore
 * means updating this constant, which is the point -- the change should be a deliberate one.
 */
private const val PINNING_METHOD = "pinDesktopWindowClass"

private const val MAIN_CLASS_PROPERTY = "azkar.desktop.mainClass"
