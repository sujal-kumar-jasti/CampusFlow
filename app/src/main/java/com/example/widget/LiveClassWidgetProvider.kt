package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.AppRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class LiveClassWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val classId = intent.getLongExtra("class_id", -1L)
        if (classId == -1L) return

        if (action == ACTION_MARK_PRESENT || action == ACTION_MARK_ABSENT || action == ACTION_MARK_CANCELLED) {
            val status = when (action) {
                ACTION_MARK_PRESENT -> "PRESENT"
                ACTION_MARK_ABSENT -> "ABSENT"
                else -> "NOT_CONDUCTED"
            }
            
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                val db = AppDatabase.getDatabase(context)
                val repo = AppRepository(db)
                val timetable = repo.allTimetableEntries.first()
                val entry = timetable.find { it.id == classId } ?: return@launch
                
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = sdf.format(Date())
                
                val existing = repo.getRecordForPeriodAndDate(classId, todayStr)
                
                val dur = try {
                    val start = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(entry.startTime)!!.time
                    val end = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(entry.endTime)!!.time
                    ((end - start) / 60000).toInt()
                } catch (_: Exception) { 60 }
                
                val max = (dur / 10) + 1
                
                val upd = com.example.data.PeriodAttendance(
                    id = existing?.id ?: 0,
                    timetableEntryId = classId,
                    date = todayStr,
                    subjectName = entry.subjectName,
                    periodTime = "${entry.startTime} - ${entry.endTime}",
                    status = status,
                    checksInside = if (status == "PRESENT") max else 0,
                    totalChecks = max,
                    isConfirmedManually = true
                )
                repo.savePeriodAttendance(upd)
                
                if (status == "NOT_CONDUCTED") {
                    // Restore sound mode to normal immediately
                    withContext(Dispatchers.Main) {
                        com.example.util.AutoSilentManager.applySoundMode(context, com.example.util.AutoSilentManager.SoundMode.NORMAL, force = true)
                    }
                }
                
                updateAllWidgets(context)
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            val db = AppDatabase.getDatabase(context)
            val repository = AppRepository(db)
            val entries = try { repository.allTimetableEntries.first() } catch (_: Exception) { emptyList() }
            
            withContext(Dispatchers.Main) {
                val views = RemoteViews(context.packageName, R.layout.widget_live_class)
                
                val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                val currentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                
                val todayEntries = entries.filter { it.dayOfWeek.equals(currentDay, ignoreCase = true) }
                
                val currentClass = todayEntries.find { entry ->
                    (currentTimeStr >= entry.startTime) && (currentTimeStr <= entry.endTime)
                }
                
                val nextClass = todayEntries.asSequence()
                    .filter { it.startTime > currentTimeStr }
                    .minByOrNull { it.startTime }

                if (currentClass != null) {
                    views.setTextViewText(R.id.widget_campus_status, "ACTIVE")
                    views.setTextViewText(R.id.widget_class_name, currentClass.subjectName)
                    views.setTextViewText(R.id.widget_class_time, "${currentClass.startTime} - ${currentClass.endTime} • Room ${currentClass.roomNumber}")
                    
                    // Button intents for active class
                    views.setViewVisibility(R.id.btn_widget_present, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.btn_widget_absent, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.btn_widget_cancel, android.view.View.VISIBLE)
                    
                    views.setOnClickPendingIntent(R.id.btn_widget_present, createActionIntent(context, ACTION_MARK_PRESENT, currentClass.id))
                    views.setOnClickPendingIntent(R.id.btn_widget_absent, createActionIntent(context, ACTION_MARK_ABSENT, currentClass.id))
                    views.setOnClickPendingIntent(R.id.btn_widget_cancel, createActionIntent(context, ACTION_MARK_CANCELLED, currentClass.id))

                } else {
                    views.setViewVisibility(R.id.btn_widget_present, android.view.View.GONE)
                    views.setViewVisibility(R.id.btn_widget_absent, android.view.View.GONE)
                    views.setViewVisibility(R.id.btn_widget_cancel, android.view.View.GONE)
                    
                    if (nextClass != null) {
                        views.setTextViewText(R.id.widget_campus_status, "NEXT")
                        views.setTextViewText(R.id.widget_class_name, nextClass.subjectName)
                        views.setTextViewText(R.id.widget_class_time, "Starts at ${nextClass.startTime} • Room ${nextClass.roomNumber}")
                    } else {
                        views.setTextViewText(R.id.widget_campus_status, "DONE")
                        views.setTextViewText(R.id.widget_class_name, "No more classes today")
                        views.setTextViewText(R.id.widget_class_time, "See you tomorrow!")
                    }
                }

                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("destination", "timetable")
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, 
                    2001, // Unique request code to avoid collision
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun createActionIntent(context: Context, action: String, classId: Long): PendingIntent {
        val intent = Intent(context, LiveClassWidgetProvider::class.java).apply {
            this.action = action
            putExtra("class_id", classId)
        }
        return PendingIntent.getBroadcast(
            context, 
            classId.toInt() + action.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_MARK_PRESENT = "com.example.ACTION_MARK_PRESENT"
        const val ACTION_MARK_ABSENT = "com.example.ACTION_MARK_ABSENT"
        const val ACTION_MARK_CANCELLED = "com.example.ACTION_MARK_CANCELLED"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, LiveClassWidgetProvider::class.java))
            for (id in ids) {
                val intent = Intent(context, LiveClassWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id))
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
