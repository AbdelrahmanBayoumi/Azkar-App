@file:OptIn(ExperimentalAtomicApi::class)

package com.azkar.ui.shell

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.azkar.i18n.LocaleStore
import com.azkar.i18n.activateLocale
import com.azkar.i18n.isSameLanguageAs
import com.azkar.ui.theme.AppTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Locale
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Colour preference, owned by the settings screen, ported from the `ColorScheme` scene property.
 *
 * [SYSTEM] is a real choice rather than only a launch default. The header's toggle could not offer
 * it — a lone glyph with no label, standing in for "whatever the desktop is doing", reads as nothing
 * — but a named option in a list of three has nowhere to hide, so following the OS is back.
 */
enum class ThemePreference { SYSTEM, LIGHT, DARK }

/**
 * Shell state holder, replacing `MainScreenController` + the scene-level theme listener.
 *
 * Kept out of the composable tree so a language switch recomposes only the content, and so the
 * library screen can be created lazily exactly like the original `libraryComponent` field.
 *
 * What lives here is only what the shell owns on its own behalf — the active language and the colour
 * preference. Which screen is on screen is *not*: that moved to the Navigation 3 back stack in
 * [AzkarNavigator], because an enum held exactly one destination and so had no way to express history
 * at all. Neither is whether the settings screen is open, which is [AzkarDestination.Settings] on that
 * stack for the same reason: it is navigation, not a boolean the shell has to keep in step with the
 * screen being drawn.
 *
 * [locale] is the app's single source of truth for the active language, provided to the tree through
 * `LocalUiLocale`. It starts from the JVM default because [LocaleStore.readAndActivate] has already
 * resolved the stored value into it before the window opens, and every later change is written to
 * both it and the JVM default — resources read the latter, everything else reads the former.
 *
 * [themePreference] follows the same shape for the same reason: it is seeded from the stored value
 * by [ThemePreferenceStore.read] and every change is handed to [persistTheme]. [typography] is the
 * third of this family and the newest: the reader's font and text size, seeded by [TypographyStore.read]
 * and handed to [persistTypography].
 *
 * All three persistence callbacks are required rather than defaulting to the real writes. They used to
 * default to `::persistThemePreference` and `::persistLocale`, which quietly meant "write to the
 * developer's own `settings.preferences_pb`" — a default no caller other than production could
 * survive. [rememberAzkarAppState] supplies them from the container, so the one place that knows
 * which store is in use is also the only place that names it.
 */
@Stable
class AzkarAppState(
    initialThemePreference: ThemePreference = ThemePreference.SYSTEM,
    initialLocale: Locale = Locale.getDefault(),
    initialTypography: AppTypography = AppTypography(),
    /** Injected so a test can observe the write without touching the real settings file. */
    private val persistTheme: (ThemePreference) -> Unit,
    /** Injected so a test can observe the write without touching the real settings file. */
    private val persist: (Locale) -> Unit,
    /** Injected so a test can observe the write without touching the real settings file. */
    private val persistTypography: (AppTypography) -> Unit,
) {
    var themePreference: ThemePreference by mutableStateOf(initialThemePreference)
        private set

    var locale: Locale by mutableStateOf(initialLocale)
        private set

    /**
     * The reader's font and text size.
     *
     * Held here rather than in the settings screen because it is not a settings-screen concern: it
     * governs the whole app including the overlay window, and the two roots that install `AzkarTheme`
     * are the only things that need to hear about it.
     */
    var typography: AppTypography by mutableStateOf(initialTypography)
        private set

    val toasts: MutableList<ToastMessage> = mutableStateListOf()

    /**
     * Enter/exit state for each live toast, keyed by [ToastMessage.id].
     *
     * It lives here rather than inside the card because the two halves of a toast's life are driven
     * from opposite ends: the controller starts the clock, and only the card can say when the exit
     * has finished playing. A card keeping its own state would have nowhere to report that back to.
     * Plain [MutableMap] on purpose — [MutableTransitionState] is observable on its own, so the map
     * itself never has to trigger recomposition.
     */
    internal val toastVisibility: MutableMap<Long, MutableTransitionState<Boolean>> = mutableMapOf()

    /** Starts [id]'s toast on its way out. The card retires it once that exit has played. */
    internal fun dismissToast(id: Long) {
        toastVisibility[id]?.targetState = false
    }

    /**
     * Drops [id] for good, called by its card once nothing is drawing it any more.
     *
     * Removing from [toasts] is deferred until this point so that a toast does not blink out of
     * existence the instant its six seconds are up.
     */
    internal fun retireToast(id: Long) {
        toastVisibility.remove(id)
        toasts.removeAll { it.id == id }
    }

    /**
     * Switches to [preference] and stores it for the next run.
     *
     * The caller states the target directly rather than asking for a toggle, because the settings
     * screen lists all three options rather than cycling them: a tap on the option already chosen is
     * the honest no-op that a three-state cycle has to fake its way around.
     *
     * Persisting is what makes the picker worth having. It used to move [themePreference] and stop
     * there, so the choice survived until the window closed and the next launch opened in
     * [ThemePreference.SYSTEM] — the app looked like it had forgotten the toggle. The write goes to
     * [persistTheme] and is fire-and-forget: the palette has already changed on screen, and a lost
     * write costs one theme choice rather than the click.
     */
    fun selectTheme(preference: ThemePreference) {
        themePreference = preference
        persistTheme(preference)
    }

    /**
     * Switches to [target], for this run and every future one.
     *
     * Both halves are needed and neither derives from the other: the JVM default is what
     * `org.jetbrains.compose.resources` reads, and [locale] is what the tree recomposes off. Nothing
     * closes here either — the settings screen stays on screen, and `key(state.locale)` rebuilds it
     * in the new language underneath the user, which is what retired the dialog's OK button.
     */
    fun applyLocale(target: Locale) {
        if (target.isSameLanguageAs(locale)) return
        target.activateLocale()
        locale = target
        persist(target)
    }

    /**
     * Draws the app in [name], and stores it.
     *
     * [AppTypography.AZKAR_FONT] is the empty string rather than a name, so "back to the bundled
     * faces" needs no special case here — it is the same call with the same argument.
     */
    fun selectFont(name: String) = updateTypography { copy(family = name) }

    /**
     * Draws the app at [percent] of its size, and stores it.
     *
     * Clamped on the way in rather than at the call site: the value arrives from a slider, but the
     * store is the thing that also reads values written by other builds of the app, so it is the only
     * place both paths meet.
     */
    fun setTextScale(percent: Int) = updateTypography { copy(scalePercent = percent) }

    /**
     * The one place a typographic change is applied, so that the two halves of [typography] are never
     * written by something that forgets the write.
     */
    private fun updateTypography(transform: AppTypography.() -> AppTypography) {
        typography = transform(typography).coerced()
        persistTypography(typography)
    }
}

