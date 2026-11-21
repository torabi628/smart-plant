package ir.aminim.smartplant.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ir.aminim.smartplant.MainActivity
import ir.aminim.smartplant.R
import ir.aminim.smartplant.data.Storage
import ir.aminim.smartplant.settings.AppSettings
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** یادآور روزانه آبیاری و کوددهی «باغچه من» */
object Reminders {
    private const val CHANNEL = "care"
    private const val WORK = "daily_care"

    fun schedule(c: Context) {
        val s = AppSettings(c)
        val wm = WorkManager.getInstance(c)
        if (!s.reminders) { wm.cancelUniqueWork(WORK); return }
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, s.reminderHour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val req = PeriodicWorkRequestBuilder<CareWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(next.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun notify(c: Context, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = c.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(CHANNEL, "یادآور مراقبت گیاهان", NotificationManager.IMPORTANCE_DEFAULT))
        val pi = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java).putExtra("open", "garden").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi).setAutoCancel(true).build()
        runCatching { NotificationManagerCompat.from(c).notify(7001, n) }
    }
}

class CareWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val garden = Storage(applicationContext).loadGarden()
        val water = garden.filter { it.waterInDays() <= 0 }
        val fert = garden.filter { it.fertInDays() <= 0 }
        if (water.isEmpty() && fert.isEmpty()) return Result.success()
        val parts = mutableListOf<String>()
        if (water.isNotEmpty()) parts += "آبیاری: " + water.joinToString("، ") { it.name }
        if (fert.isNotEmpty()) parts += "کوددهی: " + fert.joinToString("، ") { it.name }
        Reminders.notify(applicationContext, "گیاهان امروز منتظر شما هستند 🌿", parts.joinToString("\n") + "\nقبل از آبیاری، خشکی خاک را با انگشت بررسی کنید.")
        return Result.success()
    }
}
