package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // e.g. "2026-07-24"
    val checkInTime: String, // e.g. "09:15 AM"
    val checkOutTime: String? = null, // e.g. "04:30 PM"
    val status: String = "PRESENT", // PRESENT, CHECKED_OUT, ABSENT
    val durationMinutes: Long = 0,
    val campusName: String = "Main Campus",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
)

@Entity(tableName = "timetable_entries")
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectName: String,
    val roomNumber: String = "",
    val dayOfWeek: String, // "Monday", "Tuesday", etc.
    val startTime: String, // "09:00" (24h format for easy comparison)
    val endTime: String, // "10:30"
    val instructor: String = "",
    val autoSilentEnabled: Boolean = true
)

@Entity(tableName = "scanned_documents")
data class ScannedDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val pageCount: Int = 1,
    val imagePath: String = "",
    val filterType: String = "MAGIC_COLOR", // ORIGINAL, BW, MAGIC_COLOR, HIGH_CONTRAST
    val ocrText: String = "",
    val category: String = "Notes" // Notes, Assignment, Timetable, Document
)

@Entity(tableName = "calculator_history")
data class CalculatorHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "Assignment", // Assignment, Exam, Lab, General
    val dueDate: String, // e.g. "2026-07-25"
    val dueTime: String = "09:00", // e.g. "09:00"
    val isCompleted: Boolean = false,
    val priority: String = "HIGH", // HIGH, MEDIUM, LOW
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "period_attendance",
    indices = [androidx.room.Index(value = ["timetableEntryId", "date"], unique = true)]
)
data class PeriodAttendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timetableEntryId: Long = 0, // Links to TimetableEntry.id
    val date: String, // e.g. "2026-07-24"
    val subjectName: String, // e.g. "Mathematics"
    val periodTime: String, // e.g. "09:00 - 10:00"
    val status: String = "PRESENT", // PRESENT, ABSENT
    val checksInside: Int = 0,
    val totalChecks: Int = 0,
    val isConfirmedManually: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "period_gps_checks",
    indices = [androidx.room.Index(value = ["periodAttendanceId", "intervalMinute"], unique = true)]
)
data class PeriodGpsCheck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val periodAttendanceId: Long, // Links to PeriodAttendance.id
    val intervalMinute: Int, // e.g. 0, 10, 20
    val checkTime: String, // e.g. "03:10 PM"
    val isInside: Boolean,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distance: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
