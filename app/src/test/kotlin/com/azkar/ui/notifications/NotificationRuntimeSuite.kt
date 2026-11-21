package com.azkar.ui.notifications

import com.azkar.settings.withInMemorySettings
import com.azkar.ui.home.PrayerKey
import com.azkar.ui.library.FakeCollectionsGateway
import com.azkar.ui.library.customCollection
import com.azkar.ui.library.remembrance
import com.azkar.ui.notifications.presentation.NotificationPublisher
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.assertj.core.api.Assertions.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds

/**
 * That the scheduler announces what came due while it was running, and stays quiet about the rest.
 *
 * The bug these tests exist for was reported as: *a notification pops up exactly when the app is
 * opened for the first time after installing it.* The app draws one kind of notification — the
 * overlay card — and the scheduler produces it from exactly two paths: the shared reminder queue and
 * the prayer bells. Both of them were catch-up paths, and the scheduler's first poll runs the moment
 * the app opens, so anything that had come due while the app was closed was announced at the door.
 * On a fresh install there was nothing yet to have come due, which made the timing look like a
 * coincidence of the install rather than like a rule — and it was a rule, and the rule was wrong.
 *
 * The rule now is one sentence: **a reminder is announced only if it became due after the scheduler
 * started.** What came due while the app was closed is missed rather than replayed, which is the same
 * answer the schedule already gives a slot it slept through.
 *
 * The clock is injected, so "what does the scheduler do at this moment?" is a question the test can
 * ask rather than a question the test has to wait for.
 *
 * Nothing here is anchored to the day it was written on, and nothing here is anchored to the zone
 * the reader's computer happens to be in: the prayers are calculated for a fixed day in UTC, and the
 * queue's slots are wall-clock hours on a winter's day, so the fixture answers the same question in
 * Ramallah as it does in a build agent in Frankfurt. The two schedules are kept apart as well — the
 * queue tests silence the bells, and the prayer tests leave the queue off — so that a prayer landing
 * on the same minute cannot turn a statement about the scheduler into a statement about the calendar.
 */
val NotificationRuntimeSuite by testSuite("The notification scheduler") {
    test("a queue slot that came due before the scheduler started is missed, and the next one is not") {
        withInMemorySettings { _, store, _ ->
            val preferences = NotificationPreferencesRepository(store)
            preferences.savePrayerLocation(TEST_LOCATION)
            preferences.updateQueue { TEST_QUEUE_CONFIG }
            preferences.silencePrayerBells()
            preferences.refresh()

            val world = SchedulerWorld()
            // The app opens two minutes after the slot, so the slot is the one the schedule would
            // have answered and the scheduler was not there for it.
            world.clock.set(localInstantAt(QUEUE_OPENED_AFTER_SLOT))

            withScheduler(world, preferences) {
                settleOnRealClock()
                assertThat(world.publisher.notices).isEmpty()

                // The next slot comes due while the scheduler is running, and is announced.
                world.clock.set(localInstantAt(QUEUE_NEXT_SLOT))
                val notice = world.publisher.awaitNotice()
                assertThat(notice).isNotNull()
                assertThat(notice?.content).isInstanceOf(NotificationContent.Remembrance::class.java)
                assertThat(world.publisher.notices).hasSize(1)
            }
        }
    }

    test("a prayer that came due before the scheduler started is not announced") {
        withInMemorySettings { _, store, _ ->
            val preferences = NotificationPreferencesRepository(store)
            preferences.savePrayerLocation(TEST_LOCATION)
            preferences.refresh()

            val world = SchedulerWorld()
            // A minute after maghrib, so the prayer is still inside its grace window — and was
            // already due when the app opened.
            world.clock.set(prayerInstantAt(prayerTime(PrayerKey.MAGHRIB)).plusSeconds(60))

            withScheduler(world, preferences) {
                settleOnRealClock()
                assertThat(world.publisher.notices).isEmpty()

                // And the scheduler is awake: the next prayer is announced when it comes due, which
                // is what makes the silence above a statement about the rule rather than about a
                // scheduler that had stopped.
                world.clock.set(prayerInstantAt(prayerTime(PrayerKey.ISHA)).plusSeconds(30))
                val notice = world.publisher.awaitNotice()
                assertThat(notice).isNotNull()
                assertThat(notice?.content).isInstanceOf(NotificationContent.Prayer::class.java)
            }
        }
    }

    test("a prayer that comes due while the scheduler is running is announced") {
        withInMemorySettings { _, store, _ ->
            val preferences = NotificationPreferencesRepository(store)
            preferences.savePrayerLocation(TEST_LOCATION)
            preferences.refresh()

            val world = SchedulerWorld()
            world.clock.set(prayerInstantAt(prayerTime(PrayerKey.DHUHR)).minusSeconds(60))

            withScheduler(world, preferences) {
                settleOnRealClock()
                assertThat(world.publisher.notices).isEmpty()

                world.clock.set(prayerInstantAt(prayerTime(PrayerKey.DHUHR)).plusSeconds(30))
                val notice = world.publisher.awaitNotice()
                assertThat(notice).isNotNull()
                assertThat(notice?.content).isInstanceOf(NotificationContent.Prayer::class.java)
                assertThat(notice?.action).isEqualTo(NotificationAction.PrayerTimes)
            }
        }
    }

    test("the app opens with nothing on screen, even when a prayer has just been") {
        withInMemorySettings { _, store, _ ->
            val preferences = NotificationPreferencesRepository(store)
            preferences.savePrayerLocation(TEST_LOCATION)
            preferences.refresh()

            val world = SchedulerWorld()
            // The fresh install as it is: the queue is off because it has never been switched on,
            // and the prayer bells are on because that is how the app ships. The app opens half a
            // minute after dhuhr, so the prayer is still inside its grace window — and nothing is
            // announced.
            world.clock.set(prayerInstantAt(prayerTime(PrayerKey.DHUHR)).plusSeconds(30))

            withScheduler(world, preferences) {
                settleOnRealClock()
                assertThat(world.publisher.notices).isEmpty()

                // The next prayer is announced when it comes: the silence at the door is the rule
                // working, not a scheduler that has gone quiet.
                world.clock.set(prayerInstantAt(prayerTime(PrayerKey.ASR)).plusSeconds(30))
                val notice = world.publisher.awaitNotice()
                assertThat(notice).isNotNull()
                assertThat(notice?.content).isInstanceOf(NotificationContent.Prayer::class.java)
            }
        }
    }
}

