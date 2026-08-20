package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.TimetableEntry
import java.util.Calendar
import java.util.concurrent.TimeUnit

object BackgroundScheduleManager {

    private const val PERIODIC_WORK_NAME = "TimetableBackgroundWorkerPeriodic"
    private const val ONE_TIME_WORK_NAME = "TimetableBackgroundWorkerImmediate"
    const val ACTION_CHECK_TIMETABLE_SILENT = "com.example.ACTION_CHECK_TIMETABLE_SILENT"
    const val ACTION_ACADEMIC_EVENT = "com.example.ACTION_ACADEMIC_EVENT"
    const val ACTION_DAILY_REFRESH = "com.example.ACTION_DAILY_REFRESH"

    fun scheduleBackgroundTasks(context: Context, timetableEntries: List<TimetableEntry> = emptyList()) {
        try {
            // 1. Schedule Periodic WorkManager (Runs every 15 mins even when app is closed)
            val periodicWork = PeriodicWorkRequestBuilder<TimetableBackgroundWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWork
            )

            // 2. Schedule Midnight Refresh for the next day
            scheduleMidnightRefresh(context)

            // 3. Schedule Exact / Inexact Alarms for class start & end times today
            if (timetableEntries.isNotEmpty()) {
                scheduleExactClassAlarms(context, timetableEntries)
            }
            
            // 4. Schedule Academic Event Reminders
            scheduleAcademicEventReminders(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleMidnightRefresh(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1) // 12:01 AM
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val intent = Intent(context, BackgroundScheduleReceiver::class.java).apply {
            action = ACTION_DAILY_REFRESH
        }
        val pi = PendingIntent.getBroadcast(
            context,
            999, // Unique ID for midnight refresh
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }

    private fun scheduleAcademicEventReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayStr = sdf.format(java.util.Date())
        
        // Find upcoming events (today or later)
        val upcomingEvents = IIESTAcademicCalendar.events.filter { it.date >= todayStr }

        for (event in upcomingEvents) {
            try {
                val dateParts = event.date.split("-")
                val year = dateParts[0].toInt()
                val month = dateParts[1].toInt() - 1
                val day = dateParts[2].toInt()

                // LEAD TIMES: 
                // Exams: 1 week before, 1 day before, 0 days before.
                // General: 1 day before, 0 days before.
                val leadDays = if (event.type == IIESTAcademicCalendar.EventType.EXAM) {
                    listOf(7, 1, 0)
                } else {
                    listOf(1, 0)
                }

                for (lead in leadDays) {
                    val eventCal = Calendar.getInstance()
                    eventCal.set(Calendar.YEAR, year)
                    eventCal.set(Calendar.MONTH, month)
                    eventCal.set(Calendar.DAY_OF_MONTH, day)
                    eventCal.add(Calendar.DAY_OF_YEAR, -lead)
                    eventCal.set(Calendar.HOUR_OF_DAY, 8) // Notify at 8:00 AM
                    eventCal.set(Calendar.MINUTE, 0)
                    eventCal.set(Calendar.SECOND, 0)
                    eventCal.set(Calendar.MILLISECOND, 0)

                    if (eventCal.timeInMillis > System.currentTimeMillis()) {
                        val leadText = when(lead) {
                            7 -> " (In 1 Week)"
                            1 -> " (Tomorrow)"
                            else -> ""
                        }
                        val intent = Intent(context, BackgroundScheduleReceiver::class.java).apply {
                            action = ACTION_ACADEMIC_EVENT
                            putExtra("event_name", "${event.name}$leadText")
                            putExtra("event_type", event.type.name)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            (event.name + lead).hashCode(), // Unique code per lead stage
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, eventCal.timeInMillis, pendingIntent)
                            } else {
                                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, eventCal.timeInMillis, pendingIntent)
                            }
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, eventCal.timeInMillis, pendingIntent)
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun triggerImmediateCheck(context: Context) {
        try {
            val immediateWork = OneTimeWorkRequestBuilder<TimetableBackgroundWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediateWork
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleExactClassAlarms(context: Context, entries: List<TimetableEntry>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val today = AutoSilentManager.getCurrentDayName()
        val todayClasses = entries.filter { it.dayOfWeek.equals(today, ignoreCase = true) }

        for (entry in todayClasses) {
            val startMillis = parseTimeToTodayMillis(entry.startTime)
            val endMillis = parseTimeToTodayMillis(entry.endTime)
            val now = System.currentTimeMillis()

            // Schedule intervals: 0, 10, 20... minutes from start until end
            var checkTime = startMillis
            var bucketIndex = 0
            while (checkTime <= endMillis) {
                if (checkTime > now) {
                    // Unique requestCode based on entry ID and bucket index
                    val requestCode = (entry.id.toInt() * 100) + bucketIndex
                    setAlarm(context, alarmManager, checkTime, requestCode)
                }
                checkTime += 600000L // 10 minutes
                bucketIndex++
            }
            
            // Ensure end time is also checked if it didn't align exactly with 10-min marks
            if (endMillis > now && (endMillis - startMillis) % 600000L != 0L) {
                val endRequestCode = (entry.id.toInt() * 100) + 99 // Reserved for end
                setAlarm(context, alarmManager, endMillis, endRequestCode)
            }
        }
    }

    private fun setAlarm(context: Context, alarmManager: AlarmManager, triggerTimeMs: Long, requestCode: Int) {
        val intent = Intent(context, BackgroundScheduleReceiver::class.java).apply {
            action = ACTION_CHECK_TIMETABLE_SILENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Using setExactAndAllowWhileIdle for maximum precision on class boundaries
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseTimeToTodayMillis(timeStr: String): Long {
        return try {
            val totalMins = AutoSilentManager.timeStringToMinutes(timeStr)
            val h = totalMins / 60
            val m = totalMins % 60

            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, m)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } catch (e: Exception) {
            0L
        }
    }
}
