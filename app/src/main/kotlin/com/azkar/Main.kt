package com.azkar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.azkar.data.config.dataModule
import com.azkar.generated.resources.Res
import com.azkar.generated.resources.title
import com.azkar.generated.resources.updateAvailableMessage
import com.azkar.generated.resources.updateAvailableTitle
import com.azkar.generated.resources.updateDownloadingMessage
import com.azkar.generated.resources.updateDownloadingTitle
import com.azkar.generated.resources.updateReadyMessage
import com.azkar.generated.resources.updateReadyTitle
import com.azkar.i18n.LocaleStore
import com.azkar.ui.components.AzkarIcons
import com.azkar.ui.notifications.NotificationAction
import com.azkar.ui.notifications.NotificationActionRouter
import com.azkar.ui.notifications.NotificationNotice
import com.azkar.ui.notifications.NotificationPreferencesRepository
import com.azkar.ui.notifications.NotificationRuntime
import com.azkar.ui.notifications.StartupRegistration
import com.azkar.ui.notifications.presentation.NotificationOverlayWindow
import com.azkar.ui.notifications.presentation.NotificationPublisher
import com.azkar.ui.notifications.presentation.OverlayNotificationPresenter
import com.azkar.ui.notifications.presentation.OverlayTheme
import com.azkar.ui.shell.AzkarApp
import com.azkar.ui.shell.AzkarAppState
import com.azkar.ui.shell.DesktopPrayerSurfaces
import com.azkar.ui.shell.NotificationPriority
import com.azkar.ui.shell.ThemePreferenceStore
import com.azkar.ui.shell.TrayMenuActions
import com.azkar.ui.shell.TypographyStore
import com.azkar.ui.shell.rememberAzkarAppState
import com.azkar.ui.shell.rememberAzkarDarkTheme
import com.azkar.ui.shell.rememberToastController
import com.azkar.ui.shell.trayAvailable
import com.azkar.ui.theme.AppTypography
import com.azkar.ui.theme.DEFAULT_WINDOW_HEIGHT
import com.azkar.ui.theme.DEFAULT_WINDOW_WIDTH
import com.azkar.ui.theme.windowSizeFor
import com.azkar.updates.UpdateManager
import com.azkar.updates.UpdateToastGate
import com.azkar.updates.runUpdateCheckSchedule
import dev.nucleusframework.autolaunch.AutoLaunch
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform
import java.awt.Toolkit
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

/**
 * `--overlay-poc`, the manual overlay verification harness.
 *
 * The overlay's behaviour is only half testable: the presenter is unit-tested, but "is it actually
 * above that other window", "does it sit on the right screen", "does typing still reach the game"
 * and "does a hover really freeze the timer" are questions about a window manager, and no assertion
 * about them is worth writing. This flag exists so they can be answered by eye in one run:
 *
 * ```
 * azkar --overlay-poc            # start hidden and fire six cards, then leave the app up
 * ```
 *
 * It deliberately does *not* start [NotificationRuntime], so the queue cannot add a card of its own
 * and confuse what is being observed.
 */
private const val OVERLAY_PROOF_OF_CONSENT_FLAG = "--overlay-poc"

/** Shows one synthetic update notice so its overlay can be checked without depending on a release. */
private const val UPDATE_NOTIFICATION_PREVIEW_FLAG = "--update-notification-preview"

/**
 * Compose Multiplatform replacement for `com.azkar.Launcher` + `com.azkar.Home`.
 *
 * The JavaFX application warmed the database ([com.azkar.data.config.DatabaseManager.warmUp], which creates the schema
 * and opens the connection pool) off the UI thread before showing the stage and closed it again in
 * `stop()`. The Compose entry point keeps that contract — [DatabaseWarmUp] runs the warm-up before
 * showing the window, while the notification runtime and data stores live until the explicit tray
 * Quit action shuts the process down.
 */
suspend fun main(args: Array<String>) {
    if (args.contentEquals(arrayOf("--packaging-smoke-test"))) {
        verifyPackagedRuntime()
        return
    }
    pinDesktopWindowClass()
    logger.info { "Starting Azkar" }
    startKoin { modules(dataModule, appModule) }
    val koin = KoinPlatform.getKoin()
    val startup = prepareDesktopStartup(koin)
    runDesktopApplication(args, koin, startup)
}