/**
 * Runs [block] with the scheduler started on a scope of its own, and stops it afterwards.
 *
 * The scope is separate from the one the settings live on because the scheduler polls on a real
 * clock: a test that shared the scope would be racing its own fixture. It is cancelled in a
 * `finally`, because a scheduler left running would keep publishing into the next test's publisher.
 */
private suspend fun withScheduler(
    world: SchedulerWorld,
    preferences: NotificationPreferencesRepository,
    block: suspend CoroutineScope.() -> Unit,
) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    try {
        world.runtime(preferences).start(scope)
        block(scope)
    } finally {
        scope.cancel()
    }
}

/**
 * The world the scheduler runs in: a clock the test owns, a shelf with one passage in it, and a
 * publisher that keeps what it was handed.
 */
private class SchedulerWorld {
    val clock = AtomicReference(localInstantAt(QUEUE_SLOT))

    val publisher = RecordingPublisher()

    val gateway =
        FakeCollectionsGateway(
            collections =
                listOf(
                    customCollection(
                        id = SOURCE_COLLECTION_ID,
                        name = "Morning",
                        members = listOf(remembrance(id = PASSAGE_ID)),
                    ),
                ),
            remembrances = listOf(remembrance(id = PASSAGE_ID)),
        )

    fun runtime(preferences: NotificationPreferencesRepository): NotificationRuntime =
        NotificationRuntime(
            preferences = preferences,
            collections = gateway,
            locationDetector = FixedLocationSource(TEST_LOCATION),
            publisher = publisher,
            clock = { clock.get() },
            tickIntervalMillis = TEST_TICK_MILLIS,
        )
}

/** A publisher that keeps what it was given, so a test can look at what the scheduler announced. */
private class RecordingPublisher : NotificationPublisher {
    private val published = CopyOnWriteArrayList<NotificationNotice>()

    /** What has been announced, oldest first. Safe to read from the test's own thread. */
    val notices: List<NotificationNotice> get() = published.toList()

    override val latestAction: NotificationAction
        get() = notices.lastOrNull()?.action ?: NotificationAction.OpenApp

    override fun publish(notice: NotificationNotice) {
        published += notice
    }

    override suspend fun dispose() = Unit
}

/** A location source that answers with the fixture's location, so the runtime never asks the network. */
private class FixedLocationSource(
    private val location: PrayerLocationSettings,
) : PrayerLocationSource {
    override suspend fun detect(): PrayerLocationSettings = location
}

/**
 * Waits for a notice the scheduler published, and answers null if it never came.
 *
 * Polling rather than a signal, because the scheduler publishes on its own thread and the test has
 * no reason to know which one it is: the question the test asks is "what did the scheduler announce
 * within five seconds", and the answer is the same whether it arrived first time or the fortieth.
 *
 * On a real clock, deliberately: the suite runs inside `runTest`, whose clock is virtual, and a
 * timeout there does not wait — it advances the scheduler. The scheduler under test runs on a real
 * dispatcher and a real clock, so waiting on the virtual one would be waiting for something that has
 * not happened yet.
 */
private suspend fun RecordingPublisher.awaitNotice(
    predicate: (NotificationNotice) -> Boolean = { true },
    timeoutMillis: Long = AWAIT_TIMEOUT_MILLIS,
): NotificationNotice? =
    withContext(Dispatchers.IO) {
        withTimeoutOrNull(timeoutMillis.milliseconds) {
            while (notices.none(predicate)) delay(POLL_MILLIS.milliseconds)
        }
        notices.firstOrNull(predicate)
    }

