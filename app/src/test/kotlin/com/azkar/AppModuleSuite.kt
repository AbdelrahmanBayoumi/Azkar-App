package com.azkar

import com.azkar.data.config.LibraryBootstrapper
import com.azkar.data.repo.exposed.ExposedCollectionRepository
import com.azkar.data.repo.exposed.ExposedRecitationRepository
import com.azkar.data.repo.exposed.ExposedRemembranceRepository
import com.azkar.data.repo.exposed.ExposedTagRepository
import com.azkar.data.storage.CoverStorage
import com.azkar.domain.repo.CollectionRepository
import com.azkar.domain.repo.LibrarySeedState
import com.azkar.domain.repo.RecitationRepository
import com.azkar.domain.repo.RemembranceRepository
import com.azkar.domain.repo.TagRepository
import com.azkar.domain.service.CollectionOrderService
import com.azkar.domain.service.CollectionService
import com.azkar.domain.service.RecitationService
import com.azkar.domain.service.RemembranceService
import com.azkar.domain.service.TagService
import com.azkar.domain.service.TransactionRunner
import com.azkar.i18n.LocaleStore
import com.azkar.settings.FakePreferencesDataStore
import com.azkar.settings.SettingsStore
import com.azkar.ui.home.DomainHomeGateway
import com.azkar.ui.home.HomeGateway
import com.azkar.ui.library.CollectionSortStore
import com.azkar.ui.library.data.CollectionHomeGateway
import com.azkar.ui.library.data.CollectionsGateway
import com.azkar.ui.library.data.DomainCollectionsGateway
import com.azkar.ui.library.data.DomainTagsGateway
import com.azkar.ui.library.data.ReminderSources
import com.azkar.ui.library.data.TagGateway
import com.azkar.ui.notifications.DesktopAudioPlayer
import com.azkar.ui.notifications.GeocodingClient
import com.azkar.ui.notifications.LocationDetector
import com.azkar.ui.notifications.NotificationActionRouter
import com.azkar.ui.notifications.NotificationPreferencesRepository
import com.azkar.ui.notifications.NotificationRuntime
import com.azkar.ui.notifications.PrayerLocationSource
import com.azkar.ui.notifications.StartupRegistration
import com.azkar.ui.notifications.presentation.NotificationPublisher
import com.azkar.ui.notifications.presentation.OverlayNotificationPresenter
import com.azkar.ui.shell.ThemePreferenceStore
import com.azkar.ui.shell.TypographyStore
import com.azkar.updates.UpdateManager
import de.infix.testBalloon.framework.core.testSuite
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.assertj.core.api.Assertions.assertThat
import org.koin.core.Koin
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import kotlin.reflect.KClass

