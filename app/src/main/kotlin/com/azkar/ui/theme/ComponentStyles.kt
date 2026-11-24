package com.azkar.ui.theme

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.StyleScope
import androidx.compose.foundation.style.StyleState
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Token access from inside a [Style].
 *
 * A style is a plain function over [StyleScope] — it is *not* `@Composable` — so the usual
 * `AzkarTheme.colors` facade is unavailable in one, and without this every entry in
 * [ComponentStyles] would have to take its colours as a constructor argument and every caller would
 * have to remember to pass the light or the dark set by hand.
 *
 * [StyleScope] extends `CompositionLocalAccessorScope`, which is how the read works: `currentValue`
 * is the non-composable sibling of `CompositionLocal.current`, declared as a member extension of
 * that scope, and it is what the framework expects a style to use. Reading the local from the scope
 * is what lets a style be a `val` on a singleton object and still follow the theme.
 *
 * [StyleScope] is also a `Density`, so `dp` values resolve inside a style without anything being
 * converted in by hand.
 */
val StyleScope.colors: AzkarColors
    get() = LocalAzkarColors.currentValue

val shapes: Shapes
    get() = AzkarShapes

/**
 * The same padding on all four sides.
 *
 * `Style` spells padding out as four separate setters and offers no `all`, which is the right default
 * — an asymmetric control is common enough that collapsing them would be lossy — but it means the
 * common case costs four lines. This is that case.
 */
private fun StyleScope.pad(all: Dp) {
    contentPaddingStart(all)
    contentPaddingEnd(all)
    contentPaddingTop(all)
    contentPaddingBottom(all)
}

/**
 * The styleable properties of the components Material 3 does not ship.
 *
 * Material's own components bring their styling with them — a `Button` already knows it is 40dp tall
 * and round-cornered — so there is nothing to declare for them, and the Expressive artifact
 * deliberately does not expose per-component `Style` slots. This catalogue is for the opposite case:
 * the surfaces the app still draws by hand, because Material has no component for them and Expressive
 * has no recipe to lend either.
 *
 * That is a short list on purpose, and it got shorter with the Expressive adoption rather than
 * longer. `AzkarCard` is now a `Card`, every button is a `Button`, every chip is a `FilterChip`; what
 * is left is the painted card, the state pane and the dialog pane. Declaring a style for a component
 * that already has a Material one would mean maintaining two descriptions of the same surface, which
 * is the problem this file exists to remove rather than to add to.
 *
 * Not one of these entries declares a `pressed`, `hovered` or `disabled` block, and that is not an
 * omission. Every surface still hand-drawn at this point is a *container* — it is painted, or it is a
 * pane, or it is a modal — and none of them has anything to react to. `Modifier.styleable` can drive
 * those off an interaction source at no cost, so the moment one of these does become interactive the
 * block belongs here rather than in the component.
 *
 * Every value below is read through the [StyleScope] extensions, so no entry holds a colour that
 * cannot change with the theme.
 */
object ComponentStyles {
    /**
     * The full-bleed pane behind an empty or loading collection.
     *
     * A card at the smallest [CardSize], so most of this is the shape of [com.azkar.ui.components.AzkarCard]
     * carried over rather than decided here. What it does decide is the inset: this pane sits *inside* a scroll
     * stack, and the shadow that reads as lift when a card is the whole screen reads as a seam when it is one
     * item among many.
     */
    val borderedStateBox: Style = {
        shape(shapes.medium)
        background(colors.cardBackground)
        borderWidth(1.dp)
        borderColor(colors.border)
        pad(CardSize.SM.padding)
    }

    /**
     * The dialog pane every modal in the app is drawn on.
     *
     * Twelve call sites share this one surface, and the single most valuable thing it fixes is that
     * they cannot disagree about their padding — a dialog whose content sits 4dp further in than its
     * neighbour's reads as a mistake rather than as a choice.
     *
     * The radius is `extraLarge` and not the top of the ramp, because a dialog is the one surface
     * whose width is set by its content: at the narrow end a 48dp corner stops reading as a rounded
     * rectangle and starts reading as a blob.
     */
    val dialogSurface: Style = {
        shape(shapes.extraLarge)
        background(colors.cardBackground)
        borderWidth(1.dp)
        borderColor(colors.border)
        pad(AzkarSpacing.xl)
    }
}

/**
 * A [StyleState] for a surface that has nothing to react to.
 *
 * `Modifier.styleable` always takes one, which is why every component carrying a [Style] needs to
 * produce a state even when none of its entries read it. This is that production, in one place: an
 * interaction source that nothing drives, so every flag in the state stays false and any `pressed`
 * or `disabled` block a style declares is simply never entered.
 *
 * Components that *do* interact should not use this — they want [rememberUpdatedStyleState] wired to
 * the interaction source that actually carries the press, so the style changes on the real event
 * instead of on a state somebody set by hand.
 */
@Composable
fun inertStyleState(): StyleState = rememberUpdatedStyleState(remember { MutableInteractionSource() })
