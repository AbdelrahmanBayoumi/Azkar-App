package com.azkar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.azkar.testing.assertComposableRecompositions
import com.azkar.testing.assertTrackedRecompositions
import com.azkar.testing.assertTrackedStable
import com.azkar.ui.theme.AzkarTheme
import de.infix.testBalloon.framework.core.testSuite
import dejavu.resetRecompositionCounts
import dejavu.runRecompositionTrackingUiTest
import dejavu.setTrackedContent
import org.assertj.core.api.Assertions.assertThat
import java.util.Locale

private const val OVERFLOW_TAG = "overflow"
private const val CONTROL_TAG = "control"
private const val SWITCH_TAG = "switch"
private const val TALL_FIELD_TAG = "tall-field"
private const val SHORT_FIELD_TAG = "short-field"
private const val ELSEWHERE_TAG = "elsewhere"

private const val STAR_TOOLTIP = "Add to favourites"

/**
 * Material 3's floor for an `OutlinedTextField`, and the reason a caller asking for a shorter field
 * does not get one.
 *
 * It cannot be configured in this artifact: `OutlinedTextFieldDefaults.minHeight` is a private
 * constant with only a getter, and the `String`-value overload takes no `contentPadding` at all.
 */
private val MATERIAL_FIELD_FLOOR = 56.dp

/** Comfortably above [MATERIAL_FIELD_FLOOR], used to prove a large `minHeight` is honoured. */
private val TALL_FIELD_HEIGHT = 72.dp

/** Below [MATERIAL_FIELD_FLOOR], used to prove Material's own floor wins. */
private val SHORT_FIELD_HEIGHT = 8.dp

/**
 * A dimension for the placeholder boxes the container tests put inside their subjects.
 *
 * It has to be non-zero: an empty `Box` with only a `testTag` on it measures nothing, so
 * `assertIsDisplayed` would fail on a component that had rendered its content perfectly.
 */
private val PROBE_SIZE = 40.dp

/**
 * Renders [content] inside the app's real light theme, on a white canvas.
 *
 * The controls read `AzkarTheme.colors` and `MaterialTheme` in their own defaults, so testing them
 * outside a theme would not exercise the path the app runs. Light is fixed rather than following
 * the system, because a developer's desktop preference must not decide which assertions run.
 */
@Composable
private fun Themed(content: @Composable () -> Unit) {
    AzkarTheme(darkTheme = false, uiLocale = Locale.ENGLISH) {
        Box(Modifier.fillMaxSize().background(Color.White)) { content() }
    }
}

/**
 * A switch that owns its own state, the way a real call site does.
 *
 * `AzkarToggleSwitch` is fully controlled — it reports an intent and renders whatever `checked`
 * says. Hosted here it can be clicked twice, so a switch that always reported `true` would fail
 * rather than pass. It has exactly one call site in the app, buried in a dialog that no screenshot
 * in this project can reach.
 */
@Composable
private fun HostedSwitch(
    enabled: Boolean,
    onReport: (Boolean) -> Unit,
) {
    var checked by remember { mutableStateOf(false) }
    AzkarToggleSwitch(
        checked = checked,
        onCheckedChange = {
            checked = it
            onReport(it)
        },
        enabled = enabled,
        modifier = Modifier.testTag(SWITCH_TAG),
    )
}

/**
 * The `onValueChange` round trip, hosted statefully so typing is actually applied to the text.
 *
 * Driven through the real text-input semantics rather than by calling the callback directly. The
 * import payload field and the reminder dialogs depend on this, and a field that displayed its
 * value perfectly while never reporting an edit would satisfy every other assertion here.
 */
@Composable
private fun HostedField(
    onReport: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by remember { mutableStateOf("") }
    AzkarOutlinedField(
        value = value,
        onValueChange = {
            value = it
            onReport(it)
        },
        placeholder = "Search",
        modifier = modifier.fillMaxWidth(),
    )
}

private fun menuItems(onPick: (String) -> Unit): List<AzkarMenuItem> =
    listOf(
        AzkarMenuItem(label = "Edit", onClick = { onPick("Rename") }, icon = ActionIcons.edit),
        AzkarMenuItem(label = "Duplicate", onClick = { onPick("Duplicate") }, icon = ActionIcons.duplicate),
    )

