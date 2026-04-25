package ru.cbrf.rates.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ru.cbrf.rates.data.local.prefs.UpdateInterval
import ru.cbrf.rates.domain.usecase.RefreshTodayRatesUseCase
import ru.cbrf.rates.widget.LargeRateWidget
import ru.cbrf.rates.widget.MediumRateWidget
import ru.cbrf.rates.widget.SmallRateWidget
import java.util.concurrent.TimeUnit

@HiltWorker
class RateUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val refreshRates: RefreshTodayRatesUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val result = refreshRates(force = false)

        // Update all widgets regardless of success (they'll show cached data)
        updateAllWidgets()

        return if (result.isSuccess) Result.success() else Result.retry()
    }

    private suspend fun updateAllWidgets() {
        val manager = GlanceAppWidgetManager(applicationContext)
        runCatching { SmallRateWidget().updateAll(applicationContext) }
        runCatching { MediumRateWidget().updateAll(applicationContext) }
        runCatching { LargeRateWidget().updateAll(applicationContext) }
    }

    companion object {
        private const val WORK_NAME = "rate_update_periodic"

        fun schedule(context: Context, interval: UpdateInterval) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<RateUpdateWorker>(
                interval.hours, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
