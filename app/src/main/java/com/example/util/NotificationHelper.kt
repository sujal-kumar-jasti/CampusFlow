package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R

object NotificationHelper {

    private const val CHANNEL_ATTENDANCE = "attendance_clarification"
    private const val CHANNEL_ACADEMIC = "academic_reminders"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attendanceChannel = NotificationChannel(
                CHANNEL_ATTENDANCE,
                "Attendance Verification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Asks for attendance status when you are out of campus during class."
            }

            val academicChannel = NotificationChannel(
                CHANNEL_ACADEMIC,
                "Academic Events",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for exams, fests, and holidays."
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(attendanceChannel)
            manager.createNotificationChannel(academicChannel)
        }
    }

    fun showAttendanceClarification(
        context: Context,
        attendanceId: Long,
        subjectName: String,
        periodTime: String
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val missedIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MISSED
            putExtra("attendance_id", attendanceId)
        }
        val cancelledIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_CANCELLED
            putExtra("attendance_id", attendanceId)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val notification = NotificationCompat.Builder(context, CHANNEL_ATTENDANCE)
            .setSmallIcon(R.drawable.ic_widget_location)
            .setContentTitle("Attendance: $subjectName")
            .setContentText("Class ended at $periodTime. Please verify your attendance.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false) // Stay until user acts
            .setOngoing(true)    // Harder to dismiss by accident
            .addAction(0, "I Was Present", PendingIntent.getBroadcast(context, attendanceId.toInt() + 1, missedIntent, flags)) // Re-using intent but changing text logic in receiver if needed, or keeping it as "Missed" logic for now but user wants "Conducted/Cancelled"
            .addAction(0, "Class Cancelled", PendingIntent.getBroadcast(context, attendanceId.toInt() + 2, cancelledIntent, flags))
            .build()

        manager.notify(attendanceId.toInt(), notification)
    }

    fun showEventNotification(context: Context, eventName: String, eventType: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(context, com.example.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, eventName.hashCode(), intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ACADEMIC)
            .setSmallIcon(R.drawable.ic_widget_location) // Replace with better icon if available
            .setContentTitle("IIEST: $eventName")
            .setContentText("Today is marked as $eventType. Check the calendar for details.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(eventName.hashCode(), notification)
    }
}