/**
 * Build the AWT toolkit here, on the main thread, so the window claims to be this application.
 *
 * A desktop window is matched to the `.desktop` entry that launched it by its `WM_CLASS`, and
 * AWT decides what that class is by looking at the bottom frame of the stack that happened to
 * build the toolkit first: `sun.awt.X11.XToolkit` takes the last frame of its own stack trace,
 * turns the dots into hyphens and uses that as the application class. The toolkit is a
 * process-wide singleton, so whichever thread gets there first decides what every window of this
 * process calls itself, for the rest of the run.
 *
 * Left to itself that thread is a coroutine worker. The startup work suspends (the database
 * warm-up and the stores read), so AWT is first touched back on a `Dispatchers.Default` worker,
 * whose class name is `kotlinx.coroutines.scheduling.CoroutineScheduler$Worker` — and the window
 * then announced itself as `kotlinx-coroutines-scheduling-CoroutineScheduler$Worker`. No installed
 * `.desktop` entry says that, so the window manager could not tell the window apart from the
 * application it belongs to: the dock grew a second entry for it, under a Java icon and under
 * that class name, instead of showing the Azkar one.
 *
 * Doing it here fixes the name at the source. The body of a `suspend fun main` starts on the
 * calling thread, so the bottom frame is `com.azkar.MainKt`, the class becomes
 * `com-azkar-MainKt`, and that is exactly what the packager writes into the `.desktop` entry's
 * `StartupWMClass` — the two are derived from the same class name and cannot drift apart. The
 * toolkit is headless-only on a machine with no display, where there is no window to name, so the
 * failure is logged and ignored.
 */
private fun pinDesktopWindowClass() {
    runCatching { Toolkit.getDefaultToolkit() }
        .onFailure { failure -> logger.debug(failure) { "Unable to initialise the AWT toolkit" } }
}

private data class DesktopStartup(
    val databaseWarmUp: DatabaseWarmUp,
    val themePreference: com.azkar.ui.shell.ThemePreference,
    val typography: AppTypography,
)

private suspend fun prepareDesktopStartup(koin: org.koin.core.Koin): DesktopStartup {
    koin.get<LocaleStore>().readAndActivate()
    val preferences = koin.get<NotificationPreferencesRepository>()
    preferences.refresh()
    koin
        .get<StartupRegistration>()
        .migrateLegacyRegistration(preferences.startOnLogin.value)
        .onFailure { logger.warn(it) { "Unable to migrate the legacy login registration" } }
    val warmUp = DatabaseWarmUp().apply { start() }
    val theme = koin.get<ThemePreferenceStore>().read()
    val typography = koin.get<TypographyStore>().read()
    return DesktopStartup(warmUp, theme, typography)
}

private fun runDesktopApplication(
    args: Array<String>,
    koin: org.koin.core.Koin,
    startup: DesktopStartup,
) {
    application {
        DesktopApplicationRoot(args, koin, startup) { exitApplication() }
    }
}

@Composable
private fun rememberStartedAtLogin(args: Array<String>): Boolean =
    remember(args) {
        "--start-hidden" in args ||
            "--nucleus-autostart" in args ||
            runCatching { AutoLaunch.wasStartedAtLogin(args) }
                .onFailure { logger.warn(it) { "Unable to detect a login launch" } }
                .getOrDefault(false)
    }

@Composable
private fun rememberMainWindowVisibility(
    trayAvailable: Boolean,
    startedAtLogin: Boolean,
    proofOfConcept: Boolean,
    updateNotificationPreview: Boolean,
): MutableState<Boolean> {
    val visibility = remember { mutableStateOf(!(startedAtLogin && trayAvailable)) }
    LaunchedEffect(trayAvailable, proofOfConcept, updateNotificationPreview, startedAtLogin) {
        // The harness verifies notifications with the main window hidden.
        visibility.value = !proofOfConcept && !(startedAtLogin && trayAvailable)
    }
    return visibility
}

