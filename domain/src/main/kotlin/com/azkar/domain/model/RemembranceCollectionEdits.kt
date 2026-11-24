package com.azkar.domain.model

import com.azkar.domain.model.impl.RemembranceCollectionImpl
import java.util.Locale

/*
 * Rebuilding helpers for [RemembranceCollection].
 *
 * Every one of these round-trips through [RemembranceCollectionImpl] so that trimming and blank
 * normalization run on the way out as well as on the way in, and they are written against the
 * interface rather than the implementation so that callers holding a loaded aggregate do not have
 * to cast to copy it.
 *
 * `updatedAt` is deliberately left untouched. The storage layer stamps it on write, because only
 * the storage layer knows what "now" was at the moment the row actually changed.
 *
 * Everything else is copied through, field by field, and the enumeration is deliberate: this helper
 * grew a field once and forgot it, and renaming a collection silently took it off the Home rail
 * because the timestamp went back to its `null` default on the way through. A list of what is *not*
 * rebuilt does not catch that; the only thing that does is a helper that refuses to have fields of
 * its own, so a new one cannot be left behind.
 */

/**
 * A copy of this collection under a new [name] and [description].
 *
 * [translations] defaults to the receiver's and has to: a rename is how a shelf is *given* a name in
 * another language, so the editor cannot express "rename this" without also being able to say "and
 * while you are there, call it that in Arabic". Passing an empty map is the one way to say "drop every
 * translation", and it is spelled the way it is spelled rather than inferred from a null, because a
 * caller that forgets the argument should keep what is there rather than silently delete it.
 */
fun RemembranceCollection.renamed(
    name: String,
    description: String? = this.description,
    translations: Map<Locale, LocalizedPair> = this.translations,
    nameLocale: Locale = this.nameLocale,
): RemembranceCollection =
    rebuilt(name = name, description = description, translations = translations, nameLocale = nameLocale)

/**
 * A copy of this collection holding [remembrance] as a member.
 *
 * Appended rather than inserted anywhere, because a collection's membership is an ordered sequence
 * and the only thing that has an opinion about where a member belongs is [position] — a value this
 * aggregate does not carry, because ordering is a storage concern and reading is what flattens it.
 * A caller that means to place a member has to say so through the membership repository instead of
 * quietly getting an appended one.
 *
 * Returns the receiver unchanged when [remembrance] is already a member. Membership is stored under
 * a composite key, so a duplicate cannot survive to be written, and refusing it here keeps the local
 * mirror honest rather than accumulating a copy that the next read would silently drop.
 */
fun RemembranceCollection.withMember(remembrance: Remembrance): RemembranceCollection =
    if (members.any { it.id == remembrance.id }) {
        this
    } else {
        rebuilt(members = members + remembrance)
    }

/** A copy of this collection no longer holding the member with [remembranceId]. */
fun RemembranceCollection.withoutMember(remembranceId: Long): RemembranceCollection =
    if (members.none { it.id == remembranceId }) {
        this
    } else {
        rebuilt(members = members.filterNot { it.id == remembranceId })
    }

/**
 * A copy of this collection holding exactly [remembers], in the order given.
 *
 * The one deliberately unopinionated member of this file, and it exists because [Remembrance] carries
 * flags the collection does not own — a star being the one that bites. Editing such a flag means
 * rewriting one member, and a caller that reached for this would otherwise have to rebuild the whole
 * aggregate field by field outside this file, which is precisely the copy-with-fields-of-its-own that
 * [rebuilt]'s own KDoc refuses to let exist.
 *
 * Ordering is the caller's to choose because membership is an ordered sequence and this is the only
 * one of these helpers that can be handed one to lose.
 */
fun RemembranceCollection.withMembers(remembers: List<Remembrance>): RemembranceCollection =
    rebuilt(members = remembers)

/**
 * Rebuilds the aggregate from itself with whichever fields the caller changed.
 *
 * Named for what it is rather than `copy` because the implementation's generated `copy` skips the
 * factory that trims and validates — and going through the factory is the whole point of routing
 * every rebuild through one place.
 *
 * Takes only the fields these helpers actually change and copies the rest off `this`. That is the
 * point worth defending: an earlier version of this listed `name`, `description` and `visibility`
 * as its own parameters and read everything else from the receiver by hand, which is how
 * `homePinnedAt` came to be dropped whenever a collection was renamed. Anything not named here
 * cannot be forgotten.
 */
private fun RemembranceCollection.rebuilt(
    name: String = this.name,
    nameLocale: Locale = this.nameLocale,
    description: String? = this.description,
    coverImagePath: String? = this.coverImagePath,
    tags: Set<Tag> = this.tags,
    members: List<Remembrance> = this.members,
    translations: Map<Locale, LocalizedPair> = this.translations,
): RemembranceCollection =
    RemembranceCollectionImpl(
        id = id,
        name = name,
        nameLocale = nameLocale,
        description = description,
        kind = kind,
        iconKey = iconKey,
        coverImagePath = coverImagePath,
        tags = tags,
        pinnedAt = pinnedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastOpenedAt = lastOpenedAt,
        homePinnedAt = homePinnedAt,
        isFavorite = isFavorite,
        members = members,
        translations = translations,
    )
