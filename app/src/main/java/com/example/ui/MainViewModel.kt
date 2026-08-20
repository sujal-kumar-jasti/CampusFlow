package com.example.ui

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.AttendanceRecord
import com.example.data.PeriodAttendance
import com.example.data.PeriodGpsCheck
import com.example.data.Reminder
import com.example.data.TimetableEntry
import com.example.service.GeminiApiService
import com.example.util.AutoSilentManager
import com.example.util.GpsLocationHelper
import com.example.util.IIESTAcademicCalendar
import com.example.util.ReminderAlarmReceiver
import com.example.widget.LiveClassWidgetProvider
import com.example.widget.ShortcutsWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(AppDatabase.getDatabase(application))

    // --- Core Timetable Data ---
    data class TimetableState(val entries: List<TimetableEntry> = emptyList(), val isLoaded: Boolean = false)
    private val _timetableState = MutableStateFlow(TimetableState())
    val timetableEntries: StateFlow<List<TimetableEntry>> = _timetableState.map { it.entries }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    val allPeriodAttendance: StateFlow<List<PeriodAttendance>> = repository.allPeriodAttendance.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- GPS & Campus Logic ---
    private val _campusLocation = MutableStateFlow(loadInitialCampusState())
    val campusLocation: StateFlow<GpsLocationHelper.CampusLocation> = _campusLocation.asStateFlow()

    private fun loadInitialUserLocation(): Pair<Double, Double> {
        val prefs = getApplication<Application>().getSharedPreferences("campus_companion_prefs", Context.MODE_PRIVATE)
        val lat = java.lang.Double.longBitsToDouble(prefs.getLong("last_user_lat_bits", 0L))
        val lon = java.lang.Double.longBitsToDouble(prefs.getLong("last_user_lon_bits", 0L))
        return Pair(lat, lon)
    }

    private fun loadInitialCampusState(): GpsLocationHelper.CampusLocation {
        val prefs = getApplication<Application>().getSharedPreferences("campus_companion_prefs", Context.MODE_PRIVATE)
        val def = GpsLocationHelper.CampusLocation()
        val savedName = prefs.getString("campus_name", def.name) ?: def.name
        
        // Use bits for maximum precision, fallback only to hardcoded defaults
        val savedLat = if (prefs.contains("campus_lat_bits")) {
            java.lang.Double.longBitsToDouble(prefs.getLong("campus_lat_bits", 0L))
        } else def.latitude

        val savedLon = if (prefs.contains("campus_lon_bits")) {
            java.lang.Double.longBitsToDouble(prefs.getLong("campus_lon_bits", 0L))
        } else def.longitude

        val savedRad = if (prefs.contains("campus_radius_bits")) {
            java.lang.Double.longBitsToDouble(prefs.getLong("campus_radius_bits", 0L))
        } else def.radiusMeters
        
        return GpsLocationHelper.CampusLocation(savedName, savedLat, savedLon, savedRad)
    }
    private val _userLatitude = MutableStateFlow(loadInitialUserLocation().first)
    private val _userLongitude = MutableStateFlow(loadInitialUserLocation().second)
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow(); val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()
    private val _isInsideCampus = MutableStateFlow(false)
    val isInsideCampus: StateFlow<Boolean> = _isInsideCampus.asStateFlow()
    private val _distanceToCampus = MutableStateFlow(calculateInitialDistance())
    val distanceToCampus: StateFlow<Double> = _distanceToCampus.asStateFlow()
    private val _todayRecord = MutableStateFlow<AttendanceRecord?>(null); val todayRecord: StateFlow<AttendanceRecord?> = _todayRecord.asStateFlow()

    private fun calculateInitialDistance(): Double {
        val userPos = loadInitialUserLocation()
        val campus = loadInitialCampusState()
        if (userPos.first == 0.0) return 0.0
        return GpsLocationHelper.calculateDistanceMeters(userPos.first, userPos.second, campus.latitude, campus.longitude)
    }

    // --- UI Context ---
    private val _selectedDay = MutableStateFlow(AutoSilentManager.getCurrentDayName()); val selectedDay: StateFlow<String> = _selectedDay.asStateFlow()

    private fun getTodayDateString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // --- Time Ticker for UI Reactivity ---
    private val _currentMinuteTicker = MutableStateFlow(System.currentTimeMillis() / 60000)
    
    // --- Shared State for Alarms/GPS ---
    val systemActiveClassState: StateFlow<AutoSilentManager.ActiveClassState> = combine(_timetableState, allPeriodAttendance, _currentMinuteTicker) { state, logs, _ ->
        if (!state.isLoaded) return@combine AutoSilentManager.ActiveClassState(isClassActive = false, isInitialized = false)
        val check = AutoSilentManager.checkActiveClasses(state.entries)
        val today = getTodayDateString()
        val isConfirmed = check.currentClass?.let { cur -> logs.find { it.timetableEntryId == cur.id && it.date == today }?.isConfirmedManually } ?: false
        check.copy(isInitialized = true, isConfirmedManually = isConfirmed)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AutoSilentManager.ActiveClassState(false, isInitialized = false))

    val activeClassState: StateFlow<AutoSilentManager.ActiveClassState> = combine(_timetableState, _selectedDay, allPeriodAttendance, _currentMinuteTicker) { state, day, logs, _ ->
        if (!state.isLoaded) return@combine AutoSilentManager.ActiveClassState(isClassActive = false, isInitialized = false)
        val check = AutoSilentManager.checkActiveClasses(state.entries, forcedCurrentDay = day)
        val today = getTodayDateString()
        val isConfirmed = check.currentClass?.let { cur -> logs.find { it.timetableEntryId == cur.id && it.date == today }?.isConfirmedManually } ?: false
        check.copy(isInitialized = true, isConfirmedManually = isConfirmed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AutoSilentManager.ActiveClassState(false, isInitialized = false))

    // --- Reactive Period Status (Derived from DB) ---
    val classInsideCount: StateFlow<Int> = combine(systemActiveClassState, allPeriodAttendance) { state, logs ->
        val curClass = state.currentClass ?: return@combine 0
        val today = getTodayDateString()
        logs.find { it.timetableEntryId == curClass.id && it.date == today }?.checksInside ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isConfirmedManually: StateFlow<Boolean> = combine(systemActiveClassState, allPeriodAttendance) { state, logs ->
        val curClass = state.currentClass ?: return@combine false
        val today = getTodayDateString()
        logs.find { it.timetableEntryId == curClass.id && it.date == today }?.isConfirmedManually ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- Unresolved Session Review ---
    val unresolvedSessions: StateFlow<List<PeriodAttendance>> = allPeriodAttendance.map { logs ->
        val nowMins = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 + Calendar.getInstance().get(Calendar.MINUTE)
        val todayStr = getTodayDateString()
        
        logs.filter { it.status == "PENDING" && !it.isConfirmedManually }
            .filter { log ->
                // Only show if class has ended or it's a previous day
                if (log.date < todayStr) true
                else {
                    val endMins = AutoSilentManager.timeStringToMinutes(log.periodTime.split(" - ").getOrNull(1) ?: "")
                    nowMins >= endMins
                }
            }
            .sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun resolveSession(attendanceId: Long, status: String) {
        viewModelScope.launch {
            val record = allPeriodAttendance.value.find { it.id == attendanceId }
            record?.let {
                repository.savePeriodAttendance(
                    it.copy(status = status, isConfirmedManually = true)
                )
            }
        }
    }

    val groupedSubjectSummaries: StateFlow<List<SubjectAttendanceSummary>> = combine(timetableEntries, allPeriodAttendance) { entries, _ ->
        getGroupedSubjectAttendanceSummaries(entries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshTodayAttendance(); initDefaultSampleTimetableIfNeeded(); normalizeExistingTimetableEntries()
        checkAndPerformSemesterCleanup(); syncAcademicReminders()
        
        // Start minute ticker
        viewModelScope.launch {
            while(true) {
                kotlinx.coroutines.delay(10000.milliseconds) // Check every 10 seconds to catch the exact minute roll
                _currentMinuteTicker.value = System.currentTimeMillis() / 60000
            }
        }

        viewModelScope.launch { repository.allTimetableEntries.collectLatest { entries ->
            _timetableState.value = TimetableState(entries, isLoaded = true)
            com.example.util.BackgroundScheduleManager.scheduleBackgroundTasks(getApplication(), entries); updateWidget()
        }}
        // Refresh distance after init to be sure
        updateGpsLocation(_userLatitude.value, _userLongitude.value, uiOnly = true)
    }

    private fun syncAcademicReminders() {
        viewModelScope.launch {
            val existing = repository.allReminders.first()
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            // FILTER: Only important events in the FUTURE
            val academicEvents = IIESTAcademicCalendar.events.filter { 
                it.isImportant && it.date >= todayStr 
            }
            
            // Cleanup existing automated reminders that are now in the past
            existing.filter { it.title.startsWith("IIEST:") && it.dueDate < todayStr }.forEach {
                repository.deleteReminder(it.id)
            }
            
            academicEvents.forEach { event ->
                val title = "IIEST: ${event.name}"
                if (existing.none { it.title == title && it.dueDate == event.date }) {
                    val reminder = Reminder(
                        title = title,
                        description = "Automated Academic Reminder",
                        dueDate = event.date,
                        dueTime = "08:00 AM",
                        priority = "HIGH",
                        isCompleted = false
                    )
                    val id = repository.saveReminder(reminder)
                    scheduleReminderAlarm(reminder.copy(id = id))
                }
            }
        }
    }

    private fun checkAndPerformSemesterCleanup() {
        val prefs = getApplication<Application>().getSharedPreferences("campus_companion_prefs", Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // Semester 7 ends on Nov 13, 2026. Cleanup should trigger after that date.
        val sem7EndDate = "2026-11-13"
        val cleanupDone = prefs.getBoolean("sem7_cleanup_done", false)

        if (todayStr > sem7EndDate && !cleanupDone) {
            viewModelScope.launch {
                // 1. Generate Final Archive PDF
                val summaries = getGroupedSubjectAttendanceSummaries(repository.allTimetableEntries.first())
                if (summaries.isNotEmpty()) {
                    val context = getApplication<Application>()
                    val pdfFile = com.example.util.ExportUtils.createTimetableSubjectPdfReport(context, summaries, "IIEST SEM 7 ARCHIVE")
                    android.util.Log.d("CampusPulse", "[Cleanup] Sem 7 Archive created: ${pdfFile?.absolutePath}")
                }

                // 2. Wipe DB
                repository.clearAllPeriodRecords()
                repository.clearTimetable()
                repository.clearAllGpsChecks()

                // 3. Mark as done
                prefs.edit { putBoolean("sem7_cleanup_done", true) }
                android.util.Log.d("CampusPulse", "[Cleanup] Semester 7 data cleared successfully.")
            }
        }
    }

    private fun updateWidget() {
        LiveClassWidgetProvider.updateAllWidgets(getApplication())
        ShortcutsWidgetProvider.updateAllWidgets(getApplication())
    }

    /**
     * Ensures time is in "HH:mm" 24-hour format for database and widget logic.
     */
    fun formatTimeForStorage(time: String): String {
        return try {
            // If already in HH:mm format
            if (time.contains(":") && (time.length <= 5)) {
                val parts = time.split(":")
                val h = parts[0].padStart(2, '0')
                val m = parts[1].padStart(2, '0')
                "$h:$m"
            } else {
                // Try to parse from AM/PM format if provided
                val inputFormats = listOf("h:mm a", "hh:mm a", "H:mm", "HH:mm")
                var result = time
                for (format in inputFormats) {
                    try {
                        val date = SimpleDateFormat(format, Locale.getDefault()).parse(time)
                        if (date != null) {
                            result = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                            break
                        }
                    } catch (e: Exception) {}
                }
                result
            }
        } catch (e: Exception) { time }
    }
    fun selectDay(day: String) { _selectedDay.value = day }
    fun refreshActiveClassState() { _selectedDay.value = AutoSilentManager.getCurrentDayName() }

    fun addManualClass(entry: TimetableEntry) { 
        viewModelScope.launch { 
            val normalized = entry.copy(
                startTime = formatTimeForStorage(entry.startTime),
                endTime = formatTimeForStorage(entry.endTime)
            )
            repository.addTimetableEntry(normalized)
            updateWidget() 
        } 
    }
    fun deleteClassEntry(entry: TimetableEntry) { viewModelScope.launch { repository.deleteTimetableEntry(entry); updateWidget() } }
    fun updateTimetableEntry(entry: TimetableEntry) {
        viewModelScope.launch { 
            val normalized = entry.copy(
                startTime = formatTimeForStorage(entry.startTime),
                endTime = formatTimeForStorage(entry.endTime)
            )
            
            // Merge history if name changed
            val existing = timetableEntries.value.find { it.id == entry.id }
            if (existing != null && existing.subjectName != normalized.subjectName) {
                repository.updateSubjectHistoryName(existing.subjectName, normalized.subjectName)
            }

            repository.addTimetableEntry(normalized)
            updateWidget() 
        } 
    }

    fun confirmAttendanceManually(classId: Long, status: String = "PRESENT") {
        val cur = timetableEntries.value.find { it.id == classId } ?: return
        val date = getTodayDateString()
        val dur = parseTimeToMinutes(cur.endTime) - parseTimeToMinutes(cur.startTime)
        val max = (dur / 10) + 1
        viewModelScope.launch {
            val ex = repository.getRecordForPeriodAndDate(classId, date)
            val upd = PeriodAttendance(
                id = ex?.id ?: 0, 
                timetableEntryId = classId, 
                date = date, 
                subjectName = cur.subjectName, 
                periodTime = "${cur.startTime} - ${cur.endTime}", 
                status = status, 
                checksInside = if (status == "PRESENT") max else 0, 
                totalChecks = max, 
                isConfirmedManually = true
            )
            repository.savePeriodAttendance(upd)
            updateWidget()
        }
    }

    fun updateGpsLocation(lat: Double, lon: Double, uiOnly: Boolean = false) {
        if (lat == 0.0 && lon == 0.0) return // Ignore invalid/default resets
        
        _userLatitude.value = lat; _userLongitude.value = lon
        
        // PERSIST LATEST TO PREFS for instant load on next app start
        val prefs = getApplication<Application>().getSharedPreferences("campus_companion_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putLong("last_user_lat_bits", java.lang.Double.doubleToRawLongBits(lat))
                .putLong("last_user_lon_bits", java.lang.Double.doubleToRawLongBits(lon))
        }

        val campus = _campusLocation.value; val dist = GpsLocationHelper.calculateDistanceMeters(lat, lon, campus.latitude, campus.longitude); _distanceToCampus.value = dist; val inside = dist <= campus.radiusMeters; _isInsideCampus.value = inside
        if (!uiOnly) {
            if (inside) autoCheckInIfEligible(); processActiveClassLocationCheck(inside, lat, lon, dist)
        }
    }

    private fun processActiveClassLocationCheck(isInside: Boolean, lat: Double, lon: Double, dist: Double) {
        val sysState = systemActiveClassState.value
        val currentClass = sysState.currentClass ?: return
        
        val dateStr = getTodayDateString()
        val cal = Calendar.getInstance()
        val startMins = AutoSilentManager.timeStringToMinutes(currentClass.startTime)
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val elapsed = currentMins - startMins
        
        // Window-based logic: floor to nearest 10. 
        if (elapsed < -2) return // Too early
        val intervalMinute = (elapsed / 10) * 10
        if (intervalMinute > (AutoSilentManager.timeStringToMinutes(currentClass.endTime) - startMins)) return

        viewModelScope.launch {
            val existing = repository.getRecordForPeriodAndDate(currentClass.id, dateStr)
            if (existing?.isConfirmedManually == true) return@launch
            
            if (existing != null && repository.checkIntervalExists(existing.id, intervalMinute)) return@launch

            val expectedTotal = sysState.expectedTotalChecks
            val baseRecord = PeriodAttendance(
                id = existing?.id ?: 0,
                timetableEntryId = currentClass.id,
                date = dateStr,
                subjectName = currentClass.subjectName,
                periodTime = "${currentClass.startTime} - ${currentClass.endTime}",
                status = "PENDING",
                checksInside = existing?.checksInside ?: 0,
                totalChecks = expectedTotal,
                timestamp = System.currentTimeMillis()
            )
            val attendanceId = repository.savePeriodAttendance(baseRecord)
            repository.saveGpsCheck(PeriodGpsCheck(
                periodAttendanceId = attendanceId, 
                intervalMinute = intervalMinute, 
                checkTime = getCurrentTimeString(), 
                isInside = isInside,
                latitude = lat,
                longitude = lon,
                distance = dist
            ))
            
            val finalInside = repository.getInsideChecksCount(attendanceId)
            val isPresent = expectedTotal > 0 && finalInside >= (expectedTotal + 1) / 2
            repository.savePeriodAttendance(baseRecord.copy(id = attendanceId, checksInside = finalInside, totalChecks = expectedTotal, status = if (isPresent) "PRESENT" else "PENDING" ))
        }
    }

    private fun saveCampusState() {
        val prefs = getApplication<Application>().getSharedPreferences("campus_companion_prefs", Context.MODE_PRIVATE)
        val campus = _campusLocation.value
        prefs.edit {
            putString("campus_name", campus.name)
            // Store as bits to preserve full 64-bit Double precision
            putLong("campus_lat_bits", java.lang.Double.doubleToRawLongBits(campus.latitude))
            putLong("campus_lon_bits", java.lang.Double.doubleToRawLongBits(campus.longitude))
            putLong("campus_radius_bits", java.lang.Double.doubleToRawLongBits(campus.radiusMeters))
        }
    }

    fun fetchCurrentLocationAndDetectCampus(context: Context) {
        if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            android.util.Log.w("CampusPulse", "[Location] fetch requested but permission denied.")
            return
        }
        
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        
        if (!isGpsEnabled && !isNetworkEnabled) {
            android.util.Log.w("CampusPulse", "[Location] Both GPS and Network providers are DISABLED.")
            return
        }

        android.util.Log.d("CampusPulse", "[Location] Manual fetch triggered. GPS=$isGpsEnabled, Net=$isNetworkEnabled")
        val fusedClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
        
        // AGGRESSIVE INSTANT FETCH
        viewModelScope.launch {
            try {
                // 1. Try to get very recent last known location (within 10s)
                val lastLoc = fusedClient.lastLocation.await()
                if (lastLoc != null && (System.currentTimeMillis() - lastLoc.time) < 10000) {
                    android.util.Log.d("CampusPulse", "[Location] Using very recent last known.")
                    updateGpsLocation(lastLoc.latitude, lastLoc.longitude)
                    return@launch
                }

                // 2. Otherwise request high-priority current location
                val cts = com.google.android.gms.tasks.CancellationTokenSource()
                val location = fusedClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
                
                if (location != null) {
                    android.util.Log.d("CampusPulse", "[Location] Current high accuracy success.")
                    updateGpsLocation(location.latitude, location.longitude)
                } else {
                    android.util.Log.w("CampusPulse", "[Location] Current location returned null. Fallback to any last known.")
                    fusedClient.lastLocation.await()?.let { 
                        updateGpsLocation(it.latitude, it.longitude)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CampusPulse", "[Location] Aggressive fetch failed: ${e.message}")
            }
        }
    }

    fun updateCampusSettings(name: String, lat: Double, lon: Double, radius: Double) {
        _campusLocation.value = GpsLocationHelper.CampusLocation(name, lat, lon, radius)
        saveCampusState()
        updateGpsLocation(_userLatitude.value, _userLongitude.value)
    }
    fun resetAutoDetectCampus() { _campusLocation.value = loadInitialCampusState(); saveCampusState() }

    private fun getCurrentTimeString(): String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    fun refreshTodayAttendance() { viewModelScope.launch { _todayRecord.value = repository.getTodayRecord(getTodayDateString()) } }
    private fun autoCheckInIfEligible() { viewModelScope.launch { val today = getTodayDateString(); if (repository.getTodayRecord(today) == null) { repository.saveAttendanceRecord(AttendanceRecord(date = today, checkInTime = getCurrentTimeString(), campusName = _campusLocation.value.name, latitude = _userLatitude.value, longitude = _userLongitude.value)); refreshTodayAttendance() } } }
    fun manualCheckIn() { viewModelScope.launch { val today = getTodayDateString(); val ex = repository.getTodayRecord(today); if (ex == null) repository.saveAttendanceRecord(AttendanceRecord(date = today, checkInTime = getCurrentTimeString(), campusName = _campusLocation.value.name, latitude = _userLatitude.value, longitude = _userLongitude.value)) else repository.updateAttendanceRecord(ex.copy(checkInTime = getCurrentTimeString(), status = "PRESENT")); refreshTodayAttendance() } }

    fun addManualAttendanceForDate(date: String, entry: TimetableEntry, status: String) {
        viewModelScope.launch {
            val ex = repository.getRecordForPeriodAndDate(entry.id, date)
            val dur = parseTimeToMinutes(entry.endTime) - parseTimeToMinutes(entry.startTime)
            val max = (dur / 10) + 1
            
            val upd = PeriodAttendance(
                id = ex?.id ?: 0,
                timetableEntryId = entry.id,
                date = date,
                subjectName = entry.subjectName,
                periodTime = "${entry.startTime} - ${entry.endTime}",
                status = status,
                checksInside = if (status == "PRESENT") max else 0,
                totalChecks = max,
                isConfirmedManually = true,
                timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)?.time ?: System.currentTimeMillis()
            )
            repository.savePeriodAttendance(upd)
            updateWidget()
        }
    }

    fun addManualAttendanceRecord(sub: String, date: String, status: String) { viewModelScope.launch { repository.savePeriodAttendance(PeriodAttendance(timetableEntryId = -1L, date = date, subjectName = sub, periodTime = "Manual", status = status, checksInside = if (status == "PRESENT") 1 else 0, totalChecks = 1)); updateWidget() } }
    fun getGpsChecksForPeriod(id: Long): Flow<List<PeriodGpsCheck>> = repository.getChecksForPeriod(id)

    // --- Reminders ---
    val reminders: StateFlow<List<Reminder>> = repository.allReminders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    fun addReminder(reminder: Reminder) { viewModelScope.launch { val id = repository.saveReminder(reminder); scheduleReminderAlarm(reminder.copy(id = id)) } }
    private fun scheduleReminderAlarm(reminder: Reminder) {
        val am = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dueDate = try { sdf.parse("${reminder.dueDate} ${reminder.dueTime}") } catch (_: Exception) { null } ?: return
        
        val intent = Intent(getApplication(), ReminderAlarmReceiver::class.java).apply { 
            putExtra("title", reminder.title)
            putExtra("description", reminder.description)
            putExtra("id", reminder.id) 
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // 1. MAIN ALARM (On the Day/Time)
        if (dueDate.after(Date())) {
            val pi = PendingIntent.getBroadcast(getApplication(), reminder.id.toInt(), intent, flags)
            scheduleExact(am, dueDate.time, pi)
        }

        // 2. LEAD ALARM (1 Day Before at 08:00 AM)
        val leadCal = Calendar.getInstance()
        leadCal.time = dueDate
        leadCal.add(Calendar.DAY_OF_YEAR, -1)
        leadCal.set(Calendar.HOUR_OF_DAY, 8)
        leadCal.set(Calendar.MINUTE, 0)
        
        if (leadCal.timeInMillis > System.currentTimeMillis() && leadCal.timeInMillis < dueDate.time) {
            val leadIntent = Intent(getApplication(), ReminderAlarmReceiver::class.java).apply {
                putExtra("title", "${reminder.title} (Due Tomorrow)")
                putExtra("description", reminder.description)
                putExtra("id", reminder.id + 100000) // Unique ID for lead
            }
            val leadPi = PendingIntent.getBroadcast(getApplication(), (reminder.id + 100000).toInt(), leadIntent, flags)
            scheduleExact(am, leadCal.timeInMillis, leadPi)
        }
    }

    private fun scheduleExact(am: AlarmManager, timeMs: Long, pi: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pi)
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, timeMs, pi)
                }
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pi)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
    fun toggleReminderCompleted(reminder: Reminder) { viewModelScope.launch { repository.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted)) } }
    fun deleteReminder(id: Long) { viewModelScope.launch { repository.deleteReminder(id) } }

    // --- Timetable Logic ---
    private val _isParsingTimetable = MutableStateFlow(false); val isParsingTimetable: StateFlow<Boolean> = _isParsingTimetable.asStateFlow()
    private val _timetableMessage = MutableStateFlow<String?>(null)
    fun parseTimetableFromBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _isParsingTimetable.value = true
            _timetableMessage.value = null
            val result = GeminiApiService.parseTimetableFromImage(bitmap)
            if (result.isSuccess) {
                val entries = result.getOrDefault(emptyList())
                if (entries.isNotEmpty()) {
                    repository.clearTimetable()
                    repository.clearAllAttendance()
                    repository.clearAllPeriodRecords()
                    repository.clearAllGpsChecks()
                    repository.addAllTimetableEntries(entries)
                    updateWidget()
                    _timetableMessage.value = "Timetable updated! Previous attendance and logs cleared."
                } else {
                    _timetableMessage.value = "No classes found in the image. Please try a clearer photo."
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Failed to process image."
                _timetableMessage.value = "Error: $error"
            }
            _isParsingTimetable.value = false
        }
    }

    fun getSubjectAttendanceSummary(entry: TimetableEntry): SubjectAttendanceSummary {
        val norm = normalizeSubjectName(entry.subjectName)
        val sameSubjectEntries = _timetableState.value.entries.filter { normalizeSubjectName(it.subjectName) == norm }
        if (sameSubjectEntries.isNotEmpty()) {
            val summaries = getGroupedSubjectAttendanceSummaries(sameSubjectEntries)
            if (summaries.isNotEmpty()) return summaries.first()
        }
        return SubjectAttendanceSummary(entry.subjectName, entry.instructor.ifBlank { "Faculty Member" }, entry.roomNumber.ifBlank { "General Hall" }, "${entry.startTime} - ${entry.endTime}", entry.dayOfWeek, 1, 0, 0f, false, emptyList())
    }

    data class SubjectAttendanceSummary(
        val subjectName: String, 
        val instructor: String, 
        val roomNumber: String, 
        val periodTime: String, 
        val dayOfWeek: String, 
        val totalConducted: Int, 
        val attended: Int, 
        val percentage: Float, 
        val isSafe: Boolean, 
        val recentLogs: List<PeriodAttendance>,
        val requiredToReach75: Int = 0,
        val canSkip: Int = 0,
        val isEndOfSemester: Boolean = false
    )
    
    fun getGroupedSubjectAttendanceSummaries(entries: List<TimetableEntry>): List<SubjectAttendanceSummary> {
        if (entries.isEmpty()) return emptyList()
        val installTs = getInstallationTimestamp()
        val grouped = entries.groupBy { normalizeSubjectName(it.subjectName) }
        val cal = Calendar.getInstance()
        
        return grouped.map { (normKey, subjectEntries) ->
            val canonicalName = getCanonicalDisplayName(subjectEntries)
            val instructors = subjectEntries.map { it.instructor.trim() }.filter { it.isNotBlank() }.distinct().joinToString(", ").ifBlank { "Faculty Member" }
            val rooms = subjectEntries.map { it.roomNumber.trim() }.filter { it.isNotBlank() }.distinct().joinToString(", ").ifBlank { "General Hall" }
            val scheduleSummary = subjectEntries.joinToString(" • ") { "${it.dayOfWeek.take(3)} ${it.startTime}" }
            
            // Logs for this subject. We include logs even BEFORE install if they were manually added.
            val logs = allPeriodAttendance.value.filter { normalizeSubjectName(it.subjectName) == normKey }
            
            // Average duration of a class for this subject to convert minutes back to "class count" for hints
            val avgDuration = subjectEntries.map { parseTimeToMinutes(it.endTime) - parseTimeToMinutes(it.startTime) }
                .filter { it > 0 }.average().takeIf { !it.isNaN() } ?: 60.0

            // CALCULATION BASED ON MINUTES
            var attendedMins = 0
            var manualConductedMins = 0
            
            logs.forEach { log ->
                val duration = getDurationFromPeriodTime(log.periodTime)
                if (log.status.equals("PRESENT", ignoreCase = true)) {
                    attendedMins += duration
                }
                if (!log.status.equals("PENDING", ignoreCase = true) && 
                    !log.status.equals("NOT_CONDUCTED", ignoreCase = true) &&
                    !log.status.equals("HOLIDAY", ignoreCase = true)) {
                    manualConductedMins += duration
                }
            }
            
            var autoConductedMins = 0
            subjectEntries.forEach { entry ->
                val entryDow = when (entry.dayOfWeek.uppercase(Locale.getDefault())) {
                    "MONDAY" -> Calendar.MONDAY
                    "TUESDAY" -> Calendar.TUESDAY
                    "WEDNESDAY" -> Calendar.WEDNESDAY
                    "THURSDAY" -> Calendar.THURSDAY
                    "FRIDAY" -> Calendar.FRIDAY
                    "SATURDAY" -> Calendar.SATURDAY
                    "SUNDAY" -> Calendar.SUNDAY
                    else -> Calendar.MONDAY
                }
                
                val tempCal = Calendar.getInstance().apply {
                    timeInMillis = installTs
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                while (!tempCal.after(cal)) {
                    if (tempCal.get(Calendar.DAY_OF_WEEK) == entryDow) {
                        val classEndCal = tempCal.clone() as Calendar
                        val endMins = parseTimeToMinutes(entry.endTime)
                        classEndCal.set(Calendar.HOUR_OF_DAY, endMins / 60)
                        classEndCal.set(Calendar.MINUTE, endMins % 60)
                        
                        if (classEndCal.timeInMillis > installTs && classEndCal.timeInMillis < cal.timeInMillis) {
                            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tempCal.time)
                            
                            if (!IIESTAcademicCalendar.isNoClassDay(dateStr)) {
                                val specificLog = logs.find { it.date == dateStr && it.timetableEntryId == entry.id }
                                if (specificLog?.status != "NOT_CONDUCTED" && specificLog?.status != "HOLIDAY") {
                                    val duration = parseTimeToMinutes(entry.endTime) - parseTimeToMinutes(entry.startTime)
                                    autoConductedMins += duration
                                }
                            }
                        }
                    }
                    tempCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            
            val totalConductedMins = maxOf(manualConductedMins, autoConductedMins)
            val percentage = if (totalConductedMins > 0) (attendedMins.toFloat() / totalConductedMins.toFloat()) * 100f else 0f
            
            // HINTS CALCULATIONS (In minutes, then converted to approx classes)
            var reqTo75Classes = 0
            var skipCountClasses = 0
            
            if (totalConductedMins > 0) {
                if (percentage < 75f) {
                    val reqMins = (3.0 * totalConductedMins - 4.0 * attendedMins).coerceAtLeast(0.0)
                    reqTo75Classes = kotlin.math.ceil(reqMins / avgDuration).toInt().coerceAtLeast(1)
                } else {
                    val skipMins = ((4.0 * attendedMins / 3.0) - totalConductedMins).coerceAtLeast(0.0)
                    skipCountClasses = kotlin.math.floor(skipMins / avgDuration).toInt().coerceAtLeast(0)
                }
            }

            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val isEndOfSem = todayStr >= "2026-11-13" && todayStr < "2027-01-04"

            SubjectAttendanceSummary(
                canonicalName, instructors, rooms, scheduleSummary,
                "${subjectEntries.size} Class${if (subjectEntries.size > 1) "es" else ""}/Wk",
                totalConductedMins, attendedMins, percentage,
                percentage >= 75f || totalConductedMins == 0,
                logs.take(20),
                requiredToReach75 = reqTo75Classes,
                canSkip = skipCountClasses,
                isEndOfSemester = isEndOfSem
            )
        }
    }

    private fun getDurationFromPeriodTime(periodTime: String): Int {
        return try {
            val parts = periodTime.split("-")
            if (parts.size == 2) {
                parseTimeToMinutes(parts[1].trim()) - parseTimeToMinutes(parts[0].trim())
            } else 0
        } catch (e: Exception) { 0 }
    }

    private fun initDefaultSampleTimetableIfNeeded() {
        viewModelScope.launch {
            val currentEntries = repository.allTimetableEntries.first()
            if (currentEntries.isEmpty()) {
                val defaultEntries = listOf(TimetableEntry(subjectName = "Internet Tech. Lab (HX) / Inf. System & Security Lab (HY)", roomNumber = "Lab", dayOfWeek = "Monday", startTime = "09:55", endTime = "12:40", instructor = "IB / RN"), TimetableEntry(subjectName = "Internet Tech.", roomNumber = "Classroom", dayOfWeek = "Monday", startTime = "13:50", endTime = "14:45", instructor = "IB"), TimetableEntry(subjectName = "Elective I", roomNumber = "Classroom", dayOfWeek = "Tuesday", startTime = "10:50", endTime = "12:40", instructor = "BS/PG/SD"), TimetableEntry(subjectName = "Inf. System & Security", roomNumber = "Classroom", dayOfWeek = "Tuesday", startTime = "13:50", endTime = "14:45", instructor = "SK"), TimetableEntry(subjectName = "Inf. System & Security", roomNumber = "Classroom", dayOfWeek = "Wednesday", startTime = "10:50", endTime = "12:40", instructor = "SK"), TimetableEntry(subjectName = "Internet Tech.", roomNumber = "Classroom", dayOfWeek = "Wednesday", startTime = "13:50", endTime = "15:40", instructor = "IB"), TimetableEntry(subjectName = "Elective I", roomNumber = "Classroom", dayOfWeek = "Thursday", startTime = "11:45", endTime = "12:40", instructor = "BS/PG/SD"), TimetableEntry(subjectName = "HSS", roomNumber = "Classroom", dayOfWeek = "Thursday", startTime = "13:50", endTime = "14:45", instructor = ""), TimetableEntry(subjectName = "HSS", roomNumber = "Classroom", dayOfWeek = "Friday", startTime = "10:50", endTime = "12:40", instructor = ""), TimetableEntry(subjectName = "Inf. System & Security Lab (HX) / Internet Tech. Lab (HY)", roomNumber = "Lab", dayOfWeek = "Friday", startTime = "13:50", endTime = "16:35", instructor = "SK / IB"))
                repository.addAllTimetableEntries(defaultEntries)
            } else { deduplicateExistingTimetableEntries(currentEntries) }
        }
    }

    private fun deduplicateExistingTimetableEntries(entries: List<TimetableEntry>) { viewModelScope.launch { val groups = entries.groupBy { normalizeSubjectName(it.subjectName) + it.dayOfWeek + it.startTime }; for ((_, group) in groups) { if (group.size > 1) { val duplicates = group.drop(1); for (dup in duplicates) { repository.deleteTimetableEntry(dup) } } } } }
    
    private fun normalizeExistingTimetableEntries() {
        viewModelScope.launch {
            val entries = repository.allTimetableEntries.first()
            entries.forEach { entry ->
                val normStart = formatTimeForStorage(entry.startTime)
                val normEnd = formatTimeForStorage(entry.endTime)
                if (normStart != entry.startTime || normEnd != entry.endTime) {
                    repository.addTimetableEntry(entry.copy(startTime = normStart, endTime = normEnd))
                }
            }
            updateWidget()
        }
    }
    
    fun getCanonicalDisplayName(subjectEntries: List<TimetableEntry>): String { val names = subjectEntries.map { it.subjectName.trim() }; return names.maxByOrNull { it.length } ?: names.firstOrNull() ?: "Unknown Subject" }
    fun normalizeSubjectName(rawName: String): String { return rawName.trim().lowercase(Locale.getDefault()).replace(".", "").replace(",", "").replace(" and ", " & ").replace("\\s+".toRegex(), " ").trim() }
    fun getInstallationTimestamp(): Long {
        val prefs = getApplication<Application>().getSharedPreferences("campus_companion_prefs", Context.MODE_PRIVATE)
        var ts = prefs.getLong("install_timestamp", 0L)
        if (ts == 0L) {
            ts = System.currentTimeMillis()
            prefs.edit { putLong("install_timestamp", ts) }
        }
        return ts
    }
    fun parseTimeToMinutes(timeStr: String): Int { return try { val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault()); val date = sdf.parse(timeStr.trim()); if (date != null) { val c = Calendar.getInstance(); c.time = date; c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE) } else 0 } catch (e: Exception) { try { val parts = timeStr.trim().split(":"); parts[0].toInt() * 60 + parts[1].substring(0, 2).toInt() } catch (e2: Exception) { 0 } } }
}
