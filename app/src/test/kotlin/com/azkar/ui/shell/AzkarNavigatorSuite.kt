package com.azkar.ui.shell

import androidx.navigation3.runtime.NavBackStack
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * The Navigation 3 back stack the shell navigates.
 *
 * What replaced the old `state.tab` was a single value, so it could not be wrong about history —
 * and equally could not express any. The bug this suite guards is the one a real stack invites:
 * treating the two top-level destinations as a history instead of as peers, which would leave
 * `Home → Library → Home → Library` behind every pair of header clicks and give the library screen's
 * "back to home" button nothing to collapse.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val AzkarNavigatorSuite by testSuite("Moving between the top-level destinations") {
    test("the stack opens on the dashboard") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))

        assertThat(navigator.current).isEqualTo(AzkarDestination.Home)
        assertThat(navigator.entries).containsExactly(AzkarDestination.Home)
    }

    test("the library is pushed on top of the dashboard rather than replacing it") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))

        navigator.showLibrary()

        // Home stays underneath on purpose: it is the root every other destination is reachable
        // back from, and the header highlight reads `current`, which is what this asserts too.
        assertThat(navigator.entries).containsExactly(AzkarDestination.Home, AzkarDestination.Library)
        assertThat(navigator.current).isEqualTo(AzkarDestination.Library)
    }

    test("going home from the library collapses the stack instead of stacking on it") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))
        navigator.showLibrary()

        navigator.showHome()

        assertThat(navigator.entries).containsExactly(AzkarDestination.Home)
    }

    test("re-picking the destination already on screen changes nothing") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))
        navigator.showLibrary()

        navigator.showLibrary()

        assertThat(navigator.entries).containsExactly(AzkarDestination.Home, AzkarDestination.Library)
    }

    test("alternating between the two never grows the stack") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))

        repeat(5) {
            navigator.showLibrary()
            navigator.showHome()
        }

        assertThat(navigator.entries).containsExactly(AzkarDestination.Home)
    }

    test("switching top-level destinations is not itself a back step") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))

        navigator.showLibrary()
        navigator.showHome()
        navigator.showLibrary()

        // `[Home]` alone would mean the dashboard could not be returned to without replacing the
        // library; the library has to sit above it for `onBack` to have anywhere to land.
        assertThat(navigator.entries).hasSize(2)
        assertThat(navigator.current).isEqualTo(AzkarDestination.Library)
    }

    test("settings sits on top of wherever the gear was opened from") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))

        navigator.showLibrary()
        navigator.showSettings()

        // Pushed, not swapped in: the gear is on every screen, so reaching settings must not cost the
        // user the library they had open.
        assertThat(navigator.entries).containsExactly(
            AzkarDestination.Home,
            AzkarDestination.Library,
            AzkarDestination.Settings,
        )
    }

    test("back from settings returns to the screen underneath") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))
        navigator.showLibrary()
        navigator.showSettings()

        navigator.back()

        assertThat(navigator.current).isEqualTo(AzkarDestination.Library)
    }

    test("opening settings twice does not stack it twice") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))

        navigator.showSettings()
        navigator.showSettings()

        // Two entries would need two back presses to leave, which reads as the screen ignoring the
        // first one.
        assertThat(navigator.entries).containsExactly(AzkarDestination.Home, AzkarDestination.Settings)
    }

    test("the update notice opens settings at the update section") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))

        navigator.showUpdateSettings()

        assertThat(navigator.current).isEqualTo(AzkarDestination.Settings)
        assertThat(navigator.settingsInitialSection).isEqualTo(SettingsSection.UPDATES)
        val request = navigator.settingsSectionRequest

        navigator.showUpdateSettings()

        assertThat(navigator.settingsSectionRequest).isEqualTo(request + 1)
    }

    test("a top-level tab collapses settings rather than leaving it underneath") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))
        navigator.showSettings()

        navigator.showLibrary()

        // `showTopLevel` trims everything above the root, so hitting a header tab is always a way out
        // of a pushed screen rather than another thing to back out of.
        assertThat(navigator.entries).containsExactly(AzkarDestination.Home, AzkarDestination.Library)
    }

    test("back at the dashboard stops at the dashboard") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))

        navigator.back()

        // `NavDisplay` renders `backStack.last()`, so an empty stack would leave it with nothing.
        assertThat(navigator.entries).containsExactly(AzkarDestination.Home)
    }

    test("a collection opens on top of the shelf rather than replacing it") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))
        navigator.showLibrary()

        navigator.showCollection(7L)

        // Pushed, not swapped in: the shelf is where the user came from and where they are going
        // back to, and its own filters are only preserved if it stays in the stack underneath.
        assertThat(navigator.entries).containsExactly(
            AzkarDestination.Home,
            AzkarDestination.Library,
            AzkarDestination.CollectionDetail(7L),
        )
    }

    test("opening the collection already on screen does not stack it twice") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))
        navigator.showLibrary()
        navigator.showCollection(7L)

        navigator.showCollection(7L)

        // Two entries would need two back presses to leave the collection, which reads as the
        // screen ignoring the first one.
        assertThat(navigator.entries).hasSize(3)
    }

    test("opening a different collection from inside one is a real move") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))
        navigator.showLibrary()
        navigator.showCollection(7L)

        navigator.showCollection(8L)

        // The page on top has to change to something else, so this one does push rather than
        // collapsing back to the shelf.
        assertThat(navigator.current).isEqualTo(AzkarDestination.CollectionDetail(8L))
    }

    test("back from a collection returns to the shelf, not to the dashboard") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))
        navigator.showLibrary()
        navigator.showCollection(7L)

        navigator.back()

        assertThat(navigator.current).isEqualTo(AzkarDestination.Library)
    }

    test("a header tab collapses a collection page rather than leaving it underneath") {
        val navigator = AzkarNavigator(NavBackStack(AzkarDestination.Home))
        navigator.showLibrary()
        navigator.showCollection(7L)

        navigator.showLibrary()

        assertThat(navigator.entries).containsExactly(AzkarDestination.Home, AzkarDestination.Library)
    }

    test("a collection page still counts as being in the library") {
        assertThat(AzkarDestination.CollectionDetail(7L).rootTab).isEqualTo(AzkarDestination.Library)
        assertThat(AzkarDestination.Settings.rootTab).isEqualTo(AzkarDestination.Settings)
        assertThat(AzkarDestination.Library.rootTab).isEqualTo(AzkarDestination.Library)
    }
}
