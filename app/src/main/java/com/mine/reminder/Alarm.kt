package com.mine.reminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import java.time.LocalDate
import java.time.LocalDateTime

private const val CHANNEL = "marks"
private const val ACTION_ALARM = "com.mine.reminder.ALARM"
private const val EXTRA_DATE = "date"

object Alarms {
    fun ensureChannel(context: Context) {
        val local = context.withAppLocale()
        val manager = local.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL, local.getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        manager.createNotificationChannel(channel)
    }

    fun reschedule(context: Context) = scheduleNext(context, deliverIfDue = true)

    private fun scheduleNext(context: Context, deliverIfDue: Boolean) {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val (hour, minute) = Store.time(context)
        val includeToday = stillAhead(today, hour, minute, now)
        val next = nextFire(context, today, includeToday)
        if (next == null) {
            cancel(context)
            return
        }
        if (firesImmediately(next, hour, minute, LocalDateTime.now())) {
            if (deliverIfDue && deliver(context, next)) {
                scheduleNext(context, deliverIfDue = false)
                return
            }
            val later = nextFire(context, next.plusDays(1), false)
            if (later == null) cancel(context) else arm(context, later, hour, minute)
            return
        }
        arm(context, next, hour, minute)
    }

    private fun nextFire(context: Context, today: LocalDate, includeToday: Boolean): LocalDate? {
        val early = Store.earlyDays(context)
        return Store.events(context).mapNotNull { nextAlarm(it.start, today, includeToday, it.cycles, early) }.minOrNull()
    }

    private fun arm(context: Context, date: LocalDate, hour: Int, minute: Int) {
        val whenMs = date.atTime(hour, minute).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        // setAlarmClock, not setExact: ColorOS defers exact alarms. The status-bar clock icon is the cost.
        setClock(context, whenMs, alarmIntent(context, date))
    }

    fun deliver(context: Context, date: LocalDate): Boolean {
        if (date.isBefore(LocalDate.now())) return false
        if (!notificationsOn(context)) return false
        val local = context.withAppLocale()
        val early = Store.earlyDays(context)
        ensureChannel(local)
        for (event in Store.events(context)) {
            if (!Store.wasNotified(context, event.id, date)) {
                val labels = labelsOn(event.start, date, event.cycles)
                if (labels.isNotEmpty()) {
                    post(local, event.id.toString(), event.name, congratsLine(Hit(date, labels)), event.id.hashCode())
                    Store.markNotified(context, event.id, date)
                }
            }
            if (early > 0) {
                val milestone = date.plusDays(early.toLong())
                if (!Store.wasNotified(context, event.id, milestone, early = true)) {
                    val labels = labelsOn(event.start, milestone, event.cycles)
                    if (labels.isNotEmpty()) {
                        val hit = Hit(milestone, labels)
                        post(local, "e${event.id}", event.name, earlyNotice(early, hit), event.id.hashCode())
                        Store.markNotified(context, event.id, milestone, early = true)
                    }
                }
            }
        }
        return true
    }

    fun notificationsOn(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    fun exactAlarmOn(context: Context): Boolean =
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun batteryFree(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    fun openNotifications(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        start(context, intent)
    }

    fun openExactAlarm(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(Uri.parse("package:${context.packageName}"))
        start(context, intent)
    }

    fun openBattery(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
        start(context, intent)
    }

    fun openAutostart(context: Context) {
        val screens = listOf(
            ComponentName("com.oplus.safecenter", "com.oplus.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
        )
        for (component in screens) {
            try {
                context.startActivity(Intent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: Exception) {
            }
        }
        start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}")))
    }

    private fun setClock(context: Context, whenMs: Long, operation: Intent) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            1,
            operation,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val show = PendingIntent.getActivity(
            context,
            3,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        scheduleWithExactFallback(
            canScheduleExact = { alarm.canScheduleExactAlarms() },
            scheduleExact = { alarm.setAlarmClock(AlarmManager.AlarmClockInfo(whenMs, show), pending) },
            scheduleInexact = { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending) },
        )
    }

    private fun cancel(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            1,
            alarmIntent(context, LocalDate.now()),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        ) ?: return
        alarm.cancel(pending)
    }

    private fun alarmIntent(context: Context, date: LocalDate) =
        Intent(context, AlarmReceiver::class.java).setAction(ACTION_ALARM).putExtra(EXTRA_DATE, date.toString())

    private fun post(context: Context, tag: String, title: String, text: String, id: Int) {
        val open = PendingIntent.getActivity(
            context,
            3,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(tag, id, notification)
    }

    private fun start(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ALARM -> {
                val date = intent.getStringExtra(EXTRA_DATE)?.let(LocalDate::parse) ?: LocalDate.now()
                deliver(context, date)
                reschedule(context)
            }
            else -> reschedule(context)
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Alarms.onReceive(context, intent)
}
