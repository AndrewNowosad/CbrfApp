package ru.cbrf.rates.domain.model

enum class WidgetSize(val maxCurrencies: Int) {
    SMALL(1),
    MEDIUM(2),
    LARGE(4)
}

data class WidgetConfig(
    val appWidgetId: Int,
    val size: WidgetSize,
    val currencies: List<String>  // ordered list of CharCode
)
