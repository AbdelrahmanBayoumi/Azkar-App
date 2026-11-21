package com.azkar.domain.service

import com.azkar.domain.model.Remembrance
import com.azkar.domain.repo.RemembranceRepository
import java.util.Locale

/**
 * The application's entry point for everything the UI does with remembrances.
 *
 * Reads delegate straight to [repository]: a query that touches one table has nothing to make
 * atomic. Writes are wrapped in [transactions] because they touch several — a remembrance's
 * favorite flag, its translations and its tag links all have to move together or not at all.
 */
class RemembranceService(
    private val repository: RemembranceRepository,
    private val transactions: TransactionRunner,
) {
    /** Saves [remembrance] and returns the stored result. */
    suspend fun save(remembrance: Remembrance): Remembrance = transactions.run { repository.save(remembrance) }

    /** Deletes [remembrance]. */
    suspend fun delete(remembrance: Remembrance) = transactions.run { repository.delete(remembrance) }

    /** Deletes the remembrance identified by [id]. */
    suspend fun deleteById(id: Long) = transactions.run { repository.deleteById(id) }

    /** Returns the remembrance identified by [id], or `null` when unknown. */
    suspend fun findById(id: Long): Remembrance? = repository.findById(id)

    /** Returns every known remembrance. */
    suspend fun findAll(): List<Remembrance> = repository.findAll()

    /** Returns whichever remembrance comes first, or `null` when there are none. */
    suspend fun findFirst(): Remembrance? = repository.findFirst()

    /** Returns the remembrances marked as favorites. */
    suspend fun findFavorites(): List<Remembrance> = repository.findFavorites()

    /** Returns the remembrances matching [query] in the [locale]'s translations. */
    suspend fun search(
        locale: Locale,
        query: String,
    ): List<Remembrance> = repository.search(locale, query)

    /** Marks the remembrance identified by [id] as a favorite. */
    suspend fun markFavorite(id: Long) = transactions.run { repository.markFavorite(id) }

    /** Clears the favorite flag of the remembrance identified by [id]. */
    suspend fun unmarkFavorite(id: Long) = transactions.run { repository.unmarkFavorite(id) }
}
