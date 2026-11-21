package com.azkar.domain.service

import com.azkar.domain.exception.CollectionNotFoundException
import com.azkar.domain.model.RecitationProgress
import com.azkar.domain.model.RecitationSummary
import com.azkar.domain.repo.CollectionRepository
import com.azkar.domain.repo.RecitationRepository

/**
 * The application's entry point for recitation progress.
 *
 * Mirrors the other services — reads delegate straight to [recitations], writes are wrapped in
 * [transactions] — with two additions that are rules about the domain rather than about storage.
 *
 * A collection is required to exist before progress can be recorded against it. The membership table
 * already carries that as a foreign key, but a check here fails with a
 * [CollectionNotFoundException] that names the collection instead of a constraint violation that
 * names a constraint.
 *
 * Counts are clamped at zero rather than allowed to go negative. Progress that could be negative
 * would make the completion maths meaningless — a member with a count of minus three is neither
 * unfinished nor finished — so a stray undo is absorbed instead of being stored.
 */
class RecitationService(
    private val recitations: RecitationRepository,
    private val collections: CollectionRepository,
    private val transactions: TransactionRunner,
) {
    /** Returns the progress recorded against every member of the collection identified by [id]. */
    suspend fun findByCollection(id: Long): List<RecitationProgress> = recitations.findByCollection(id)

    /**
     * Returns one [RecitationSummary] per named collection that has any recited members.
     *
     * Delegates straight through, with no per-collection existence check: a summary is a reading of
     * stored progress rather than a change to it, so a name that matches nothing simply comes back
     * absent. Asking about a collection that does not exist is a caller's mistake to notice, not
     * something worth a round trip per id to catch here.
     */
    suspend fun findSummaries(ids: List<Long>): Map<Long, RecitationSummary> = recitations.findSummaries(ids)

    /**
     * Adds each value of [deltas] to the matching member's count and stamps the moment just now.
     *
     * Entries worth zero are dropped before the write: a caller tapping a control to acknowledge it
     * should not create a row that says nothing happened.
     */
    suspend fun record(
        id: Long,
        deltas: Map<Long, Int>,
    ): Int =
        transactions.run {
            require(id)
            recitations.bump(id, deltas.filterValues { it != 0 })
        }

    /** Forgets every member's progress in the collection identified by [id]. */
    suspend fun reset(id: Long): Int =
        transactions.run {
            require(id)
            recitations.reset(id)
        }

    /**
     * Loads [id] or fails loudly.
     *
     * The same reasoning as [CollectionService]'s: treating a missing collection as "do nothing"
     * would turn a stale id into a control that appears to work and changes nothing.
     */
    private suspend fun require(id: Long) {
        if (collections.findById(id) == null) throw CollectionNotFoundException(id)
    }
}
