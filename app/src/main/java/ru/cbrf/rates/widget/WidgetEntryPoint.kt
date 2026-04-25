package ru.cbrf.rates.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.cbrf.rates.data.local.prefs.AppPreferences
import ru.cbrf.rates.data.local.prefs.WidgetPreferences
import ru.cbrf.rates.domain.usecase.GetRatesForDisplayUseCase
import ru.cbrf.rates.domain.usecase.RefreshTodayRatesUseCase

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getRatesForDisplay(): GetRatesForDisplayUseCase
    fun refreshTodayRates(): RefreshTodayRatesUseCase
    fun widgetPreferences(): WidgetPreferences
    fun appPreferences(): AppPreferences
}
