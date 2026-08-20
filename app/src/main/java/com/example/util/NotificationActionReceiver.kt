package com.example.util

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.data.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MISSED = "com.example.ACTION_MISSED"
        const val ACTION_CANCELLED = "com.example.ACTION_CANCELLED"
        const val ACTION_HOLIDAY = "com.example.ACTION_HOLIDAY"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val attendanceId = intent.getLongExtra("attendance_id", -1L)
        if (attendanceId == -1L) return

        val status = when (action) {
            ACTION_MISSED -> "ABSENT"
            ACTION_CANCELLED -> "NOT_CONDUCTED"
            ACTION_HOLIDAY -> "HOLIDAY"
            else -> return
        }

        val db = AppDatabase.getDatabase(context)
        val repository = AppRepository(db)

        CoroutineScope(Dispatchers.IO).launch {
            val record = repository.getPeriodRecordById(attendanceId)
            if (record != null) {
                repository.savePeriodAttendance(
                    record.copy(
                        status = status,
                        isConfirmedManually = true
                    )
                )
            }
            
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(attendanceId.toInt())
        }
    }
}
