package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackgroundScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        android.util.Log.d("CampusPulse", "[Receiver] Triggered with action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == BackgroundScheduleManager.ACTION_CHECK_TIMETABLE_SILENT ||
            action == BackgroundScheduleManager.ACTION_DAILY_REFRESH
        ) {
            // Trigger immediate location check in background
            BackgroundScheduleManager.triggerImmediateCheck(context)

            // INSTANT SOUND MODE UPDATE
            // We launch a high-priority coroutine to check DB and apply sound mode immediately.
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val day = AutoSilentManager.getCurrentDayName()
                    val entries = db.timetableDao().getEntriesListForDay(day)

                    // Apply active sound mode right now (before worker even starts)
                    val activeState = AutoSilentManager.checkActiveClasses(entries)
                    android.util.Log.d("CampusPulse", "[Receiver] Applying sound mode: ${activeState.soundModeSet} for ${activeState.currentClass?.subjectName ?: "None"}")
                    AutoSilentManager.applySoundModeForClass(context, activeState)

                    // INSTANT WIDGET REFRESH
                    com.example.widget.LiveClassWidgetProvider.updateAllWidgets(context)
                    com.example.widget.ShortcutsWidgetProvider.updateAllWidgets(context)

                    // Reschedule daily alarms for the rest of today
                    BackgroundScheduleManager.scheduleBackgroundTasks(context, entries)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else if (action == BackgroundScheduleManager.ACTION_ACADEMIC_EVENT) {
            val eventName = intent.getStringExtra("event_name") ?: "Academic Event"
            val eventType = intent.getStringExtra("event_type") ?: "Event"
            NotificationHelper.showEventNotification(context, eventName, eventType)
        }
    }
}