/**
 * Drives both activation routes at [node] — a real pointer press and a semantic click — and asserts
 * it is flagged disabled. The caller checks its own callback immediately afterwards, which is what
 * keeps this from being vacuous.
 *
 * `assertHasNoClickAction()` is deliberately **not** used here, and the first draft of this file got
 * it wrong. Material 3 keeps the `OnClick` semantics action on a disabled control on purpose: an
 * assistive technology has to be able to find and announce a dimmed button, and dropping the action
 * would leave it undiscoverable rather than merely dimmed. So the action is present *and* inert —
 * a sharper contract to pin than its absence, and one that would catch a control advertising itself
 * as activatable and then quietly doing nothing anyway.
 */
private fun assertDisabledAndInert(node: SemanticsNodeInteraction) {
    node.assertIsNotEnabled()
    node.performMouseInput {
        moveTo(center)
        press()
        release()
    }
    node.performClick()
}

private fun ComposeUiTest.resetControlBudget() {
    waitForIdle()
    resetRecompositionCounts()
}

private fun ComposeUiTest.assertControlBudget(
    tag: String = CONTROL_TAG,
    atMost: Int = 0,
    expectedFunction: String? = null,
) {
    waitForIdle()
    onNodeWithTag(tag).assertTrackedRecompositions(atMost = atMost, expectedFunction = expectedFunction)
}

/**
 * A tooltip button beside somewhere else for the pointer to be, so a hover can be *ended* rather
 * than merely begun.
 *
 * Moving the pointer to a coordinate outside the button would do it too, but only for as long as the
 * test harness kept the pointer in one window; a second hit target is what makes the dismissal a real
 * pointer transition — a `PointerEventType.Exit`, the same event a user's mouse leaves behind — rather
 * than a jump to somewhere nothing is listening.
 */
@Composable
private fun StarBesideElsewhere() {
    Box(Modifier.fillMaxSize()) {
        TooltipIconButton(
            icon = AzkarIcons.Star,
            tooltip = STAR_TOOLTIP,
            onClick = {},
            modifier = Modifier.align(Alignment.TopStart).testTag(CONTROL_TAG),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(PROBE_SIZE)
                .background(Color.LightGray)
                .testTag(ELSEWHERE_TAG),
        )
    }
}

private fun ComposeUiTest.renderOverflowMenu(onPick: (String) -> Unit) {
    setTrackedContent {
        Themed {
            AzkarOverflowMenu(
                icon = AzkarIcons.Settings,
                contentDescription = "Row actions",
                items = menuItems(onPick),
                modifier = Modifier.testTag(OVERFLOW_TAG),
            )
        }
    }
}

/**
 * Opens the menu the way a user does: a raw pointer press at the control's centre.
 *
 * A semantic click on the button's own node would not do. The button is inert by design — a
 * transparent sibling box overlays it and owns the open — so the action a user performs belongs to
 * an element with no click semantics of its own.
 */
private fun ComposeUiTest.openOverflowMenu() {
    onNodeWithTag(OVERFLOW_TAG).performMouseInput {
        moveTo(center)
        press()
        release()
    }
}

/**
 * Rests the pointer on the overflow trigger without pressing it.
 *
 * Deliberately *not* [openOverflowMenu]. A press is a click outside the tooltip's own popup, which
 * Material answers by dismissing it — correct, and exactly what should happen, but it means the click
 * path can never be the thing that proves the tooltip appeared. Reading the label is a hover, so the
 * hover is what this has to do.
 */
private fun ComposeUiTest.hoverOverflowMenu() {
    onNodeWithTag(OVERFLOW_TAG).performMouseInput { moveTo(center) }
}