/** One `.library-toast` popup, ported from `LibraryOverlaySupport.showNotificationPopup`. */
@Immutable
data class ToastMessage(
    val id: Long,
    val title: String,
    val message: String,
    val priority: NotificationPriority,
    val onClick: (() -> Unit)? = null,
)

private val toastIds = AtomicLong(0L)

/**
 * Auto-dismisses each toast after [TOAST_VISIBLE_MILLIS], matching the 6s JavaFX timeout.
 *
 * The JavaFX original hopped back with `Platform.runLater`; here the same lifetime is expressed by
 * launching a delayed dismissal on the composition scope, so callers can fire toasts from any
 * thread. It dismisses rather than removes, because the card is what knows when the toast has
 * finished leaving — see [AzkarAppState.retireToast].
 */
@Stable
class ToastController(
    private val state: AzkarAppState,
    private val scope: CoroutineScope,
) {
    fun show(
        title: String,
        message: String,
        priority: NotificationPriority,
        onClick: (() -> Unit)? = null,
    ) {
        val toast = ToastMessage(toastIds.incrementAndFetch(), title, message, priority, onClick)
        state.toasts.add(toast)
        scope.launch {
            delay(TOAST_VISIBLE_MILLIS.milliseconds)
            state.dismissToast(toast.id)
        }
    }
}

@Composable
fun rememberToastController(state: AzkarAppState): ToastController {
    val scope = rememberCoroutineScope()
    return remember(state, scope) { ToastController(state, scope) }
}

const val TOAST_VISIBLE_MILLIS = 6_000L

/**
 * `remember`ed shell state, hoisted once by [AzkarApp].
 *
 * The two persistence callbacks are built here from the container's [LocaleStore],
 * [ThemePreferenceStore] and [TypographyStore] rather than defaulted inside [AzkarAppState], so that
 * a state holder built
 * by hand — a test, or the next caller that is not a screen — has to say what a language, theme or
 * typographic change should do to the settings file instead of inheriting the answer from wherever
 * the code happens to sit.
 */
@Composable
fun rememberAzkarAppState(
    initialThemePreference: ThemePreference = ThemePreference.SYSTEM,
    initialLocale: Locale = Locale.getDefault(),
    initialTypography: AppTypography = AppTypography(),
): AzkarAppState {
    val locales = koinInject<LocaleStore>()
    val themePreferences = koinInject<ThemePreferenceStore>()
    val typography = koinInject<TypographyStore>()
    return remember(locales, themePreferences, typography) {
        AzkarAppState(
            initialThemePreference = initialThemePreference,
            initialLocale = initialLocale,
            initialTypography = initialTypography,
            persistTheme = { themePreferences.persist(it) },
            persist = { locales.persist(it) },
            persistTypography = { typography.persist(it) },
        )
    }
}
