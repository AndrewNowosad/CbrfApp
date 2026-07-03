package ru.cbrf.rates.domain.usecase

import ru.cbrf.rates.domain.repository.RateRepository
import java.time.LocalDate
import javax.inject.Inject

class RefreshTodayRatesUseCase @Inject constructor(
    private val repository: RateRepository
) {
    suspend operator fun invoke(force: Boolean = false): Result<LocalDate> {
        val today = LocalDate.now()

        // Try to get today's rate (or force refresh)
        val todayResult = if (force) {
            repository.forceRefresh(today)
        } else {
            repository.fetchRatesIfNeeded(today)
        }

        // Opportunistically fetch tomorrow
        repository.fetchRatesIfNeeded(today.plusDays(1))

        if (todayResult.isFailure) {
            return Result.failure(todayResult.exceptionOrNull() ?: IllegalStateException("Unknown error"))
        }

        // When today isn't published yet, the CBR response carries an earlier publish date and
        // the repository has already backfilled the cache up to yesterday — the latest cached
        // date IS the effective date, no day-by-day walk-back needed.
        val effectiveDate = if (todayResult.getOrNull() == false) {
            repository.getLatestAvailableDate(today) ?: today
        } else {
            today
        }
        return Result.success(effectiveDate)
    }
}
