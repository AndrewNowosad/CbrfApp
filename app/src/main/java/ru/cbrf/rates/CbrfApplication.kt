package ru.cbrf.rates

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import dagger.hilt.android.HiltAndroidApp
import ru.cbrf.rates.data.local.prefs.UpdateInterval
import ru.cbrf.rates.worker.RateUpdateWorker
import javax.inject.Inject

@HiltAndroidApp
class CbrfApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Ensure the periodic worker is always registered, even on a fresh install.
        // KEEP is a no-op if the work is already enqueued; otherwise it starts with the
        // default interval (H1). The user's saved interval (if different) was already
        // scheduled when they changed it in Settings and survives app restarts via WorkManager.
        RateUpdateWorker.schedule(this, UpdateInterval.H1, ExistingPeriodicWorkPolicy.KEEP)
    }
}
