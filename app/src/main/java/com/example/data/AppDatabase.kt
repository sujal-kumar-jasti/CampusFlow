package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AttendanceRecord::class,
        TimetableEntry::class,
        ScannedDocument::class,
        CalculatorHistory::class,
        Reminder::class,
        PeriodAttendance::class,
        PeriodGpsCheck::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
    abstract fun timetableDao(): TimetableDao
    abstract fun documentDao(): DocumentDao
    abstract fun calculatorHistoryDao(): CalculatorHistoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun periodAttendanceDao(): PeriodAttendanceDao
    abstract fun periodGpsCheckDao(): PeriodGpsCheckDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "campus_companion_db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