@Composable
private fun DesktopApplicationRoot(
    args: Array<String>,
    koin: org.koin.core.Koin,
    startup: DesktopStartup,
    exitApplication: () -> Unit,
) {
    val trayAvailable = trayAvailable()
    val startedAtLogin = rememberStartedAtLogin(args)
    val proofOfConcept = OVERLAY_PROOF_OF_CONSENT_FLAG in args
    val updateNotificationPreview = UPDATE_NOTIFICATION_PREVIEW_FLAG in args
    var windowVisible by rememberMainWindowVisibility(
        trayAvailable,
        startedAtLogin,
        proofOfConcept,
        updateNotificationPreview,
    )
    val composeScope = rememberCoroutineScope()
    val actionRouter = remember { koin.get<NotificationActionRouter>() }
    // A notification activation, a tray click and an overlay card all arrive on somebody else's
    // thread, so showing the window is handed to the composition's scope rather than written from
    // there. The router runs this half before the navigation half, so a reminder that arrives before
    // the tree has ever composed still brings the window up.
    val showWindow: () -> Unit = { composeScope.launch { windowVisible = true } }
    SideEffect {
        actionRouter.setBeforeNavigate { showWindow() }
    }

    // One state for the whole application: the main tree and the overlay window are two separate
    // windows, and two state holders would mean two theme preferences and two locales, so the
    // overlay would stop looking like the app the moment either of them changed.
    val appState =
        rememberAzkarAppState(
            initialThemePreference = startup.themePreference,
            initialTypography = startup.typography,
        )

    val preferences = remember { koin.get<NotificationPreferencesRepository>() }
    val runtime = remember { koin.get<NotificationRuntime>() }
    val dispatcher = koin.get<NotificationPublisher>()
    val overlay = koin.get<OverlayNotificationPresenter>()
    val notificationScope = koin.get<CoroutineScope>(named("notificationScope"))
    NotificationRuntimeLifecycle(runtime, notificationScope, proofOfConcept || updateNotificationPreview)
    val quitAction = rememberQuitAction(koin, startup, notificationScope, dispatcher, exitApplication)
    UpdateFeature(koin, appState, actionRouter, updateNotificationPreview)

    // The overlay is its own window, created once and hidden when there is nothing to say. It is
    // composed beside the main window rather than inside it, so a reminder still arrives while the
    // app has no visible surface at all — which is the state the tray leaves it in.
    NotificationOverlayHost(appState, preferences, overlay, actionRouter, runtime)

    if (proofOfConcept) {
        OverlayProofOfConcept(dispatcher)
    }
    if (trayAvailable) {
        DesktopPrayerSurfaces(
            appState = appState,
            dispatcher = dispatcher,
            actionRouter = actionRouter,
            preferences = preferences,
            menuActions =
                TrayMenuActions(
                    openApp = showWindow,
                    quit = quitAction,
                    panelHidden = { windowVisible = false },
                ),
        )
    }

    MainWindow(
        visible = windowVisible,
        onCloseRequest = { if (trayAvailable) windowVisible = false else quitAction() },
        appState = appState,
        actionRouter = actionRouter,
    )
}

@Composable
private fun rememberQuitAction(
    koin: org.koin.core.Koin,
    startup: DesktopStartup,
    notificationScope: CoroutineScope,
    dispatcher: NotificationPublisher,
    exitApplication: () -> Unit,
): () -> Unit {
    val shutdownScope = rememberCoroutineScope()
    return remember(
        koin,
        startup.databaseWarmUp,
        notificationScope,
        dispatcher,
        exitApplication,
        shutdownScope,
    ) {
        {
            shutdownScope.launch {
                shutdown(
                    koin = koin,
                    databaseWarmUp = startup.databaseWarmUp,
                    notificationScope = notificationScope,
                    dispatcher = dispatcher,
                    exitApplication = exitApplication,
                )
            }
        }
    }
}

@Composable
private fun UpdateFeature(
    koin: org.koin.core.Koin,
    appState: AzkarAppState,
    actionRouter: NotificationActionRouter,
    showPreview: Boolean,
) {
    val manager = remember { koin.get<UpdateManager>() }
    UpdateLifecycle(manager, appState, actionRouter, enabled = !showPreview)
    if (showPreview) UpdateNotificationPreview(appState, actionRouter)
}

@Composable
private fun NotificationRuntimeLifecycle(
    runtime: NotificationRuntime,
    scope: CoroutineScope,
    proofOfConcept: Boolean,
) {
    LaunchedEffect(runtime, proofOfConcept) {
        // Keep the harness deterministic: it fires its own cards and never starts the scheduler.
        if (!proofOfConcept) runtime.start(scope)
    }
}

