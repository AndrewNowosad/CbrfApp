package ru.cbrf.rates.widget

import ru.cbrf.rates.domain.model.CurrencyRateUiModel

data class WidgetDisplayData(
    val appWidgetId: Int,
    val currencies: List<CurrencyRateUiModel>,
    val displayDate: String,
    val decimalPlaces: Int,
    val invertColors: Boolean,
    val bgAlpha: Float,
    val cornerRadius: Float
)
