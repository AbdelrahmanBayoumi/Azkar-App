package com.azkar.i18n

import androidx.compose.ui.unit.LayoutDirection
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * Which way a piece of user-typed text should be laid out.
 *
 * The centre of these is that non-Latin text has to come back right-to-left, because the one way this
 * can go quietly wrong is by testing only with English: a check written against Latin letters agrees
 * with every Latin test and with every empty field, and disagrees with every Arabic name in the app.
 * The scripts tested here are therefore the ones a single wrong constant would silently exclude —
 * Arabic, Hebrew, Syriac and Thaana all report right-to-left through *different* platform constants,
 * and a check that names only one of them passes the suite while leaving the other three laid out
 * backwards.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val BidiDirectionSuite by testSuite("Which way a typed name should be laid out") {
    testSuite("a letter decides, whichever script it is written in") {
        test("Arabic is right to left") {
            assertThat(directionalHint("أذكار الصباح")).isEqualTo(LayoutDirection.Rtl)
        }

        test("Hebrew is right to left") {
            assertThat(directionalHint("תזכורות")).isEqualTo(LayoutDirection.Rtl)
        }

        test("Syriac is right to left") {
            assertThat(directionalHint("ܕܫܡܪܘܢܐ")).isEqualTo(LayoutDirection.Rtl)
        }

        test("Thaana is right to left") {
            assertThat(directionalHint("ދިވެސް")).isEqualTo(LayoutDirection.Rtl)
        }

        test("Latin is left to right") {
            assertThat(directionalHint("Morning Azkar")).isEqualTo(LayoutDirection.Ltr)
        }
    }

    testSuite("only a letter counts as strong") {
        test("text that opens with digits is decided by the letters behind them") {
            // "2024 planner" opens with numbers and is plainly left-to-right, which is why digits
            // cannot be taken for an answer.
            assertThat(directionalHint("2024 planner")).isEqualTo(LayoutDirection.Ltr)
            assertThat(directionalHint("2024 مخطط")).isEqualTo(LayoutDirection.Rtl)
        }

        test("Latin brackets do not outvote Arabic letters inside them") {
            assertThat(directionalHint("(أذكار)")).isEqualTo(LayoutDirection.Rtl)
            assertThat(directionalHint("[أذكار]")).isEqualTo(LayoutDirection.Rtl)
        }

        test("leading punctuation and spaces are skipped rather than answered") {
            assertThat(directionalHint("  ... أذكار")).isEqualTo(LayoutDirection.Rtl)
        }

        test("a currency symbol is not a letter and does not decide") {
            assertThat(directionalHint("﷼100 morning")).isEqualTo(LayoutDirection.Ltr)
        }
    }

    testSuite("text that does not say") {
        test("nothing at all says nothing") {
            assertThat(directionalHint(null)).isNull()
            assertThat(directionalHint("")).isNull()
        }

        test("a field holding only digits or punctuation says nothing") {
            assertThat(directionalHint("12345")).isNull()
            assertThat(directionalHint("!!!")).isNull()
            assertThat(directionalHint("   ")).isNull()
        }

        test("the layout falls back to what the field was already using") {
            assertThat(textDirectionOf("أذكار", LayoutDirection.Ltr)).isEqualTo(LayoutDirection.Rtl)
            assertThat(textDirectionOf("Morning", LayoutDirection.Rtl)).isEqualTo(LayoutDirection.Ltr)
            assertThat(textDirectionOf("", LayoutDirection.Rtl)).isEqualTo(LayoutDirection.Rtl)
            assertThat(textDirectionOf(null, LayoutDirection.Ltr)).isEqualTo(LayoutDirection.Ltr)
        }
    }
}
