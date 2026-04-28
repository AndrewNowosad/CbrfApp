package ru.cbrf.rates.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import android.util.Log
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withTimeoutOrNull
import ru.cbrf.rates.data.local.prefs.UpdateInterval
import ru.cbrf.rates.domain.usecase.RefreshTodayRatesUseCase
import ru.cbrf.rates.widget.WidgetUpdateHelper
import java.util.concurrent.TimeUnit

@HiltWorker
class RateUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val refreshRates: RefreshTodayRatesUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val timedOut = withTimeoutOrNull(60_000L) {
            val result = refreshRates(force = false)
            updateAllWidgets()
            result
        }
        if (timedOut == null) {
            Log.w("RateUpdateWorker", "doWork timed out after 60 s — scheduling retry")
            return Result.retry()
        }
        return if (timedOut.isSuccess) Result.success() else Result.retry()
    }

    private suspend fun updateAllWidgets() {
        WidgetUpdateHelper.requestUpdate(applicationContext)
    }

    companion object {
        private const val WORK_NAME = "rate_update_periodic"

        fun schedule(
            context: Context,
            interval: UpdateInterval,
            policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // UpdateInterval minimum is H1 (1 hour) > Android's 15-minute floor, so no clamping needed.
            val request = PeriodicWorkRequestBuilder<RateUpdateWorker>(
                interval.hours, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, policy, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