/**
 * The behaviour of the shared controls, and of the Material 3 components they were migrated onto.
 *
 * None of this was covered before. The controls in `Controls.kt` carry a few hundred call sites
 * between them — `AzkarFilterChip` alone has twenty-five — and the move to Material 3 Expressive
 * replaced the bodies of most of them wholesale: hand-drawn `Box`/`Row` chains became `Button`,
 * `IconButton`, `FilterChip`, `Switch` and `DropdownMenu`. Every one of those swaps changed how a
 * control *behaves*, not merely how it looks, and none of the change was asserted anywhere.
 *
 * The gap is not academic. The migrations replaced disabled-state handling (a whole-control
 * `alpha(0.5f)` with M3's `disabledContainerColor`/`disabledContentColor` pair), the interaction
 * plumbing (a bare `clickable` with `indication = null` against M3's ripple and
 * `minimumInteractiveComponentSize`), and in one case the popup behaviour of the row overflow menu.
 * Any of those can regress invisibly — a screenshot of a resting app cannot tell a button that
 * ignores clicks from one that works, because they look identical until someone presses them.
 *
 * These assert **behaviour and state only**: no colour, size or pixel expectations. Colour is the
 * design system's business and lives in the scheme. The reason is not squeamishness about coupling —
 * twice in this project a small upscaled screenshot crop was mistaken for a rendering defect, which
 * is what pushed the styling questions toward something a test can actually answer. What belongs
 * here is whether a control tells the caller the truth when the user touches it.
 */
