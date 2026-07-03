package ru.cbrf.rates.domain.usecase

import ru.cbrf.rates.domain.model.CurrencyRateUiModel
import ru.cbrf.rates.domain.repository.RateRepository
import java.time.LocalDate
import javax.inject.Inject

class GetRatesForDisplayUseCase @Inject constructor(
    private val repository: RateRepository
) {
    /**
     * Returns UI models for the given display date.
     * Trend is computed by comparing displayDate against the previous available date.
     * tomorrowValue is populated if displayDate is the effective current date (today, or the
     * latest published date before it) and tomorrow rates are cached.
     */
    suspend operator fun invoke(
        displayDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): List<CurrencyRateUiModel> {
        val dateRates = repository.getRatesForDate(displayDate)
        if (dateRates.isEmpty()) return emptyList()

        val previousDate = repository.getLatestAvailableDate(displayDate.minusDays(1))
        val previousRates = previousDate?.let { repository.getRatesForDate(it) }
            ?.associateBy { it.charCode }
            ?: emptyMap()

        // Tomorrow applies when displayDate is the effective current date: today itself, or —
        // when today's rates aren't published (weekends/holidays) — the latest published date
        // before it. Comparing against today alone would hide cached tomorrow rates in that case.
        val effectiveToday = repository.getLatestAvailableDate(today)
        val tomorrowRates = if (displayDate == today || displayDate == effectiveToday) {
            repository.getRatesForDate(today.plusDays(1)).associateBy { it.charCode }
        } else {
            emptyMap()
        }

        return dateRates.map { entry ->
            CurrencyRateUiModel(
                charCode = entry.charCode,
                nameRu = entry.nameRu,
                nameEn = entry.nameEn,
                flagEmoji = entry.flagEmoji,
                todayValue = entry.unitValue,
                tomorrowValue = tomorrowRates[entry.charCode]?.unitValue,
                previousValue = previousRates[entry.charCode]?.unitValue
            )
        }
    }
}