/**
 * What [appModule] promises to the composables that resolve out of it.
 *
 * `DataModuleSuite` covers the other half of the graph. This covers the half only this module knows
 * how to build, and what is worth asserting here is shape rather than behaviour: that every
 * definition resolves under the name a screen asks for, that the `bind` really does yield one object
 * under two names, that the adapters cannot be reached past the seam they are registered under, and
 * that the whole notification stack resolves without a composition existing. Each of those is
 * invisible to a resolution test alone, and each would otherwise surface as a crash on the first
 * window rather than as a failing test.
 *
 * The definitions are resolved over stubbed storage because the module, not the adapters, is what is
 * under test. `:app` could not use a real database in any case: `com.azkar.data` declares Exposed
 * `implementation`, so its tables are on this module's runtime classpath and not on its compile
 * classpath, and the database itself is `DatabaseManager`'s, opened under `~/.azkar`. The stubbing
 * stops at the ports `dataModule` registers — that is the seam between the two halves — so below it
 * every definition is the production one and above it every definition is `appModule`'s.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val AppModuleSuite by testSuite("AppModule") {
    testFixture { StubbedGraph.start() } closeWith {
        close()
    } asParameterForAll {
        test("every definition resolves, asked for by the name the UI asks for") { graph ->
            val koin = graph.koin
            assertThat(
                listOf(
                    koin.get<SettingsStore>(),
                    koin.get<ReminderSources>(),
                    koin.get<CollectionsGateway>(),
                    koin.get<CollectionHomeGateway>(),
                    koin.get<HomeGateway>(),
                    koin.get<TagGateway>(),
                    koin.get<ThemePreferenceStore>(),
                    koin.get<CollectionSortStore>(),
                    koin.get<LocaleStore>(),
                    koin.get<HttpClient>(),
                    koin.get<LocationDetector>(),
                    koin.get<PrayerLocationSource>(),
                    koin.get<GeocodingClient>(),
                    koin.get<NotificationPreferencesRepository>(),
                    koin.get<NotificationActionRouter>(),
                    koin.get<StartupRegistration>(),
                    koin.get<NotificationPublisher>(),
                    koin.get<NotificationRuntime>(),
                    koin.get<OverlayNotificationPresenter>(),
                    koin.get<CoroutineDispatcher>(
                        org.koin.core.qualifier
                            .named("notificationDispatcher"),
                    ),
                    koin.get<CoroutineScope>(
                        org.koin.core.qualifier
                            .named("notificationScope"),
                    ),
                    koin.get<UpdateManager>(),
                ),
            ).doesNotContainNull()
        }

        test("a single really is a single") { graph ->
            val koin = graph.koin
            assertThat(koin.get<SettingsStore>()).isSameAs(koin.get<SettingsStore>())
            assertThat(koin.get<HomeGateway>()).isSameAs(koin.get<HomeGateway>())
            assertThat(koin.get<ReminderSources>()).isSameAs(koin.get<ReminderSources>())
            assertThat(koin.get<CollectionsGateway>()).isSameAs(koin.get<CollectionsGateway>())
            assertThat(koin.get<TagGateway>()).isSameAs(koin.get<TagGateway>())
            assertThat(koin.get<HttpClient>()).isSameAs(koin.get<HttpClient>())
            assertThat(koin.get<NotificationPreferencesRepository>())
                .isSameAs(koin.get<NotificationPreferencesRepository>())
        }

        test("the library shelf and the home rail share one collections gateway") { graph ->
            val koin = graph.koin
            // The rail asks for `CollectionHomeGateway` and the shelf asks for `CollectionsGateway`,
            // and they have to be the same object: the rail's cards come from the very read model the
            // shelf mutates, so two instances would be two copies of one rule, free to drift apart.
            assertThat(koin.get<CollectionHomeGateway>()).isSameAs(koin.get<CollectionsGateway>())
        }

        test("the reminder sources are a single in their own right") { graph ->
            val koin = graph.koin
            // The queue resolves one source instance, separate from the library gateway.
            val reminderSources: Any = koin.get<ReminderSources>()
            assertThat(reminderSources).isSameAs(koin.get<ReminderSources>())
            assertThat(reminderSources).isNotSameAs(koin.get<CollectionsGateway>())
        }

        test("the prayer location source is the location detector") { graph ->
            val koin = graph.koin
            // The scheduler asks for the seam rather than for the detector, because the detector
            // talks to the network and a scheduler that has to be handed one is a scheduler that
            // cannot be tested. The seam resolves to the one detector, so the app still detects the
            // reader's location and a test can still stand in for it.
            assertThat(koin.get<PrayerLocationSource>()).isSameAs(koin.get<LocationDetector>())
        }

        test("the adapters cannot be reached past the seam they are registered under") { graph ->
            val koin = graph.koin
            // A guard rather than a fix: nothing asks for an adapter by name, and `koinInject` only
            // ever names the seam, so registering one under its own class would add a way in for
            // whoever comes next without changing a single call site.
            assertThat(koin.getOrNull<DomainCollectionsGateway>()).isNull()
            assertThat(koin.getOrNull<DomainHomeGateway>()).isNull()
            assertThat(koin.getOrNull<DomainTagsGateway>()).isNull()
        }

        test("the module registers its app services and UI seams") {
            assertThat(registeredTypes())
                .containsExactlyInAnyOrder(
                    SettingsStore::class,
                    ThemePreferenceStore::class,
                    CollectionSortStore::class,
                    LocaleStore::class,
                    TypographyStore::class,
                    LibrarySeedState::class,
                    ReminderSources::class,
                    CollectionsGateway::class,
                    HomeGateway::class,
                    TagGateway::class,
                    HttpClient::class,
                    LocationDetector::class,
                    GeocodingClient::class,
                    NotificationPreferencesRepository::class,
                    DesktopAudioPlayer::class,
                    NotificationActionRouter::class,
                    StartupRegistration::class,
                    NotificationPublisher::class,
                    NotificationRuntime::class,
                    OverlayNotificationPresenter::class,
                    CoroutineDispatcher::class,
                    CoroutineScope::class,
                    UpdateManager::class,
                )
        }

        test("the notification stack is a single, and it is in the container") { graph ->
            val koin = graph.koin
            // The overlay window is a composition, but everything that decides *whether* to show
            // something is not: the scheduler, the dispatcher and the presenter have to exist before
            // any window does, because the state the tray leaves the app in is "no window at all" and
            // a reminder still has to arrive there. So the container owns them, and this is the
            // assertion that they are built once — a second overlay presenter would be a second
            // stack, and a second runtime would be a second scheduler.
            assertThat(koin.get<NotificationPublisher>())
                .isSameAs(koin.get<NotificationPublisher>())
            assertThat(koin.get<OverlayNotificationPresenter>())
                .isSameAs(koin.get<OverlayNotificationPresenter>())
            assertThat(koin.get<NotificationRuntime>()).isSameAs(koin.get<NotificationRuntime>())
        }
    }
}

/**
 * Every type [appModule] registers a definition under, once each.
 *
 * `Module.mappings` holds one entry per index key rather than one per definition, so the bound
 * `CollectionHomeGateway` key points at the `CollectionsGateway` factory as well: seven entries for
 * six definitions. That duplication is the shape the previous test relies on, so it is collapsed
 * here rather than asserted twice.
 *
 * Being the module's own index rather than the container's resolved list is also what pins the OS
 * notification channel's absence: it is gone because nothing registers it, not because nothing
 * asked for it.
 *
 * The module's own index is Koin's internal API, so the opt-in lives here rather than on the whole
 * file: two assertions need it, and every other assertion in the suite asks the container the way
 * the UI does, which is the way worth teaching.
 */
