package ru.cbrf.rates.domain.usecase

import ru.cbrf.rates.domain.repository.RateRepository
import java.time.LocalDate
import javax.inject.Inject

class RefreshTodayRatesUseCase @Inject constructor(
    private val repository: RateRepository
) {
    /**
     * Fetches today's rates and tomorrow's rates (if not yet cached).
     * Handles weekend logic: walks back up to 7 days to find the latest published rate.
     * Returns the effective display date (may be in the past during weekends).
     */
    suspend operator fun invoke(force: Boolean = false): Result<LocalDate> {
        val today = LocalDate.now()

        // Try to get today's rate (or force refresh)
        val todayResult = if (force) {
            repository.forceRefresh(today)
        } else {
            repository.fetchRatesIfNeeded(today)
        }

        // If today is not published yet, walk back to find latest available
        val effectiveDate = if (todayResult.getOrNull() == false) {
            var candidate = today.minusDays(1)
            var found = false
            repeat(7) {
                if (!found) {
                    val r = repository.fetchRatesIfNeeded(candidate)
                    if (r.getOrNull() == true) found = true
                    else candidate = candidate.minusDays(1)
                }
            }
            if (found) candidate else today
        } else {
            today
        }

        // Opportunistically fetch tomorrow
        repository.fetchRatesIfNeeded(today.plusDays(1))

        return if (todayResult.isFailure && effectiveDate == today) {
            Result.failure(todayResult.exceptionOrNull()!!)
        } else {
            Result.success(effectiveDate)
        }
    }
}