@Composable
private fun UpdateLifecycle(
    manager: UpdateManager,
    appState: AzkarAppState,
    actionRouter: NotificationActionRouter,
    enabled: Boolean,
) {
    val toastController = rememberToastController(appState)
    LaunchedEffect(manager, toastController, actionRouter, enabled) {
        val toastGate = UpdateToastGate()
        launch {
            manager.status.collect { status ->
                if (enabled) {
                    val toast =
                        when (status) {
                            is com.azkar.updates.UpdateStatus.Available ->
                                if (toastGate.shouldToast("available:${status.info.version}")) {
                                    getString(Res.string.updateAvailableTitle) to
                                        getString(Res.string.updateAvailableMessage, status.info.version)
                                } else {
                                    null
                                }
                            is com.azkar.updates.UpdateStatus.Downloading ->
                                if (toastGate.shouldToast("downloading:${status.version}")) {
                                    getString(Res.string.updateDownloadingTitle) to
                                        getString(Res.string.updateDownloadingMessage, status.version)
                                } else {
                                    null
                                }
                            is com.azkar.updates.UpdateStatus.ReadyToInstall ->
                                if (toastGate.shouldToast("ready:${status.info.version}")) {
                                    getString(Res.string.updateReadyTitle) to
                                        getString(Res.string.updateReadyMessage, status.info.version)
                                } else {
                                    null
                                }
                            else -> null
                        }
                    toast?.let { (title, message) ->
                        toastController.show(
                            title = title,
                            message = message,
                            priority = NotificationPriority.HIGH,
                            onClick = { actionRouter.dispatch(NotificationAction.Update) },
                        )
                    }
                }
            }
        }
        if (enabled) runUpdateCheckSchedule(manager::checkAutomatically)
    }
}

/**
 * The one place the process is allowed to end, in the order that leaves nothing half-released.
 *
 * The scheduler's scope goes first so nothing new is scheduled while the rest is being torn down;
 * the publisher next, so the overlay's stack stops; then the database, the one HTTP client, and
 * Koin itself. Anything that can fail during shutdown is inside a `runCatching` on purpose — a tray
 * menu item that threw on the way out would leave the window gone and the process alive, which is
 * the one state with no way back into it.
 */
private suspend fun shutdown(
    koin: org.koin.core.Koin,
    databaseWarmUp: DatabaseWarmUp,
    notificationScope: CoroutineScope,
    dispatcher: NotificationPublisher,
    exitApplication: () -> Unit,
) {
    notificationScope.coroutineContext[Job]?.cancelChildren()
    runCatching { dispatcher.dispose() }
        .onFailure { failure -> logger.warn(failure) { "Unable to dispose the notification publisher" } }
    notificationScope.cancel()
    koin.get<CoroutineScope>(named("updateScope")).cancel()
    databaseWarmUp.close()
    runCatching { koin.get<HttpClient>().close() }
        .onFailure { failure -> logger.warn(failure) { "Unable to close the HTTP client" } }
    stopKoin()
    logger.info { "Azkar is shutting down" }
    exitApplication()
}

/**
 * The notification overlay window, and the app state it has to be drawn with.
 *
 * The theme and language are read from the *same* [AzkarAppState] the main tree uses, which is why
 * that state is created up here and passed down rather than being remembered inside `MainWindow`: a
 * second instance would be a second theme preference and a second locale, and the overlay would
 * drift away from the app the moment either was changed. It is also why the overlay window is not
 * re-created when the theme changes — only its content is recomposed.
 */
@Composable
private fun NotificationOverlayHost(
    appState: AzkarAppState,
    preferences: NotificationPreferencesRepository,
    overlay: OverlayNotificationPresenter,
    actionRouter: NotificationActionRouter,
    runtime: NotificationRuntime,
) {
    val settings by preferences.presentation.collectAsState()
    // The star, written from the overlay and told to the tree.
    //
    // The write goes through the runtime, which holds the same gateway the library writes through, so
    // it is the same write rather than a second one that happens to touch the same column. The tree
    // is then told, because the library keeps its own copy of the flag and a tile that still shows
    // the old star would make the next press write back what the shelf believed.
    //
    // Neither half goes through [NotificationActionRouter.dispatch], because dispatching an action
    // brings the app forward — and starring a passage is not a request to be taken somewhere.
    val onToggleFavorite: (suspend (Long, Boolean) -> Boolean) =
        remember(runtime, actionRouter) {
            val toggle: suspend (Long, Boolean) -> Boolean = { id, favorite ->
                val written = runtime.setFavorite(id, favorite)
                if (written) {
                    actionRouter.dispatchFavoriteChanged(id, favorite)
                }
                written
            }
            toggle
        }
    NotificationOverlayWindow(
        presenter = overlay,
        settings = settings,
        // The app's appearance, not a reminder appearance: a card in someone else's typeface reads as
        // a notification from somewhere else, and a card in a size the reader has already rejected
        // everywhere else reads as worse.
        theme =
            OverlayTheme(
                darkTheme = rememberAzkarDarkTheme(appState.themePreference),
                locale = appState.locale,
                typography = appState.typography,
            ),
        // Through the router rather than straight to the navigator, because the overlay window can be
        // clicked before the main tree has ever composed — the autostart path is exactly that case.
        onOpen = { action -> actionRouter.dispatch(action) },
        onToggleFavorite = onToggleFavorite,
    )
}

