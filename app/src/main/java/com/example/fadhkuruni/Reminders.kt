package com.example.fadhkuruni

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

/** إعداد تذكير لفئة واحدة: مفعّل أم لا، والوقت. */
data class ReminderConfig(
    val category: Category,
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
)

object Reminders {
    private const val PREFS = "settings"
    private const val CHANNEL_ID = "adhkar_reminders"
    private const val EXTRA_CATEGORY = "category"

    /** الفئات التي يمكن تذكيرها. */
    val categories: List<Category> = listOf(Category.MORNING, Category.EVENING, Category.SLEEP)

    private val defaultTimes = mapOf(
        Category.MORNING to Pair(6, 30),
        Category.EVENING to Pair(17, 30),
        Category.SLEEP to Pair(21, 30),
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): List<ReminderConfig> {
        val p = prefs(context)
        return categories.map { c ->
            val (defHour, defMinute) = defaultTimes.getValue(c)
            ReminderConfig(
                category = c,
                enabled = p.getBoolean("${c.name}_enabled", false),
                hour = p.getInt("${c.name}_hour", defHour),
                minute = p.getInt("${c.name}_minute", defMinute),
            )
        }
    }

    /** يحفظ الإعداد ويعيد جدولة تذكيره فوراً. */
    fun save(context: Context, config: ReminderConfig) {
        prefs(context).edit()
            .putBoolean("${config.category.name}_enabled", config.enabled)
            .putInt("${config.category.name}_hour", config.hour)
            .putInt("${config.category.name}_minute", config.minute)
            .apply()
        reschedule(context, config)
    }

    /** يُعيد جدولة كل التذكيرات (تُستخدم بعد إعادة تشغيل الجهاز). */
    fun rescheduleAll(context: Context) {
        load(context).forEach { reschedule(context, it) }
    }

    fun isAnyEnabled(context: Context): Boolean = load(context).any { it.enabled }

    private fun reschedule(context: Context, config: ReminderConfig) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = pendingIntent(context, config.category)
        if (!config.enabled) {
            am.cancel(intent)
            return
        }
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, config.hour)
            set(Calendar.MINUTE, config.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            intent,
        )
    }

    private fun pendingIntent(context: Context, category: Category): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_CATEGORY, category.name)
        return PendingIntent.getBroadcast(
            context,
            100 + category.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun showNotification(context: Context, category: Category) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "تذكير الأذكار", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(category.title)
            .setContentText("حان وقت ${category.title}")
            .setAutoCancel(true)
            .build()
        nm.notify(category.ordinal, notification)
    }

    /** يُستدعى من ReminderReceiver عند وصول المنبّه. */
    fun handleAlarm(context: Context, intent: Intent) {
        val name = intent.getStringExtra(EXTRA_CATEGORY) ?: return
        val category = runCatching { Category.valueOf(name) }.getOrNull() ?: return
        showNotification(context, category)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.handleAlarm(context, intent)
    }
}

/** المنبّهات تُمسح عند إعادة تشغيل الجهاز، فنعيد جدولتها هنا. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Reminders.rescheduleAll(context)
        }
    }
}