val ControlBehaviourSuite by testSuite("Control behaviour") {
    // ---------------------------------------------------------------- buttons

    test("a primary button reports every click") {
        runRecompositionTrackingUiTest {
            var clicks = 0
            setTrackedContent {
                Themed {
                    PrimaryButton(
                        text = "Start session",
                        onClick = { clicks++ },
                        modifier = Modifier.testTag(CONTROL_TAG),
                    )
                }
            }
            resetControlBudget()
            repeat(3) { onNodeWithText("Start session").performClick() }
            assertThat(clicks).describedAs("three clicks, three callbacks").isEqualTo(3)
            assertControlBudget()
        }
    }

    /**
     * The `leadingIcon` sits inside the button's `RowScope` beside the label. If it ever absorbed
     * the click — by becoming separately clickable, or by measuring wide enough to take the row —
     * the button would go dead at every call site that passes an icon, of which there are many.
     */
    test("a primary button with a leading icon still reports its clicks") {
        runRecompositionTrackingUiTest {
            var clicks = 0
            setTrackedContent {
                Themed {
                    PrimaryButton(text = "Read", onClick = {
                        clicks++
                    }, leadingIcon = ActionIcons.read, modifier = Modifier.testTag(CONTROL_TAG))
                }
            }
            resetControlBudget()
            onNodeWithText("Read").performClick()
            assertThat(clicks).isEqualTo(1)
            assertControlBudget()
        }
    }

    test("a disabled primary button cannot be activated by any route") {
        runRecompositionTrackingUiTest {
            var clicks = 0
            setTrackedContent {
                Themed {
                    PrimaryButton(
                        text = "Start session",
                        onClick = { clicks++ },
                        enabled = false,
                        modifier = Modifier.testTag(CONTROL_TAG),
                    )
                }
            }
            resetControlBudget()
            assertDisabledAndInert(onNodeWithText("Start session"))
            assertThat(clicks).describedAs("a real pointer press must go nowhere").isZero()
            assertControlBudget()
        }
    }

    test("a secondary button reports its clicks") {
        runRecompositionTrackingUiTest {
            var clicks = 0
            setTrackedContent {
                Themed {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = { clicks++ },
                        modifier = Modifier.testTag(CONTROL_TAG),
                    )
                }
            }
            resetControlBudget()
            onNodeWithText("Cancel").performClick()
            assertThat(clicks).isEqualTo(1)
            assertControlBudget()
        }
    }

    test("a disabled secondary button cannot be activated") {
        runRecompositionTrackingUiTest {
            var clicks = 0
            setTrackedContent {
                Themed {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = { clicks++ },
                        enabled = false,
                        modifier = Modifier.testTag(CONTROL_TAG),
                    )
                }
            }
            resetControlBudget()
            assertDisabledAndInert(onNodeWithText("Cancel"))
            assertThat(clicks).isZero()
            assertControlBudget()
        }
    }

    test("a nav button reports its clicks") {
        runRecompositionTrackingUiTest {
            var clicks = 0
            setTrackedContent {
                Themed {
                    NavButton(
                        text = "Home",
                        selected = false,
                        onClick = { clicks++ },
                        modifier = Modifier.testTag(CONTROL_TAG),
                    )
                }
            }
            resetControlBudget()
            onNodeWithText("Home").performClick()
            assertThat(clicks).isEqualTo(1)
            assertControlBudget()
        }
    }

    // ----------------------------------------------------------- icon buttons

    test("an icon action button reports its clicks") {
        runRecompositionTrackingUiTest {
            var clicks = 0
            setTrackedContent {
                Themed {
                    IconActionButton(
                        icon = ActionIcons.favorite(on = true),
                        contentDescription = "Favourite",
                        onClick = { clicks++ },
                        modifier = Modifier.testTag(CONTROL_TAG),
                    )
                }
            }
            resetControlBudget()
            onNodeWithContentDescription("Favourite").performClick()
            assertThat(clicks).isEqualTo(1)
            assertControlBudget()
        }
    }

    test("a circle icon button reports its clicks") {
        runRecompositionTrackingUiTest {
            var clicks = 0
            setTrackedContent {
                Themed {
                    CircleIconButton(
                        icon = AzkarIcons.Star,
                        contentDescription = "Favourite",
                        onClick = { clicks++ },
                        modifier = Modifier.testTag(CONTROL_TAG),
                    )
                }
            }
            resetControlBudget()
            onNodeWithContentDescription("Favourite").performClick()
            assertThat(clicks).isEqualTo(1)
            assertControlBudget()
        }
    }

    test("a disabled circle icon button cannot be activated") {
        runRecompositionTrackingUiTest {
            var clicks = 0
            setTrackedContent {
                Themed {
                    CircleIconButton(
                        icon = AzkarIcons.Star,
                        contentDescription = "Favourite",
                        onClick = { clicks++ },
                        enabled = false,
                        modifier = Modifier.testTag(CONTROL_TAG),
                    )
                }
            }
            resetControlBudget()
            assertDisabledAndInert(onNodeWithContentDescription("Favourite"))
            assertThat(clicks).isZero()
            assertControlBudget()
        }
    }

    /**
     * The tooltip is a popup of its own layered over the button, and it must not eat the click that
     * was meant for the button underneath.
     */
    test("a tooltip icon button still reports its clicks") {
        runRecompositionTrackingUiTest {
            var clicks = 0
            setTrackedContent {
                Themed {
                    TooltipIconButton(
                        icon = AzkarIcons.Star,
                        tooltip = "Add to favourites",
                        onClick = { clicks++ },
                        modifier = Modifier.testTag(CONTROL_TAG),
                    )
                }
            }
            resetControlBudget()
            onNodeWithContentDescription("Add to favourites").performClick()
            assertThat(clicks).isEqualTo(1)
            assertControlBudget(atMost = 1, expectedFunction = "AzkarTooltip")
            assertComposableRecompositions("com.azkar.ui.components.CircleIconButton", 0..0)
        }
    }

    /**
     * The whole reason the wrappers exist: a glyph with nothing but a `contentDescription` says its
     * name to a screen reader and to nobody else.
     *
     * Both halves are asserted — it appears on a hover and it *goes away* on the way out. A tooltip
     * that shows once and never dismisses is worse than none, because it would sit over the control
     * the user is trying to reach, and it would fail in exactly the way a test that only checks the
     * appearance could not see.
     *
     * The button's own accessibility label and its tooltip are the same string, so this is also
     * asserting the two agree: a control that announced "Favourite" to a screen reader and popped up
     * "Add to favourites" for the mouse would be describing two different buttons.
     */
    test("a tooltip names the control while the pointer rests on it, and leaves when it does not") {
        runRecompositionTrackingUiTest {
            setTrackedContent { Themed { StarBesideElsewhere() } }
            resetControlBudget()
            onNodeWithText(STAR_TOOLTIP).assertDoesNotExist()

            onNodeWithContentDescription(STAR_TOOLTIP).performMouseInput { moveTo(center) }
            onNodeWithText(STAR_TOOLTIP).assertIsDisplayed()

            onNodeWithTag(ELSEWHERE_TAG).performMouseInput { moveTo(center) }
            onNodeWithText(STAR_TOOLTIP).assertDoesNotExist()
            assertControlBudget(atMost = 2, expectedFunction = "AzkarTooltip")
            assertComposableRecompositions("com.azkar.ui.components.CircleIconButton", 0..0)
        }
    }

    /**
     * The ellipsis hides what it does, so it is the glyph that most needs to announce itself.
     *
     * It is worth a test of its own rather than trusting the icon-button case, because this trigger is
     * the one control whose anchor is a [com.azkar.ui.components.AzkarTooltip] wrapping a whole
     * `Box` — the button, the transparent click-catcher that owns the open, and the menu — rather
     * than one that wraps a single button. A tooltip anchored to the wrong box would appear offset
     * from the control it names, or not at all.
     */
    test("an overflow menu's trigger names itself on hover") {
        runRecompositionTrackingUiTest {
            renderOverflowMenu { }
            resetControlBudget()
            onNodeWithText("Row actions").assertDoesNotExist()
            hoverOverflowMenu()
            onNodeWithText("Row actions").assertIsDisplayed()
            assertControlBudget(OVERFLOW_TAG, atMost = 1, expectedFunction = "AzkarTooltip")
            assertComposableRecompositions("com.azkar.ui.components.AzkarOverflowMenu", 0..1)
            assertComposableRecompositions("com.azkar.ui.components.CircleIconButton", 0..0)
        }
    }

    // ----------------------------------------------------------------- switch

    test("a switch reports the opposite of its current state, in both directions") {
        runRecompositionTrackingUiTest {
            val reported = mutableListOf<Boolean>()
            setTrackedContent { Themed { HostedSwitch(enabled = true, onReport = { reported += it }) } }
            waitForIdle()
            resetRecompositionCounts()
            onNodeWithTag(SWITCH_TAG).performClick()
            assertThat(reported).describedAs("off must report on").isEqualTo(listOf(true))
            onNodeWithTag(SWITCH_TAG).performClick()
            assertThat(reported).describedAs("on must report off").isEqualTo(listOf(true, false))
            onNodeWithTag(SWITCH_TAG).assertTrackedRecompositions(exactly = 2)
        }
    }

    test("a disabled switch cannot be activated") {
        runRecompositionTrackingUiTest {
            val reported = mutableListOf<Boolean>()
            setTrackedContent { Themed { HostedSwitch(enabled = false, onReport = { reported += it }) } }
            waitForIdle()
            resetRecompositionCounts()
            assertDisabledAndInert(onNodeWithTag(SWITCH_TAG))
            assertThat(reported).isEmpty()
            onNodeWithTag(SWITCH_TAG).assertTrackedStable()
        }
    }

    // ------------------------------------------------------------------ chips

    test("a pill toggle reports its clicks") {
        runRecompositionTrackingUiTest {
            var clicks = 0
            setTrackedContent {
                Themed {
                    PillToggle(
                        text = "Grid",
                        selected = true,
                        onClick = { clicks++ },
                        modifier = Modifier.testTag(CONTROL_TAG),
                    )
                }
            }
            resetControlBudget()
            onNodeWithText("Grid").performClick()
            assertThat(clicks).isEqualTo(1)
            assertControlBudget()
        }
    }

    test("a filter chip reports its clicks") {
        runRecompositionTrackingUiTest {
            var clicks = 0
            setTrackedContent {
                Themed {
                    AzkarFilterChip(text = "Favourites", selected = false, onClick = {
                        clicks++
                    }, modifier = Modifier.testTag(CONTROL_TAG))
                }
            }
            resetControlBudget()
            onNodeWithText("Favourites").performClick()
            assertThat(clicks).isEqualTo(1)
            assertControlBudget()
        }
    }

    /**
     * Both chips, selected and not.
     *
     * They are the same `FilterChip` differing only in one colour argument, which is exactly why
     * they used to be two hand-drawn `Row`s. Rendering both states catches a chip that only lays
     * itself out correctly when it happens to be the selected one — a real hazard now that the
     * unselected fill is transparent and carries no minimum size of its own.
     */
    test("chips lay out in both the selected and unselected states") {
        runRecompositionTrackingUiTest {
            setTrackedContent {
                Themed {
                    Column {
                        PillToggle(
                            text = "Grid",
                            selected = true,
                            onClick = {},
                            modifier = Modifier.testTag("grid-chip"),
                        )
                        PillToggle(
                            text = "List",
                            selected = false,
                            onClick = {},
                            modifier = Modifier.testTag("list-chip"),
                        )
                        AzkarFilterChip(
                            text = "All",
                            selected = true,
                            onClick = {},
                            modifier = Modifier.testTag("all-chip"),
                        )
                        AzkarFilterChip(
                            text = "Some",
                            selected = false,
                            onClick = {},
                            modifier = Modifier.testTag("some-chip"),
                        )
                    }
                }
            }
            resetControlBudget()
            listOf("Grid", "List", "All", "Some").forEach { onNodeWithText(it).assertIsDisplayed() }
            listOf("grid-chip", "list-chip", "all-chip", "some-chip").forEach { assertControlBudget(it) }
        }
    }

    // --------------------------------------------------------- overflow menu

    /**
     * The overflow menu changed more than any other control: its popup went from a hand-drawn
     * bordered `Box` to Material 3's `DropdownMenu`, whose `DropdownMenuItem` now owns the row
     * chrome, the icon slot and the click semantics.
     */
    test("an overflow menu reveals its items when opened") {
        runRecompositionTrackingUiTest {
            renderOverflowMenu { }
            resetControlBudget()
            onNodeWithText("Edit").assertDoesNotExist()
            openOverflowMenu()
            onNodeWithText("Edit").assertIsDisplayed()
            onNodeWithText("Duplicate").assertIsDisplayed()
            assertControlBudget(OVERFLOW_TAG, atMost = 2, expectedFunction = "AzkarTooltip")
            assertComposableRecompositions("com.azkar.ui.components.AzkarOverflowMenu", 0..1)
        }
    }

    test("choosing an overflow menu item fires its action and closes the menu") {
        runRecompositionTrackingUiTest {
            val picked = mutableListOf<String>()
            renderOverflowMenu { picked += it }
            openOverflowMenu()
            resetControlBudget()
            onNodeWithText("Duplicate").performClick()
            assertThat(picked).isEqualTo(listOf("Duplicate"))
            onNodeWithText("Duplicate").assertDoesNotExist()
            onNodeWithText("Edit").assertDoesNotExist()
            assertControlBudget(OVERFLOW_TAG, atMost = 2, expectedFunction = "AzkarTooltip")
            assertComposableRecompositions("com.azkar.ui.components.AzkarOverflowMenu", 0..1)
        }
    }

    // ------------------------------------------------------------ text field

    test("a text field shows its value and hides its placeholder") {
        runRecompositionTrackingUiTest {
            setTrackedContent {
                Themed {
                    AzkarOutlinedField(
                        value = "Ar-Rahman",
                        onValueChange = {},
                        placeholder = "Search collections",
                        modifier = Modifier.testTag(CONTROL_TAG),
                    )
                }
            }
            resetControlBudget()
            onNodeWithText("Ar-Rahman").assertIsDisplayed()
            onNodeWithText("Search collections").assertDoesNotExist()
            assertControlBudget()
        }
    }

    test("a text field shows its placeholder while empty") {
        runRecompositionTrackingUiTest {
            setTrackedContent {
                Themed {
                    AzkarOutlinedField(
                        value = "",
                        onValueChange = {},
                        placeholder = "Search",
                        modifier = Modifier.testTag(CONTROL_TAG),
                    )
                }
            }
            resetControlBudget()
            onNodeWithText("Search").assertIsDisplayed()
            assertControlBudget()
        }
    }

    test("a text field reports the text typed into it") {
        runRecompositionTrackingUiTest {
            var value = ""
            setTrackedContent {
                Themed {
                    HostedField(
                        onReport = { value = it },
                        modifier = Modifier.testTag("hosted-field"),
                    )
                }
            }
            waitForIdle()
            resetRecompositionCounts()
            onNode(hasSetTextAction()).performTextInput("Ar-Rah")
            assertThat(value).isEqualTo("Ar-Rah")
            onNodeWithTag("hosted-field").assertTrackedRecompositions(atLeast = 1, atMost = 2)
        }
    }

    test("a text field renders its supporting text") {
        runRecompositionTrackingUiTest {
            setTrackedContent {
                Themed {
                    AzkarOutlinedField(
                        value = "",
                        onValueChange = {},
                        placeholder = "Tags",
                        supportingText = "Comma separated",
                        modifier = Modifier.testTag(CONTROL_TAG),
                    )
                }
            }
            resetControlBudget()
            onNodeWithText("Comma separated").assertIsDisplayed()
            assertControlBudget()
        }
    }

    /**
     * `minHeight` is a floor, and Material's own floor wins where the two disagree.
     *
     * Both halves matter. A field that ignored a large `minHeight` would clip the multiline import
     * and description dialogs; one that honoured a tiny one would collapse below what Material
     * draws inside itself.
     */
    test("a text field honours a min height above Material's own floor") {
        runRecompositionTrackingUiTest {
            setTrackedContent {
                Themed {
                    AzkarOutlinedField(
                        value = "",
                        onValueChange = {},
                        placeholder = "Description",
                        minHeight = TALL_FIELD_HEIGHT,
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth().testTag(TALL_FIELD_TAG),
                    )
                }
            }
            resetControlBudget()
            val height = onNodeWithTag(TALL_FIELD_TAG).fetchSemanticsNode().boundsInRoot.height
            assertThat(height).describedAs("minHeight=$TALL_FIELD_HEIGHT").isGreaterThanOrEqualTo(72f)
            assertControlBudget(TALL_FIELD_TAG)
        }
    }

    test("a text field never renders shorter than Material's floor") {
        runRecompositionTrackingUiTest {
            setTrackedContent {
                Themed {
                    AzkarOutlinedField(
                        value = "",
                        onValueChange = {},
                        placeholder = "Search",
                        minHeight = SHORT_FIELD_HEIGHT,
                        modifier = Modifier.fillMaxWidth().testTag(SHORT_FIELD_TAG),
                    )
                }
            }
            resetControlBudget()
            val height = onNodeWithTag(SHORT_FIELD_TAG).fetchSemanticsNode().boundsInRoot.height
            assertThat(height)
                .describedAs("Material's OutlinedTextField floor is $MATERIAL_FIELD_FLOOR")
                .isGreaterThanOrEqualTo(MATERIAL_FIELD_FLOOR.value)
            assertControlBudget(SHORT_FIELD_TAG)
        }
    }

    // ------------------------------------------------------------- tab strip

    test("a tab strip shows every tab and reports the index chosen") {
        runRecompositionTrackingUiTest {
            val chosen = mutableListOf<Int>()
            setTrackedContent {
                Themed {
                    AzkarTabStrip(
                        tabs = listOf("Overview", "Members", "Activity"),
                        selectedIndex = 0,
                        onSelect = { chosen += it },
                        modifier = Modifier.testTag(CONTROL_TAG),
                    )
                }
            }
            resetControlBudget()
            listOf("Overview", "Members", "Activity").forEach { onNodeWithText(it).assertIsDisplayed() }
            onNodeWithText("Members").performClick()
            assertThat(chosen).isEqualTo(listOf(1))
            onNodeWithText("Activity").performClick()
            assertThat(chosen).isEqualTo(listOf(1, 2))
            assertControlBudget()
        }
    }

    // ------------------------------------------------------------ containers

    /**
     * The three containers are the only shared surfaces that moved to the Styles API, so they are
     * the only ones whose chrome a caller can now override by passing a `Style`. Rendering each one's
     * content proves the styleable modifier did not swallow it along with the padding and border.
     */
    test("the shared surfaces all render their content") {
        runRecompositionTrackingUiTest {
            setTrackedContent {
                Themed {
                    Column {
                        AzkarCard(modifier = Modifier.testTag("card")) {
                            Box(Modifier.testTag("card-content").size(PROBE_SIZE))
                        }
                        BorderedStateBox(modifier = Modifier.testTag("state-box")) {
                            Box(Modifier.testTag("state-box-content").size(PROBE_SIZE))
                        }
                        AzkarDialogSurface(modifier = Modifier.testTag("dialog")) {
                            Box(Modifier.testTag("dialog-content").size(PROBE_SIZE))
                        }
                    }
                }
            }
            resetControlBudget()
            listOf("card-content", "state-box-content", "dialog-content").forEach {
                onNodeWithTag(it).assertIsDisplayed()
            }
            listOf("card", "state-box", "dialog").forEach { assertControlBudget(it) }
        }
    }
}
