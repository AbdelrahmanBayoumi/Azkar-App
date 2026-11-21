package com.azkar.data.config

import com.azkar.data.repo.exposed.ExposedTagRepository
import com.azkar.data.seed.CollectionSeed
import com.azkar.data.seed.SeedDataLoader
import com.azkar.data.transaction.ExposedTransactionRunner
import com.azkar.domain.repo.CollectionRepository
import com.azkar.domain.repo.RecitationRepository
import com.azkar.domain.repo.RemembranceRepository
import com.azkar.domain.repo.TagRepository
import com.azkar.domain.service.CollectionOrderService
import com.azkar.domain.service.CollectionService
import com.azkar.domain.service.RecitationService
import com.azkar.domain.service.RemembranceService
import com.azkar.domain.service.TagService
import com.azkar.domain.service.TransactionRunner
import com.azkar.testUtils.TestDatabase
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

/**
 * What [dataModule] promises to the rest of the app.
 *
 * The module replaced a `DomainServiceContext` that every gateway constructed for itself, twenty-five
 * times over, so its whole value is that the graph resolves: each definition names its own
 * dependencies, the ports the UI consumes are indexed, and a single really is a single. Nothing else
 * in the suite exercises that — the repository suites hand-build the same adapters — so a definition
 * asking for the wrong thing, or a `bind` left off, would only fail once the desktop app was launched.
 *
 * The database is overridden rather than taken from `DatabaseManager`, and that is the whole reason
 * the override exists: the production definition opens the real file under `~/.azkar`, and a graph
 * test has no business writing there. Everything above the [Database] — the transaction runner, the
 * repositories, the services — is the production wiring running against the same DDL and the same
 * seed data the app runs against, which is what gives the behavioural tests below their meaning.
 *
 * The dataset is loaded here rather than left to the graph because the piece that loads it in the
 * app — `LibraryBootstrapper` — is deliberately not fully wired in this module: its gate is a port
 * that `:app` implements, and a graph test has nothing to say about how that gate is stored. What is
 * under test below is the wiring, so the suite asks for the rows the same way the bootstrapper does.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val DataModuleSuite by testSuite("DataModule") {
    testFixture {
        val database = TestDatabase.bootstrapWithH2()
        transaction(database) {
            val seeded = SeedDataLoader.load()
            CollectionSeed.load(seeded.remembranceIds)
        }
        startKoin {
            modules(
                dataModule,
                module { single<Database> { database } },
            )
        }
        KoinPlatform.getKoin()
    } closeWith {
        stopKoin()
    } asParameterForAll {
        test("every definition in the module resolves") { koin ->
            assertThat(koin.resolved()).doesNotContainNull()
        }

        test("asking twice returns the same instance") { koin ->
            assertThat(koin.get<TransactionRunner>()).isSameAs(koin.get<TransactionRunner>())
            assertThat(koin.get<CollectionRepository>()).isSameAs(koin.get<CollectionRepository>())
            assertThat(koin.get<RecitationRepository>()).isSameAs(koin.get<RecitationRepository>())
            assertThat(koin.get<RemembranceRepository>()).isSameAs(koin.get<RemembranceRepository>())
            assertThat(koin.get<TagRepository>()).isSameAs(koin.get<TagRepository>())
            assertThat(koin.get<RemembranceService>()).isSameAs(koin.get<RemembranceService>())
            assertThat(koin.get<CollectionService>()).isSameAs(koin.get<CollectionService>())
            assertThat(koin.get<CollectionOrderService>()).isSameAs(koin.get<CollectionOrderService>())
            assertThat(koin.get<RecitationService>()).isSameAs(koin.get<RecitationService>())
        }

        test("a bound type is an index entry, not a second construction") { koin ->
            // The transaction runner is the invariant that used to be a comment in
            // `DomainServiceContext`: one runner, so a repository call nested inside a service
            // transaction joins that transaction rather than opening a competing one. Two runners
            // would be two independent decisions about where the boundary sits.
            assertThat(koin.get<ExposedTransactionRunner>()).isSameAs(koin.get<TransactionRunner>())

            // The tag repository is indexed under both names because `ExposedRemembranceRepository`
            // hands the concrete class to the writer that persists tags alongside a remembrance, while
            // the service layer only ever knows the port.
            assertThat(koin.get<ExposedTagRepository>()).isSameAs(koin.get<TagRepository>())
        }

        test("the graph reaches the overridden database, all the way down") { koin ->
            val remembrances = koin.get<RemembranceService>()
            val target = requireNotNull(remembrances.findFirst())
            val id = requireNotNull(target.id)

            remembrances.markFavorite(id)

            // The round trip is the assertion: a write and a read through one service, across that
            // service's own transaction boundary, over the injected database. If any definition were
            // asking for the wrong dependency, the write would land somewhere this read cannot see.
            assertThat(remembrances.findFavorites().map { it.id }).contains(id)
        }

        test("the seeded schema is visible through the service layer") { koin ->
            // A resolution test alone would pass against an empty database, so this is what separates
            // "the graph is wired" from "the graph is wired to the right database": the shipped
            // collections only exist because the fixture loaded them.
            assertThat(koin.get<CollectionService>().findSummaries()).isNotEmpty()
        }
    }
}

/** Every type the module registers, asked for by the name the module registers it under. */
private fun Koin.resolved(): List<Any> =
    listOf(
        get<Database>(),
        get<TransactionRunner>(),
        get<CollectionRepository>(),
        get<RecitationRepository>(),
        get<RemembranceRepository>(),
        get<TagRepository>(),
        get<TagService>(),
        get<RemembranceService>(),
        get<CollectionService>(),
        get<RecitationService>(),
    )
