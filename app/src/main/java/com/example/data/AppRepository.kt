package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {

    suspend fun getTodayRecord(todayDate: String): AttendanceRecord? {
        return db.attendanceDao().getTodayRecord(todayDate)
    }

    suspend fun saveAttendanceRecord(record: AttendanceRecord): Long {
        return db.attendanceDao().insertRecord(record)
    }

    suspend fun updateAttendanceRecord(record: AttendanceRecord) {
        db.attendanceDao().updateRecord(record)
    }

    suspend fun clearAllAttendance() {
        db.attendanceDao().deleteAll()
    }

    // Timetable
    val allTimetableEntries: Flow<List<TimetableEntry>> = db.timetableDao().getAllEntries()

    suspend fun addTimetableEntry(entry: TimetableEntry): Long {
        return db.timetableDao().insertEntry(entry)
    }

    suspend fun addAllTimetableEntries(entries: List<TimetableEntry>) {
        db.timetableDao().insertAll(entries)
    }

    suspend fun deleteTimetableEntry(entry: TimetableEntry) {
        db.timetableDao().deleteEntry(entry)
    }

    suspend fun clearTimetable() {
        db.timetableDao().deleteAll()
    }

    // Scanned Documents
    val allScannedDocuments: Flow<List<ScannedDocument>> = db.documentDao().getAllDocuments()

    suspend fun saveScannedDocument(document: ScannedDocument): Long {
        return db.documentDao().insertDocument(document)
    }

    suspend fun deleteScannedDocument(id: Long) {
        db.documentDao().deleteById(id)
    }

    // Calculator History
    val calculatorHistory: Flow<List<CalculatorHistory>> = db.calculatorHistoryDao().getHistory()

    suspend fun addCalculatorHistory(item: CalculatorHistory) {
        db.calculatorHistoryDao().insertHistory(item)
    }

    suspend fun clearCalculatorHistory() {
        db.calculatorHistoryDao().clearHistory()
    }

    // Reminders
    val allReminders: Flow<List<Reminder>> = db.reminderDao().getAllReminders()

    suspend fun saveReminder(reminder: Reminder): Long {
        return db.reminderDao().insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: Reminder) {
        db.reminderDao().updateReminder(reminder)
    }

    suspend fun deleteReminder(id: Long) {
        db.reminderDao().deleteById(id)
    }

    // Period Attendance
    val allPeriodAttendance: Flow<List<PeriodAttendance>> = db.periodAttendanceDao().getAllPeriodAttendance()

    suspend fun getRecordForPeriodAndDate(entryId: Long, date: String): PeriodAttendance? {
        return db.periodAttendanceDao().getRecordForPeriodAndDate(entryId, date)
    }

    suspend fun getPeriodRecordById(id: Long): PeriodAttendance? {
        return db.periodAttendanceDao().getRecordById(id)
    }

    suspend fun savePeriodAttendance(record: PeriodAttendance): Long {
        return db.periodAttendanceDao().insertPeriodRecord(record)
    }

    suspend fun updateSubjectHistoryName(oldName: String, newName: String) {
        db.periodAttendanceDao().updateSubjectName(oldName, newName)
    }

    suspend fun clearAllPeriodRecords() {
        db.periodAttendanceDao().deleteAll()
    }

    // Period GPS Checks
    fun getChecksForPeriod(attendanceId: Long): Flow<List<PeriodGpsCheck>> {
        return db.periodGpsCheckDao().getChecksForPeriod(attendanceId)
    }

    suspend fun saveGpsCheck(check: PeriodGpsCheck): Long {
        return db.periodGpsCheckDao().insertCheck(check)
    }

    suspend fun checkIntervalExists(attendanceId: Long, minute: Int): Boolean {
        return db.periodGpsCheckDao().checkExists(attendanceId, minute)
    }

    suspend fun getInsideChecksCount(attendanceId: Long): Int {
        return db.periodGpsCheckDao().getInsideChecksCount(attendanceId)
    }

    suspend fun clearAllGpsChecks() {
        db.periodGpsCheckDao().deleteAll()
    }
}
