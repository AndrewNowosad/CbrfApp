package ru.cbrf.rates.domain.model

import java.time.LocalDate
import java.util.Locale

data class RateEntry(
    val date: LocalDate,
    val charCode: String,
    val nameRu: String,
    val nameEn: String,
    val nominal: Int,
    val value: Double,
    val flagEmoji: String
) {
    /** Rate per 1 unit of currency (nominal already divided out) */
    val unitValue: Double get() = value / nominal
}

data class CurrencyRateUiModel(
    val charCode: String,
    val nameRu: String,
    val nameEn: String,
    val flagEmoji: String,
    val todayValue: Double,
    val tomorrowValue: Double?,
    val previousValue: Double?
) {
    val trend: Int? get() = previousValue?.let { todayValue.compareTo(it) }
    val displayName: String get() = if (Locale.getDefault().language == "ru") nameRu else nameEn
}
