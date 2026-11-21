package com.azkar.ui.notifications.presentation

import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * The rule the bar along a card's top edge follows.
 *
 * The bar is the only part of a reminder card that says how long the card intends to stay, so it has
 * to be a clock and not a decoration. A clock runs one way, and the rule that keeps it running one
 * way is the whole of what this suite is about: the bar keeps the smallest fraction it has been told
 * and refuses a reading that would put time back on it.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val CountdownStateSuite by testSuite("The notification card's countdown") {
    test("a card that has just arrived is all of its time") {
        val countdown = CountdownState()

        assertThat(countdown.target).isEqualTo(1f)
    }

    test("the bar follows the card down") {
        val countdown = CountdownState()

        countdown.sample(0.6f)

        assertThat(countdown.target).isEqualTo(0.6f)
    }

    test("the bar never fills back up") {
        val countdown = CountdownState()
        countdown.sample(0.4f)

        // A card on its way off the screen is still asked how much time it has, and the answer can
        // come back as "all of it" from a reader that has already forgotten the card. Believing that
        // is what made the bar jump back to the top as the card left the screen.
        countdown.sample(1f)

        assertThat(countdown.target).isEqualTo(0.4f)
    }

    test("a reading outside the bar's own range is not believed") {
        val tooMuch = CountdownState()
        tooMuch.sample(3f)
        assertThat(tooMuch.target).isEqualTo(1f)

        val tooLittle = CountdownState()
        tooLittle.sample(-2f)
        assertThat(tooLittle.target).isEqualTo(0f)
    }
}
