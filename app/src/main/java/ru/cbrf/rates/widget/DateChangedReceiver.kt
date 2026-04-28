package ru.cbrf.rates.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import ru.cbrf.rates.worker.RateUpdateWorker

class DateChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_DATE_CHANGED) return
        // Enqueue a one-time refresh so the widget switches to the new date immediately.
        // No network constraint: if today's rates are already cached the job runs instantly;
        // if not, it retries when connectivity is restored.
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<RateUpdateWorker>().build()
        )
    }
}
