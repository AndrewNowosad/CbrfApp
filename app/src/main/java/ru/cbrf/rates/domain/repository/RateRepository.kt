package ru.cbrf.rates.domain.repository

import ru.cbrf.rates.domain.model.RateEntry
import java.time.LocalDate

interface RateRepository {
    /** Returns cached rates for the given date, or empty list if not yet loaded */
    suspend fun getRatesForDate(date: LocalDate): List<RateEntry>

    /** Fetches rates from CBRF API if not already cached. Returns false if no data published. */
    suspend fun fetchRatesIfNeeded(date: LocalDate): Result<Boolean>

    /** Force-fetches from network regardless of cache */
    suspend fun forceRefresh(date: LocalDate): Result<Boolean>

    /** Returns the most recent date for which rates are available (cached or fetched) */
    suspend fun getLatestAvailableDate(upTo: LocalDate): LocalDate?

    /** Returns all dates that have cached data */
    suspend fun getCachedDates(): List<LocalDate>
}