@OptIn(KoinInternalApi::class)
private fun registeredTypes(): List<KClass<*>> =
    appModule.mappings.values
        .map { it.beanDefinition.primaryType }
        .distinct()

/**
 * The container, plus the one piece of scratch state the overrides need released afterwards.
 *
 * A class rather than a bare `Koin` because the settings store's scope outlives the fixture block
 * that creates it: `testFixture` and `closeWith` are sibling lambdas, so a local declared in the
 * first is not in scope in the second.
 */
private class StubbedGraph private constructor(
    val koin: Koin,
    private val settingsScope: CoroutineScope,
) {
    fun close() {
        // The notification scope outlives the fixture block for the same reason the settings scope
        // does, and it holds the presenter's expiry jobs, so it is cancelled rather than leaked.
        runCatching {
            koin
                .get<CoroutineScope>(
                    org.koin.core.qualifier
                        .named("notificationScope"),
                ).cancel()
        }
        runCatching {
            koin
                .get<CoroutineScope>(
                    org.koin.core.qualifier
                        .named("updateScope"),
                ).cancel()
        }
        runCatching { koin.get<HttpClient>().close() }
        stopKoin()
        settingsScope.cancel()
    }

    companion object {
        /** Starts the real [appModule] over stubbed storage and settings of its own. */
        fun start(): StubbedGraph {
            val settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            startKoin {
                modules(
                    storagePortModule,
                    appModule,
                    // The production definition builds over `~/.azkar/settings.preferences_pb`.
                    // Pointed at a store of its own instead, so a graph test never writes settings
                    // the developer would then open the real app against — and never writes them
                    // to a disk at all.
                    module { single { SettingsStore(FakePreferencesDataStore(), settingsScope) } },
                )
            }
            return StubbedGraph(KoinPlatform.getKoin(), settingsScope)
        }
    }
}

/**
 * `dataModule` with the database taken out from under it.
 *
 * Every adapter's constructor takes nothing but the [TransactionRunner] port — no `Database`, no
 * Exposed type in any signature — and that is what makes this reachable from a module which cannot
 * see Exposed at all. [PassThroughTransactionRunner] runs the block and does nothing else, so no
 * schema and no connection are needed; the real runner's behaviour, and the wiring around it, are
 * what `DataModuleSuite` and the repository suites are for.
 *
 * Mirrors `dataModule` definition for definition, including the tag repository's `bind` and the
 * single shared transaction runner, so a definition added to one and not the other fails here.
 */
private val storagePortModule =
    module {
        single<TransactionRunner> { PassThroughTransactionRunner }

        single<CollectionRepository> { ExposedCollectionRepository(get(), get()) }
        single<RecitationRepository> { ExposedRecitationRepository(get()) }
        single { ExposedTagRepository(get()) } bind TagRepository::class
        single<RemembranceRepository> { ExposedRemembranceRepository(get(), get()) }

        single { TagService(get(), get()) }
        single { RemembranceService(get(), get()) }
        single { CollectionService(get(), get()) }
        single { RecitationService(get(), get(), get()) }
        single { CollectionOrderService(get(), get()) }

        // Neither of these touches the filesystem or a connection at construction, so they are
        // mirrored verbatim. What the bootstrapper *reads* — the gate — is a `LibrarySeedState`,
        // which is `appModule`'s to supply rather than this one's, and that asymmetry is the point.
        single { CoverStorage() }
        single { LibraryBootstrapper(get(), get(), get(), get(), get()) }
    }

/**
 * A [TransactionRunner] that does the one thing a resolution test needs, and nothing else.
 *
 * Deliberately not a spy, and deliberately never failing: the claim under test here is that a
 * definition resolves, and an adapter's transaction handling is claimed elsewhere, against a real
 * runner over a real schema.
 */
private object PassThroughTransactionRunner : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}