/**
 * Six cards, one every few seconds, for [OVERLAY_PROOF_OF_CONSENT_FLAG].
 *
 * The timing is chosen so the three things under test are all reachable in one run and are visible
 * at the same moment: the stack cap (three arrive together and the fourth pushes the first out), the
 * hover pause (leave the pointer on one card for ten seconds and it is still there afterwards), and
 * the click routes (the first opens a prayer, the rest open a remembrance). Nothing here is
 * localized: it is a developer harness read off a terminal, not a screen a user meets.
 */
@Composable
private fun OverlayProofOfConcept(publisher: NotificationPublisher) {
    LaunchedEffect(publisher) {
        var index = 1
        while (true) {
            publisher.publish(
                NotificationNotice(
                    title = "Overlay card $index of $PROOF_OF_CONSENT_CARDS",
                    message =
                        "Hover to pause the timer, × to dismiss, tap the body to open Azkar. " +
                            "It should sit above whatever you have open and never steal the keyboard.",
                    action =
                        if (index == 1) {
                            NotificationAction.PrayerTimes
                        } else {
                            NotificationAction.Remembrance(index.toLong())
                        },
                ),
            )
            index += 1
            delay(PROOF_OF_CONSENT_INTERVAL_MILLIS.milliseconds)
        }
    }
}

private const val PROOF_OF_CONSENT_CARDS = 6
private const val PROOF_OF_CONSENT_INTERVAL_MILLIS = 3_000L

@Composable
private fun UpdateNotificationPreview(
    appState: AzkarAppState,
    actionRouter: NotificationActionRouter,
) {
    val toastController = rememberToastController(appState)
    LaunchedEffect(toastController, actionRouter) {
        toastController.show(
            title = getString(Res.string.updateAvailableTitle),
            message = getString(Res.string.updateAvailableMessage, "0.1.2-beta.999.1"),
            priority = NotificationPriority.HIGH,
            onClick = { actionRouter.dispatch(NotificationAction.Update) },
        )
    }
}

@Composable
private fun MainWindow(
    visible: Boolean,
    onCloseRequest: () -> Unit,
    appState: AzkarAppState,
    actionRouter: NotificationActionRouter,
) {
    val screen = screenSizeInDp(LocalDensity.current)
    val windowState =
        rememberWindowState(
            size =
                screen?.let { windowSizeFor(it.width, it.height) }
                    ?: DpSize(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT),
            position = WindowPosition(Alignment.Center),
        )
    Window(
        onCloseRequest = onCloseRequest,
        visible = visible,
        state = windowState,
        title = windowTitle(appState.locale),
        icon = rememberVectorPainter(AzkarIcons.Brand),
    ) {
        AzkarApp(state = appState, actionRouter = actionRouter)
    }
}

/**
 * The window title, in the language the app is currently in.
 *
 * Read in the composition rather than resolved once at launch, which is what the original
 * one-time blocking resource read did and why the title never changed language: the
 * JavaFX original set `stage.setTitle` once in `start()` and had no live switch to miss, but
 * [com.azkar.ui.shell.AzkarAppState.applyLocale] retranslates in place, so the title had to follow.
 *
 * The `key` is the same device `AzkarApp` uses for the tree, and for the same reason. Compose
 * Multiplatform resources resolve against `androidx.compose.ui.text.intl.Locale.current`, which is
 * snapshot-backed and is not notified by the `Locale.setDefault` that [com.azkar.i18n.activateLocale] performs, so
 * a plain read would hand back the language the process started in. Keying discards the cached
 * resource and re-reads it, and dropping the cache is the only part that is actually needed here —
 * the title is a leaf, so unlike the tree it has no subtree to throw away.
 */
@Composable
private fun windowTitle(locale: Locale): String = key(locale) { stringResource(Res.string.title) }

/**
 * The primary screen's size in the same dp space the window state is measured in, or `null` when
 * there is no screen to ask.
 *
 * `Toolkit` throws [java.awt.HeadlessException] on a machine with no display rather than answering,
 * and the primary screen is the right one to ask about: it is the screen the window is centred on,
 * and a window is never positioned across displays before the user moves it.
 */
private fun screenSizeInDp(density: Density): DpSize? =
    runCatching {
        val size = Toolkit.getDefaultToolkit().screenSize
        DpSize(
            width = (size.width / density.density).dp,
            height = (size.height / density.density).dp,
        )
    }.onFailure { failure -> logger.debug(failure) { "Unable to read the screen size" } }
        .getOrNull()
