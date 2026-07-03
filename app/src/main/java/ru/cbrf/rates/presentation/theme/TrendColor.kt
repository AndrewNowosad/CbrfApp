package ru.cbrf.rates.presentation.theme

import androidx.compose.ui.graphics.Color

val TrendUpColor = Color(0xFF43A047)
val TrendDownColor = Color(0xFFE53935)

/**
 * Color highlighting a rate trend, or null when there is nothing to highlight
 * (no previous value to compare against, or the rate is unchanged).
 * [invertColors] swaps green/red for color-blind users.
 */
fun trendColor(trend: Int?, invertColors: Boolean): Color? = when {
    trend == null || trend == 0 -> null
    trend > 0 -> if (invertColors) TrendDownColor else TrendUpColor
    else -> if (invertColors) TrendUpColor else TrendDownColor
}