/**
 * Waits for a while, on a real clock.
 *
 * The counterpart to the wait above: the scheduler polls on a real dispatcher, so a test that wants
 * to know that *nothing* was announced has to wait for a real while before it can say so. Waiting on
 * the suite's virtual clock would return at once and prove nothing.
 *
 * Every test that waits for silence follows it with a wait for a notice, so the silence is never
 * the only thing the test knows.
 */
private suspend fun settleOnRealClock() {
    withContext(Dispatchers.IO) { delay(SETTLE_MILLIS.milliseconds) }
}

/**
 * Turns every prayer bell off.
 *
 * The bells ship on, and a test that is about the queue should not also hear about a prayer that
 * happens to fall on the same minute.
 */
private suspend fun NotificationPreferencesRepository.silencePrayerBells() {
    PrayerKey.entries.forEach { key -> setPrayerEnabled(key, false) }
}

/**
 * The day the fixture's prayers are calculated for.
 *
 * A fixed day, in a fixed zone, so the fixture's numbers are the same on every machine. The clock
 * the scheduler reads is injected, but the *zone* it reads that clock in is the one the location
 * names — and a fixture that leaned on the reader's own zone would answer differently in Ramallah
 * than it does in a build agent in Frankfurt.
 */
private val PRAYER_DATE: LocalDate = LocalDate.of(2026, 1, 15)

/**
 * The zone the fixture's prayers live in.
 *
 * UTC, because it has no daylight saving and no surprises: the same date always means the same
 * offset, and therefore the same prayer times.
 */
private val PRAYER_ZONE: ZoneId = ZoneId.of("UTC")

/** The location the fixture's prayers are calculated for: the city the app ships with, in UTC. */
private val TEST_LOCATION: PrayerLocationSettings =
    PrayerLocationSettings(
        latitude = 31.9,
        longitude = 35.2,
        city = "Test City",
        zoneId = PRAYER_ZONE.id,
        autoDetect = false,
    )

/** The fixture's prayer times, as the app's own calculator answers them. */
private val TEST_PRAYER_TIMES: Map<PrayerKey, LocalTime> =
    PrayerTimeCalculator.calculate(PRAYER_DATE, TEST_LOCATION)

/** The time of one of the fixture's prayers. */
private fun prayerTime(key: PrayerKey): LocalTime = TEST_PRAYER_TIMES.getValue(key)

/** The instant of a prayer time on the fixture's day, in the fixture's own zone. */
private fun prayerInstantAt(time: LocalTime): Instant = ZonedDateTime.of(PRAYER_DATE, time, PRAYER_ZONE).toInstant()

/**
 * The day the fixture's queue slots fall on, in the reader's own wall clock.
 *
 * A winter day, because the reader's zone may have daylight saving and the hours around noon are
 * the ones no clock changes.
 */
private val QUEUE_DATE: LocalDate = LocalDate.of(2026, 1, 15)

/** The queue slot the fixture's reminder comes from: noon. */
private val QUEUE_SLOT: LocalTime = LocalTime.of(12, 0)

/** The moment the app "opens" in the first test: two minutes after the slot it missed. */
private val QUEUE_OPENED_AFTER_SLOT: LocalTime = LocalTime.of(12, 2)

/** The next slot along, which comes due while the scheduler is running. */
private val QUEUE_NEXT_SLOT: LocalTime = LocalTime.of(13, 0)

/** The instant of a wall-clock time on the fixture's day, in the reader's own zone. */
private fun localInstantAt(time: LocalTime): Instant =
    ZonedDateTime.of(QUEUE_DATE, time, ZoneId.systemDefault()).toInstant()

/**
 * The queue the fixture's reminder comes from: enabled, one collection, one passage.
 *
 * The window and the interval are written out rather than left to [ReminderQueueConfig]'s defaults,
 * so that the fixture's slots are the slots this test means and not the ones a future default
 * would produce.
 */
private val TEST_QUEUE_CONFIG: ReminderQueueConfig =
    ReminderQueueConfig(
        enabled = true,
        collectionIds = listOf(SOURCE_COLLECTION_ID),
        windowStartMinute = 11 * 60,
        windowEndMinute = 14 * 60,
        intervalMinutes = 60,
    )

private const val SOURCE_COLLECTION_ID = 7L
private const val PASSAGE_ID = 3L

/** How often the fixture's scheduler polls. The production value is thirty seconds. */
private const val TEST_TICK_MILLIS = 20L

/** How long the fixture waits for an announcement before giving up on it. */
private const val AWAIT_TIMEOUT_MILLIS = 5_000L

/** How long the fixture waits before concluding that nothing is going to be announced. */
private const val SETTLE_MILLIS = 300L

/** How often the fixture looks at what has been announced while it waits. */
private const val POLL_MILLIS = 10L
