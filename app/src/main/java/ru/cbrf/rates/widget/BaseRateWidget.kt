package ru.cbrf.rates.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import android.util.Log
import ru.cbrf.rates.BuildConfig
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import ru.cbrf.rates.data.local.prefs.WidgetBgColorMode
import ru.cbrf.rates.domain.model.CurrencyRateUiModel
import ru.cbrf.rates.domain.repository.RateRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import ru.cbrf.rates.presentation.MainActivity
import ru.cbrf.rates.widget.config.WidgetConfigActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Sizes used by the header row. Subclasses may override [headerConfig] to return responsive values. */
data class HeaderConfig(
    val headerSize: TextUnit,
    val iconSize: TextUnit,
    val padding: Dp
)

abstract class BaseRateWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    /** Maximum number of currencies to load and display. */
    abstract val maxCurrencies: Int

    /**
     * Returns sizes for the header row and outer padding.
     * Override in subclasses that need responsive sizing (e.g. SmallRateWidget).
     */
    @androidx.glance.GlanceComposable
    @Composable
    protected open fun headerConfig(): HeaderConfig =
        HeaderConfig(headerSize = 11.sp, iconSize = 16.sp, padding = 8.dp)

    /**
     * Renders the currency list area when [displayData] has at least one currency.
     * Called inside a [Column] that already contains the header row.
     */
    @androidx.glance.GlanceComposable
    @Composable
    protected abstract fun CurrencyContent(
        displayData: WidgetDisplayData,
        contentColor: Color,
        secondaryColor: Color,
        mainIntent: Intent
    )

    /**
     * Renders the empty state shown when no currencies are configured.
     * Override to provide widget-specific empty state (e.g. icon-only for Small).
     */
    @androidx.glance.GlanceComposable
    @Composable
    protected open fun EmptyState(
        secondaryColor: Color,
        configIntent: Intent,
        headerConfig: HeaderConfig
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(configIntent)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Tap ⚙ to configure",
                style = TextStyle(fontSize = 11.sp, color = ColorProvider(secondaryColor))
            )
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadDataAndPersistState(context, id, maxCurrencies)
        val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, data?.appWidgetId ?: 0)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        provideContent {
            val prefs = currentState<Preferences>()
            val displayData = prefs.readWidgetData() ?: data ?: return@provideContent
            val isDark = (LocalContext.current.resources.configuration.uiMode
                    and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val bgBase = when (displayData.bgColorMode) {
                WidgetBgColorMode.LIGHT -> Color.White
                WidgetBgColorMode.DARK -> Color(0xFF1C1B1F)
                WidgetBgColorMode.AUTO -> if (isDark) Color(0xFF1C1B1F) else Color.White
            }
            val bgColor = bgBase.copy(alpha = displayData.bgAlpha)
            val isDarkBg = when (displayData.bgColorMode) {
                WidgetBgColorMode.DARK -> true
                WidgetBgColorMode.LIGHT -> false
                WidgetBgColorMode.AUTO -> isDark
            }
            val contentColor = if (isDarkBg) Color(0xFFE1E1E1) else Color(0xFF212121)
            val secondaryColor = if (isDarkBg) Color(0xFF9E9E9E) else Color(0xFF757575)

            val hc = headerConfig()

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .appWidgetBackground()
                    .background(bgColor)
                    .cornerRadius(displayData.cornerRadius.dp)
                    .padding(hc.padding)
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayData.displayDate,
                            style = TextStyle(fontSize = hc.headerSize, color = ColorProvider(secondaryColor)),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = "↻",
                            style = TextStyle(fontSize = hc.iconSize, color = ColorProvider(secondaryColor)),
                            modifier = GlanceModifier.clickable(actionRunCallback<WidgetRefreshCallback>())
                        )
                        Text(
                            text = " ⚙",
                            style = TextStyle(fontSize = hc.iconSize, color = ColorProvider(secondaryColor)),
                            modifier = GlanceModifier.clickable(actionStartActivity(configIntent))
                        )
                    }

                    if (displayData.currencies.isNotEmpty()) {
                        CurrencyContent(displayData, contentColor, secondaryColor, mainIntent)
                    } else {
                        EmptyState(secondaryColor, configIntent, hc)
                    }
                }
            }
        }
    }

    internal suspend fun loadData(context: Context, glanceId: GlanceId, maxCurrencies: Int): WidgetDisplayData? {
        val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        if (BuildConfig.DEBUG) Log.d("CbrfWidget", "loadData START appWidgetId=$appWidgetId glanceId=$glanceId")

        return try {
            withTimeout(30_000L) {
                val widgetPrefs = ep.widgetPreferences()
                val appPrefs = ep.appPreferences()
                val getRates = ep.getRatesForDisplay()
                val repository = ep.rateRepository()
                val today = LocalDate.now()

                val (currencies, decimalPlaces, invertColors, bgAlpha, cornerRadius, bgColorMode) = coroutineScope {
                    val c = async { widgetPrefs.getCurrenciesOnce(appWidgetId).take(maxCurrencies) }
                    val d = async { appPrefs.decimalPlaces.first() }
                    val i = async { appPrefs.invertColors.first() }
                    val a = async { appPrefs.widgetBgAlpha.first() }
                    val r = async { appPrefs.widgetCornerRadius.first() }
                    val m = async { appPrefs.widgetBgColorMode.first() }
                    PrefsSnapshot(c.await(), d.await(), i.await(), a.await(), r.await(), m.await())
                }
                if (BuildConfig.DEBUG) Log.d("CbrfWidget", "loadData currencies=$currencies bgAlpha=$bgAlpha")

                // Find effective date; if DB is empty, fetch from network first
                var effectiveDate = repository.getLatestAvailableDate(today) ?: run {
                    ep.refreshTodayRates()(force = false)
                    repository.getLatestAvailableDate(today) ?: today
                }

                val rates = if (currencies.isEmpty()) {
                    emptyList()
                } else {
                    var result = getRates(effectiveDate, today)
                        .filter { it.charCode in currencies }
                        .sortedBy { currencies.indexOf(it.charCode).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }

                    if (result.isEmpty()) {
                        // Data missing for selected currencies — fetch and retry
                        ep.refreshTodayRates()(force = false)
                        effectiveDate = repository.getLatestAvailableDate(today) ?: today
                        result = getRates(effectiveDate, today)
                            .filter { it.charCode in currencies }
                            .sortedBy { currencies.indexOf(it.charCode).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }
                    }
                    result
                }

                val dateStr = effectiveDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
                if (BuildConfig.DEBUG) Log.d("CbrfWidget", "loadData END effectiveDate=$effectiveDate rates=${rates.size}")

                WidgetDisplayData(
                    appWidgetId = appWidgetId,
                    currencies = rates,
                    displayDate = dateStr,
                    decimalPlaces = decimalPlaces,
                    invertColors = invertColors,
                    bgAlpha = bgAlpha,
                    cornerRadius = cornerRadius,
                    bgColorMode = bgColorMode
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.w("CbrfWidget", "loadData timed out after 30 s for appWidgetId=$appWidgetId")
            null
        }
    }

    protected suspend fun loadDataAndPersistState(context: Context, glanceId: GlanceId, maxCurrencies: Int): WidgetDisplayData? {
        val data = loadData(context, glanceId, maxCurrencies) ?: return null
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().also { it.writeWidgetData(data) }
        }
        return data
    }
}

