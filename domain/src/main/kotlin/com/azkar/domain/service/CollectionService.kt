package com.azkar.domain.service

import com.azkar.domain.exception.CollectionNotFoundException
import com.azkar.domain.model.CollectionKind
import com.azkar.domain.model.CollectionSummary
import com.azkar.domain.model.LocalizedPair
import com.azkar.domain.model.RemembranceCollection
import com.azkar.domain.model.Tag
import com.azkar.domain.model.impl.RemembranceCollectionImpl
import com.azkar.domain.model.renamed
import com.azkar.domain.repo.CollectionRepository
import java.util.Locale

/**
 * The application's entry point for everything the UI does with collections.
 *
 * Mirrors the other services — reads delegate straight to [repository], writes are wrapped in
 * [transactions] — with one rule that used to be the whole point of the class and is now simply
 * absent: every collection is editable. A collection that shipped and one the user built hold the
 * same fields, accept the same changes and store membership the same way, so there is nothing left
 * to check before a write. [CollectionKind] survives as provenance for the badge and nothing more.
 *
 * What the class still earns its keep on is the no-op guards. Every toggle below reads the row and
 * skips the write when the row already says what was asked for, because a toggle that changes
 * nothing is not an edit and should not cost a transaction — and, more importantly, should not move
 * a timestamp that a later ordering reads.
 */
