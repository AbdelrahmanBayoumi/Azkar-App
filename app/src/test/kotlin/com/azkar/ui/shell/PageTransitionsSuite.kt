package com.azkar.ui.shell

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * The direction the shell's page transitions travel in.
 *
 * Nothing about a transition can be checked by looking at a screenshot — `AnimatedContent` does not
 * animate its first composition, so the still the harness captures is the same with or without any
 * of this, and the desktop `NavDisplay` shipped a hard cut before it, which is exactly why nobody
 * noticed. The sign is also the one piece of this that is genuinely easy to get wrong twice: it is
 * derived from a boolean in one place and a layout direction in another, and a page that slides the
 * wrong way reads as a bug rather than as an effect.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val PageTransitionsSuite by testSuite("Moving between pages") {
    test("a step forwards arrives from the trailing edge") {
        // Left-to-right: further in means further right, so the newcomer starts to the right of
        // where it settles and slides left into place.
        assertThat(pageTravelOffset(isRtl = false, forwards = true, width = 1000)).isPositive()
    }

    test("a step back retraces the path forwards took") {
        assertThat(pageTravelOffset(isRtl = false, forwards = false, width = 1000))
            .isEqualTo(-pageTravelOffset(isRtl = false, forwards = true, width = 1000))
    }

    test("Arabic mirrors the whole gesture") {
        // Reading direction reversed, so the same "further in" starts from the other edge entirely.
        assertThat(pageTravelOffset(isRtl = true, forwards = true, width = 1000))
            .isEqualTo(-pageTravelOffset(isRtl = false, forwards = true, width = 1000))
    }

    test("the travel is a fraction of the surface rather than the whole of it") {
        // A full-width slide is a phone gesture; on a desktop window it reads as a slideshow.
        val travel = pageTravelOffset(isRtl = false, forwards = true, width = 1000)

        assertThat(travel).isPositive().isLessThan(1000)
    }

    test("both page transitions move something") {
        // `EnterTransition.None` is what the desktop NavDisplay used before this existed, and it is
        // what a transition silently degrades back to if a half of it stops being applied.
        assertThat(pushPageTransition(isRtl = false).targetContentEnter).isNotEqualTo(EnterTransition.None)
        assertThat(popPageTransition(isRtl = false).targetContentEnter).isNotEqualTo(EnterTransition.None)
        assertThat(pushPageTransition(isRtl = false).initialContentExit).isNotEqualTo(ExitTransition.None)
        assertThat(popPageTransition(isRtl = false).initialContentExit).isNotEqualTo(ExitTransition.None)
    }

    test("going back is not the same transition as going in") {
        // The halves are the same pair, swapped — but not the *same* pair, or one of the two
        // directions would move both pages the same way.
        assertThat(pushPageTransition(isRtl = false)).isNotEqualTo(popPageTransition(isRtl = false))
    }

    test("the Arabic transitions are not the Latin ones") {
        // Guards against the mirroring being dropped at the call site rather than in the arithmetic.
        assertThat(pushPageTransition(isRtl = true)).isNotEqualTo(pushPageTransition(isRtl = false))
        assertThat(popPageTransition(isRtl = true)).isNotEqualTo(popPageTransition(isRtl = false))
    }
}