@androidx.glance.GlanceComposable
@androidx.compose.runtime.Composable
fun WidgetCurrencyRow(
    rate: CurrencyRateUiModel,
    decimalPlaces: Int,
    invertColors: Boolean,
    contentColor: Color = Color(0xFF212121),
    secondaryColor: Color = Color(0xFF757575),
    verticalPadding: androidx.compose.ui.unit.Dp = 5.dp
) {
    val trend = rate.trend
    val trendColor: Color? = when {
        trend == null || trend == 0 -> null
        trend > 0 -> if (invertColors) Color(0xFFE53935) else Color(0xFF43A047)
        else -> if (invertColors) Color(0xFF43A047) else Color(0xFFE53935)
    }
    val valueColor = trendColor ?: contentColor

    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${rate.flagEmoji} ${rate.charCode}",
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium,
                color = ColorProvider(contentColor)),
            modifier = GlanceModifier.defaultWeight()
        )
        Text(
            text = rate.todayValue.formatRate(decimalPlaces),
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(valueColor)
            )
        )
        if (rate.tomorrowValue != null) {
            val tomorrowTrend = rate.tomorrowValue.compareTo(rate.todayValue)
            val tomorrowColor = when {
                tomorrowTrend > 0 -> if (invertColors) Color(0xFFE53935) else Color(0xFF43A047)
                tomorrowTrend < 0 -> if (invertColors) Color(0xFF43A047) else Color(0xFFE53935)
                else -> secondaryColor
            }
            Text(
                text = "→ ${rate.tomorrowValue.formatRate(decimalPlaces)}",
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(tomorrowColor)
                ),
                modifier = GlanceModifier.padding(start = 6.dp)
            )
        }
    }
}

internal fun Double.formatRate(decimals: Int) = "%.${decimals}f".format(this)
