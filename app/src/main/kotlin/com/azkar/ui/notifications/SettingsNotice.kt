package com.azkar.ui.notifications

import androidx.compose.runtime.Immutable

/**
 * The two failures a settings editor can report, as separate sealed hierarchies rather than one.
 *
 * The screen these replace held a single nullable `String` and reused it for two unrelated jobs: a
 * rejected coordinate, and a failure to register Azkar with the desktop login manager. The startup
 * message therefore rendered at the bottom of the *prayer location* card, several sections away from
 * the switch that caused it, and cleared itself the next time a location was saved. The bug was not
 * the shared string so much as the shared slot — so each family now has its own type, and
 * [LocationNotice] cannot be passed to whatever renders a startup failure.
 *
 * Two types rather than one sealed interface with a `when` at the display site, because the display
 * sites are on different tabs: a location failure has nothing to say on the notifications tab, and a
 * startup failure has nothing to say on the prayer tab. Giving each a type means neither renderer
 * needs a fall-through branch for cases that cannot reach it.
 *
 * Cases carrying a `reason` keep the underlying exception's message for the times it is worth
 * showing; the fallback for each is a string resource resolved at the point of display, so this file
 * stays free of Compose resources.
 */
sealed interface LocationNotice {
    /** Coordinates did not parse, or a field was left empty. */
    data object InvalidCoordinates : LocationNotice

    /** The time zone field named something `ZoneId` does not know. */
    data object InvalidTimeZone : LocationNotice

    /** The location settings were written. */
    data object Saved : LocationNotice

    /** IP geolocation answered, naming the place it found. */
    data class Detected(
        val city: String,
    ) : LocationNotice

    /** IP geolocation could not answer — offline, the lookup refused, or the host has no client. */
    data class Unavailable(
        val reason: String?,
    ) : LocationNotice

    /** The place search could not answer — offline, the lookup refused, or the host has no client. */
    data class SearchFailed(
        val reason: String?,
    ) : LocationNotice

    /** The place search answered, and nothing matched what was typed. */
    data object NoResults : LocationNotice

    /** A suggestion was picked and written, naming the place it filled in. */
    data class PlaceSelected(
        val city: String,
    ) : LocationNotice
}

/** Why the desktop login manager refused. Its own type so it cannot be shown as a location problem. */
sealed interface StartupNotice {
    data object DisabledByUser : StartupNotice

    data object ManagedByPolicy : StartupNotice

    data object DevelopmentBuild : StartupNotice

    data object Unsupported : StartupNotice

    /** The platform call failed, or the host has no login manager to call. */
    data class Unavailable(
        val reason: String?,
    ) : StartupNotice
}

/**
 * The four location fields as the user is editing them, held as text.
 *
 * Separate from [PrayerLocationSettings] because a half-typed `"3"` is not a latitude and cannot be
 * pushed through a `Double` constructor: keeping the draft as strings is what lets the editor exist
 * at all. Re-seeding from storage after a write is what stops the fields drifting into showing
 * something that was never stored — without it a failed zone id stays on screen next to the error
 * that rejected it, and a successful save leaves the old value in the box.
 */
@Immutable
data class PrayerLocationDraft(
    val city: String,
    val latitude: String,
    val longitude: String,
    val zoneId: String,
) {
    /** Returns a copy with one field replaced, so the editor can pass an updater per field. */
    fun with(
        city: String = this.city,
        latitude: String = this.latitude,
        longitude: String = this.longitude,
        zoneId: String = this.zoneId,
    ): PrayerLocationDraft =
        PrayerLocationDraft(city = city, latitude = latitude, longitude = longitude, zoneId = zoneId)

    companion object {
        /** Seeds the draft from stored settings; the empty draft is never worth showing. */
        fun of(location: PrayerLocationSettings): PrayerLocationDraft =
            PrayerLocationDraft(
                city = location.city,
                latitude = location.latitude.toString(),
                longitude = location.longitude.toString(),
                zoneId = location.zoneId,
            )
    }
}
