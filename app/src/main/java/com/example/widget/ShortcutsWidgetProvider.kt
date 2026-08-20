package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class ShortcutsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_shortcuts)

        // Attendance Shortcut
        views.setOnClickPendingIntent(R.id.shortcut_attend, createPendingIntent(context, "attendance"))
        // Timetable Shortcut
        views.setOnClickPendingIntent(R.id.shortcut_timetable, createPendingIntent(context, "timetable"))
        // Reminders Shortcut
        views.setOnClickPendingIntent(R.id.shortcut_reminders, createPendingIntent(context, "dashboard")) // Goes to home for reminders
        // Scanner Shortcut
        views.setOnClickPendingIntent(R.id.shortcut_scanner, createPendingIntent(context, "scanner"))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun createPendingIntent(context: Context, destination: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("destination", destination)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 
            destination.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, ShortcutsWidgetProvider::class.java))
            for (id in ids) {
                val intent = Intent(context, ShortcutsWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id))
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