class CollectionService(
    private val repository: CollectionRepository,
    private val transactions: TransactionRunner,
) {
    /** Returns every known collection, each with its members and tags loaded. */
    suspend fun findAll(): List<RemembranceCollection> = repository.findAll()

    /** Returns the collection identified by [id], or `null` when [id] is `null` or unknown. */
    suspend fun findById(id: Long?): RemembranceCollection? = repository.findById(id)

    /**
     * Creates a collection.
     *
     * A new collection always starts empty and unpinned, so the generated wash is what the user sees
     * first and can be replaced by picking a picture. A name, a description and its labels are the only
     * things chosen up front — and the labels are written in a **second statement**, not left to
     * [CollectionRepository.save].
     *
     * That second write is the whole reason this method is more than a call to `save`. `save` owns the
     * parent row and deliberately says nothing about labels, because a label is edited by picking one
     * rather than by filling in a form, and a full-aggregate write would turn every rename into a
     * silent membership edit. But a collection *being created* has no row to preserve that separation
     * for: it has never had any labels, so the only statement that can give it any is the one the
     * caller drives. Passing `tags` into the aggregate and trusting them to appear is the bug this
     * paragraph exists for — the aggregate carries them, `save` drops them on the floor, and the
     * collection reaches the shelf with none of the labels the user just picked, which looks exactly
     * like the app forgetting what they typed.
     *
     * The write is guarded on `tags` being non-empty so the ordinary case — a collection with no labels
     * at all — still costs one statement, and the aggregate is read back afterwards for the same reason
     * [duplicate] reads its copy back: the only thing that knows what the labels resolved to is the
     * storage that resolved them, so handing the caller an aggregate read before that would hand it an
     * empty set.
     */
    suspend fun create(
        name: String,
        description: String? = null,
        iconKey: String? = null,
        coverImagePath: String? = null,
        tags: Set<Tag> = emptySet(),
        translations: Map<Locale, LocalizedPair> = emptyMap(),
        nameLocale: Locale = Locale.ENGLISH,
    ): RemembranceCollection =
        transactions.run {
            val created =
                repository.save(
                    RemembranceCollectionImpl(
                        name = name,
                        nameLocale = nameLocale,
                        description = description,
                        iconKey = iconKey,
                        coverImagePath = coverImagePath,
                        tags = tags,
                        translations = translations,
                    ),
                )
            if (tags.isEmpty()) {
                created
            } else {
                val id = checkNotNull(created.id) { "A just-created collection must have an id" }
                repository.setTags(id, tags)
                repository.findById(id)
                    ?: throw CollectionNotFoundException(id)
            }
        }

    /**
     * Renames and redescribes the collection identified by [id].
     *
     * [translations] is threaded through rather than edited separately because a name and its
     * translations are one thing: a shelf whose canonical name changed but whose Arabic did not is not a
     * half-finished edit, it is a shelf that now claims to be called something its own translation does
     * not agree with. See [renamed] for why the default keeps what is there.
     */
    suspend fun rename(
        id: Long,
        name: String,
        description: String? = null,
        translations: Map<Locale, LocalizedPair>? = null,
        nameLocale: Locale? = null,
    ): RemembranceCollection =
        transactions.run {
            val current = require(id)
            repository.save(
                current.renamed(
                    name = name,
                    description = description,
                    // Defaulted at the *service* rather than at `renamed` because this layer knows what
                    // the caller almost always means: an editor that opened the canonical name and did
                    // not touch the translations has not asked to delete them.
                    translations = translations ?: current.translations,
                    nameLocale = nameLocale ?: current.nameLocale,
                ),
            )
        }

    /**
     * Copies the collection identified by [id] into a new custom collection called [newName].
     *
     * [newName] is a parameter rather than something derived here because the natural suffix is
     * localized and this layer has no access to the user's language.
     *
     * The copy is [CollectionKind.CUSTOM] even when the source shipped, because that is what a copy
     * has always meant: a shelf the user made, which the user then owns outright. The cover is
     * deliberately not copied. It names a file the application copied into its own storage, and a
     * second collection pointing at the same one would make deleting either of them ambiguous.
     */
    suspend fun duplicate(
        id: Long,
        newName: String,
        nameLocale: Locale = Locale.ENGLISH,
    ): RemembranceCollection =
        transactions.run {
            val source = require(id)
            val copy =
                repository.save(
                    RemembranceCollectionImpl(
                        name = newName,
                        nameLocale = nameLocale,
                        description = source.description,
                        iconKey = source.iconKey,
                        tags = source.tags,
                    ),
                )
            val copyId = copy.id ?: error("The duplicated collection was saved without an identifier")
            repository.addMembers(copyId, source.members.mapNotNull { it.id })
            repository.findById(copyId) ?: throw CollectionNotFoundException(copyId)
        }

    /** Deletes the collection identified by [id] along with its membership and labels. */
    suspend fun deleteById(id: Long) = transactions.run { repository.deleteById(require(id).id) }

    /** Appends [remembranceIds] to the collection, returning how many were not already members. */
    suspend fun addMembers(
        id: Long,
        remembranceIds: List<Long>,
    ): Int =
        transactions.run {
            require(id)
            repository.addMembers(id, remembranceIds)
        }

    /** Removes [remembranceIds] from the collection, returning how many were members. */
    suspend fun removeMembers(
        id: Long,
        remembranceIds: List<Long>,
    ): Int =
        transactions.run {
            require(id)
            repository.removeMembers(id, remembranceIds)
        }

    /** Moves the member at [fromIndex] to [toIndex] within the collection identified by [id]. */
    suspend fun moveMember(
        id: Long,
        fromIndex: Int,
        toIndex: Int,
    ) = transactions.run {
        require(id)
        repository.moveMember(id, fromIndex, toIndex)
    }

    /**
     * Points the collection's cover at [coverImagePath], or back at the generated wash when `null`.
     *
     * The name has already been through [com.azkar.domain.repo.CollectionRepository.setCoverImage]'s
     * storage layer by the time it gets here: it is a file the application copied into its own
     * directory, not a path the caller typed.
     */
    suspend fun setCoverImage(
        id: Long,
        coverImagePath: String?,
    ) {
        transactions.run {
            val collection = require(id)
            if (collection.coverImagePath != coverImagePath) {
                repository.setCoverImage(id, coverImagePath)
            }
        }
    }

    /** Pins the collection to the top of the shelf, or unpins it. */
    suspend fun setPinned(
        id: Long,
        pinned: Boolean,
    ) {
        transactions.run {
            val collection = require(id)
            if (collection.isPinned != pinned) {
                repository.setPinned(id, pinned)
            }
        }
    }

    /**
     * Replaces the collection's labels with exactly [tags].
     *
     * Takes tags rather than names, unlike the repository, because a caller assembling the set has
     * usually just read them off the collection already and handing them back untouched should not
     * require resolving every name to a row and back again.
     */
    suspend fun setTags(
        id: Long,
        tags: Set<Tag>,
    ) {
        transactions.run {
            val collection = require(id)
            if (collection.tags != tags) {
                repository.setTags(id, tags)
            }
        }
    }

    /** Records that the collection identified by [id] was opened just now. */
    suspend fun touchLastOpened(id: Long) = transactions.run { repository.touchLastOpened(id) }

    /**
     * Stars a collection or takes the star off again.
     *
     * Skipped when the collection is already in the requested state, for the reason [setHomePinned]
     * gives and for the same reason as every other toggle here.
     */
    suspend fun setFavorite(
        id: Long,
        favorite: Boolean,
    ) {
        transactions.run {
            val collection = require(id)
            if (collection.isFavorite != favorite) {
                repository.setFavorite(id, favorite)
            }
        }
    }

    /**
     * Puts a collection on the Home screen or takes it off again.
     *
     * A display preference in the same family as [setPinned] and [setCoverImage]: it says where the
     * user wants to see the shelf, not anything about what is on it.
     */
    suspend fun setHomePinned(
        id: Long,
        pinned: Boolean,
    ) {
        transactions.run {
            val collection = require(id)
            if (collection.isOnHome != pinned) {
                repository.setHomePinned(id, pinned)
            }
        }
    }

    /** The collections the user put on the Home screen, most recently pinned first. */
    suspend fun findHomePinned(): List<RemembranceCollection> = repository.findHomePinned()

    /**
     * Every collection as a label and a member count, the pinned ones first.
     *
     * What the Home rail reads. [findHomePinned] remains the way to ask only for what is pinned; this
     * is the way to ask for everything the rail might show, which is a question about two fields per
     * collection and not about their contents.
     */
    suspend fun findSummaries(): List<CollectionSummary> = repository.findSummaries()

    /**
     * Loads [id] or fails loudly.
     *
     * The alternative — treating a missing collection as "do nothing" — turns a stale id in a
     * pushed navigation entry into a button that appears to work and changes nothing.
     */
    private suspend fun require(id: Long): RemembranceCollection =
        repository.findById(id) ?: throw CollectionNotFoundException(id)
}
