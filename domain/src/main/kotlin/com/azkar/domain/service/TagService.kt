package com.azkar.domain.service

import com.azkar.domain.exception.TagNameTakenException
import com.azkar.domain.model.Tag
import com.azkar.domain.model.TagSummary
import com.azkar.domain.model.TagUsage
import com.azkar.domain.model.impl.TagImpl
import com.azkar.domain.repo.TagRepository
import java.util.Locale

/**
 * The application's entry point for everything the UI does with tags.
 *
 * Mirrors [RemembranceService]: reads go straight through, writes are made atomic.
 *
 * The one place a tag write is checked before it is attempted. [rename] and [create] are the only
 * operations where the domain has a rule the database merely enforces — `tag.name` is unique, but
 * "somebody else already holds this name" is a statement about the user's edit, and by the time the
 * driver raises it there is nothing left to attach it to.
 *
 * "This name" is read wider than `tag.name`. A translated tag is known by two words, and a reader
 * typing the one on screen has to be stopped for the tag that already answers to it — otherwise the
 * translation buys nothing but a second tag called the same thing. See [findClash].
 */
class TagService(
    private val repository: TagRepository,
    private val transactions: TransactionRunner,
) {
    /** Deletes the tag identified by [id], doing nothing when [id] is `null`. */
    suspend fun deleteById(id: Long?) = transactions.run { repository.softDelete(id) }

    /**
     * Brings back the tag identified by [id], with everything it was carrying, doing nothing when [id]
     * is `null`.
     */
    suspend fun restore(id: Long?) = transactions.run { repository.restore(id) }

    /** Saves [tag] and returns the stored result. */
    suspend fun save(tag: Tag): Tag = transactions.run { repository.save(tag) }

    /**
     * Renames the tag [id] to [newName] and returns the stored result.
     *
     * The conflict check and the write share one transaction rather than being two calls, so that two
     * renames racing for the same name cannot both pass the check and leave one of them to fail on the
     * index. `TagImpl` folds the name before it reaches the repository, which is what lets the
     * comparison here be an equality against the index rather than a `LOWER(...)` scan.
     *
     * Renaming a tag onto the name it already has is not a conflict and succeeds: the lookup that
     * detects the clash ignores the tag being renamed, and returning early when the folded names match
     * means an editor the user opened and confirmed without typing is not reported as an error.
     *
     * Every translation this rename carries is checked too, against every other tag, for the reason
     * [findClash] gives. A rename that would make this tag answer to a word another tag already answers
     * to is refused, so the check has to cover what the tag is being *called* as well as what it is
     * being stored as.
     */
    suspend fun rename(
        id: Long,
        newName: String,
        translations: Map<Locale, String>? = null,
        nameLocale: Locale? = null,
    ): Tag =
        transactions.run {
            val current = repository.findById(id) ?: error("Tag $id does not exist")
            val wording = translations ?: current.translations
            val renamed =
                TagImpl(
                    id = id,
                    name = newName,
                    nameLocale = nameLocale ?: current.nameLocale,
                    translations = wording,
                )
            findClash(renamed, ignoring = id)?.let { throw TagNameTakenException(it) }
            repository.save(renamed)
        }

    /**
     * Adds a tag called [name] and returns it.
     *
     * The same check as [rename] for the same reason, and one transaction for the same reason: a name
     * is taken or it is not, and the answer and the write have to agree about which.
     *
     * Only live tags block the name. A tag the user deleted is not in the way, and retyping its name
     * is how a deleted tag comes back — see `ExposedTagRepository.save`, which revives the tombstone
     * rather than inserting over it. That is the one behaviour here that is not obvious from the code
     * above it, and it is why this is not just [save] with a check bolted on: what "add" means when
     * the name is free-but-recently-used is a question about the delete, not about the insert.
     *
     * A blank [name] is rejected by `TagImpl`, which is where the rule belongs rather than in a guard
     * here — a tag with no name is not a tag, and every path that builds one should have to answer for
     * it.
     */
    suspend fun create(
        name: String,
        translations: Map<Locale, String> = emptyMap(),
        nameLocale: Locale = Locale.ENGLISH,
    ): Tag =
        transactions.run {
            val created = TagImpl(name = name, nameLocale = nameLocale, translations = translations)
            findClash(created, ignoring = null)?.let { throw TagNameTakenException(it) }
            repository.save(created)
        }

    /**
     * The name that is already taken, or `null` when this tag's is free.
     *
     * Checks the stored name and every translation against every other live tag, and answers with the
     * *word that was taken* rather than with the tag's stored name — so a user who typed "أذكار
     * الصباح" and was refused is told that name is taken, which is the name they can see on the chip
     * they are trying to attach it to.
     *
     * [ignoring] is the tag being renamed, so that renaming a tag without touching it is not a clash
     * with itself. Every translation is checked even against that tag, because a tag that translates
     * to the same word twice is a different problem from one that is merely unchanged.
     */
    private suspend fun findClash(
        candidate: Tag,
        ignoring: Long?,
    ): String? {
        val names =
            buildList {
                add(candidate.name)
                candidate.translations.values.forEach { text ->
                    if (text.isNotBlank()) add(text.trim())
                }
            }
        var clash: String? = null
        for (word in names) {
            val takenByName = repository.findByNameIgnoreCase(word)?.id?.let { it != ignoring } == true
            val takenByTranslation =
                !takenByName && repository.findByTranslationIgnoreCase(word)?.id?.let { it != ignoring } == true
            if (takenByName || takenByTranslation) {
                clash = word
                break
            }
        }
        return clash
    }

    /** Returns the live tag identified by [id], or `null` when [id] is `null`, unknown or deleted. */
    suspend fun findById(id: Long?): Tag? = repository.findById(id)

    /** Returns every live tag. */
    suspend fun findAll(): List<Tag> = repository.findAll()

    /** Every live tag with its usage counts. */
    suspend fun findSummaries(): List<TagSummary> = repository.findSummaries()

    /** Every soft-deleted tag with its usage counts. */
    suspend fun findDeletedSummaries(): List<TagSummary> = repository.findDeletedSummaries()

    /**
     * The shelves and remembrances carrying [id], or `null` when it names no live tag.
     *
     * A read, so it goes straight through without taking a transaction — the same as every other
     * read here. There is no rule for the domain to enforce on the way in, and wrapping it would
     * only add a boundary the database does not need for a single joined statement.
     */
    suspend fun findUsage(id: Long?): TagUsage? = repository.findUsage(id)
}
