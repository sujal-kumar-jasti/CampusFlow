package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date = :todayDate ORDER BY id DESC LIMIT 1")
    suspend fun getTodayRecord(todayDate: String): AttendanceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord): Long

    @Update
    suspend fun updateRecord(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAll()
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_entries ORDER BY startTime ASC")
    fun getAllEntries(): Flow<List<TimetableEntry>>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :day ORDER BY startTime ASC")
    fun getEntriesForDay(day: String): Flow<List<TimetableEntry>>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :day ORDER BY startTime ASC")
    suspend fun getEntriesListForDay(day: String): List<TimetableEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimetableEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TimetableEntry>)

    @Delete
    suspend fun deleteEntry(entry: TimetableEntry)

    @Query("DELETE FROM timetable_entries")
    suspend fun deleteAll()
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM scanned_documents ORDER BY createdTimestamp DESC")
    fun getAllDocuments(): Flow<List<ScannedDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: ScannedDocument): Long

    @Query("DELETE FROM scanned_documents WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CalculatorHistoryDao {
    @Query("SELECT * FROM calculator_history ORDER BY timestamp DESC LIMIT 50")
    fun getHistory(): Flow<List<CalculatorHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: CalculatorHistory)

    @Query("DELETE FROM calculator_history")
    suspend fun clearHistory()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, dueDate ASC, dueTime ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PeriodAttendanceDao {
    @Query("SELECT * FROM period_attendance WHERE date = :date ORDER BY id ASC")
    fun getAttendanceForDate(date: String): Flow<List<PeriodAttendance>>

    @Query("SELECT * FROM period_attendance ORDER BY timestamp DESC")
    fun getAllPeriodAttendance(): Flow<List<PeriodAttendance>>

    @Query("SELECT * FROM period_attendance WHERE LOWER(subjectName) = LOWER(:subjectName) ORDER BY timestamp DESC")
    fun getAttendanceForSubject(subjectName: String): Flow<List<PeriodAttendance>>

    @Query("SELECT * FROM period_attendance WHERE timetableEntryId = :entryId AND date = :date LIMIT 1")
    suspend fun getRecordForPeriodAndDate(entryId: Long, date: String): PeriodAttendance?

    @Query("UPDATE period_attendance SET subjectName = :newName WHERE subjectName = :oldName")
    suspend fun updateSubjectName(oldName: String, newName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriodRecord(record: PeriodAttendance): Long

    @Query("SELECT * FROM period_attendance WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): PeriodAttendance?

    @Query("DELETE FROM period_attendance")
    suspend fun deleteAll()
}

@Dao
interface PeriodGpsCheckDao {
    @Query("SELECT * FROM period_gps_checks WHERE periodAttendanceId = :attendanceId ORDER BY timestamp ASC")
    fun getChecksForPeriod(attendanceId: Long): Flow<List<PeriodGpsCheck>>

    @Query("SELECT EXISTS(SELECT 1 FROM period_gps_checks WHERE periodAttendanceId = :attendanceId AND intervalMinute = :minute)")
    suspend fun checkExists(attendanceId: Long, minute: Int): Boolean

    @Query("SELECT COUNT(*) FROM period_gps_checks WHERE periodAttendanceId = :attendanceId")
    suspend fun getTotalChecksCount(attendanceId: Long): Int

    @Query("SELECT COUNT(*) FROM period_gps_checks WHERE periodAttendanceId = :attendanceId AND isInside = 1")
    suspend fun getInsideChecksCount(attendanceId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheck(check: PeriodGpsCheck): Long

    @Query("DELETE FROM period_gps_checks")
    suspend fun deleteAll()
}
