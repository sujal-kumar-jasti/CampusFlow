package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.widget.LiveClassWidgetProvider
import com.example.widget.ShortcutsWidgetProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import androidx.core.content.edit
import kotlin.time.Duration.Companion.milliseconds

class TimetableBackgroundWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(appContext)
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val todayStr = sdf.format(java.util.Date())

            val dayName = AutoSilentManager.getCurrentDayName()
            val entries = db.timetableDao().getEntriesListForDay(dayName)

            if (IIESTAcademicCalendar.isNoClassDay(todayStr)) {
                android.util.Log.d("CampusPulse", "[Worker] Holiday/Suspended day. Auto-marking periods as HOLIDAY.")
                // Auto-mark all classes today as HOLIDAY
                entries.forEach { entry ->
                    val existing = db.periodAttendanceDao().getRecordForPeriodAndDate(entry.id, todayStr)
                    if (existing == null || (!existing.isConfirmedManually && existing.status != "HOLIDAY")) {
                        db.periodAttendanceDao().insertPeriodRecord(
                            com.example.data.PeriodAttendance(
                                id = existing?.id ?: 0,
                                timetableEntryId = entry.id,
                                date = todayStr,
                                subjectName = entry.subjectName,
                                periodTime = "${entry.startTime} - ${entry.endTime}",
                                status = "HOLIDAY",
                                isConfirmedManually = false
                            )
                        )
                    }
                }
                return Result.success()
            }

            val activeState = AutoSilentManager.checkActiveClasses(entries)
            
            android.util.Log.d("CampusPulse", "[Worker] Starting background check. Class active: ${activeState.isClassActive}")

            // Sound mode is primarily handled by the Receiver for speed, 
            // but the Worker acts as a backup/sync.
            AutoSilentManager.applySoundModeForClass(appContext, activeState)

            if (activeState.isClassActive) {
                // High-precision location fetch
                fetchBackgroundGpsLocation(appContext, db, activeState.currentClass)
            }

            LiveClassWidgetProvider.updateAllWidgets(appContext)
            ShortcutsWidgetProvider.updateAllWidgets(appContext)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun fetchBackgroundGpsLocation(context: Context, db: AppDatabase, activeClass: com.example.data.TimetableEntry?) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        android.util.Log.d("CampusPulse", "[Worker] Location check started. GPS Enabled: $isGpsEnabled")

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            
            // STRICT HIGH PRECISION FETCH: 
            // We bypass lastLocation as it's often stale when moving.
            // We request a fresh HIGH_ACCURACY fix.
            val cts = CancellationTokenSource()
            var loc: android.location.Location? = try {
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
            } catch (e: Exception) { 
                android.util.Log.w("CampusPulse", "[Worker] High accuracy initial attempt failed.")
                null
            }

            // If initial fix is missing, poor accuracy (> 50m), or not from GPS provider:
            // Wait 5 seconds for GPS to converge and retry once.
            if (loc == null || loc.accuracy > 50) {
                android.util.Log.d("CampusPulse", "[Worker] Location inaccurate (${loc?.accuracy}m). Waiting for GPS lock...")
                kotlinx.coroutines.delay(5000.milliseconds) 
                loc = try {
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
                } catch (e: Exception) { loc } // Keep previous if retry fails
            }

            if (loc != null) {
                val prefs: SharedPreferences = context.getSharedPreferences("campus_companion_prefs", Context.MODE_PRIVATE)
                
                // SAVE CURRENT FOR DASHBOARD SYNC - Use bits for high precision
                prefs.edit {
                    putLong(
                        "last_user_lat_bits",
                        java.lang.Double.doubleToRawLongBits(loc.latitude)
                    )
                        .putLong(
                            "last_user_lon_bits",
                            java.lang.Double.doubleToRawLongBits(loc.longitude)
                        )
                        .putLong("last_location_time", loc.time)
                }

                // PRIORITIZE USER SETTINGS from SharedPreferences - Load with high precision
                val defaultLoc = GpsLocationHelper.CampusLocation()
                
                val savedCampusLat = if (prefs.contains("campus_lat_bits")) {
                    java.lang.Double.longBitsToDouble(prefs.getLong("campus_lat_bits", 0L))
                } else defaultLoc.latitude

                val savedCampusLon = if (prefs.contains("campus_lon_bits")) {
                    java.lang.Double.longBitsToDouble(prefs.getLong("campus_lon_bits", 0L))
                } else defaultLoc.longitude

                val radius = if (prefs.contains("campus_radius_bits")) {
                    java.lang.Double.longBitsToDouble(prefs.getLong("campus_radius_bits", 0L))
                } else defaultLoc.radiusMeters

                val dist = GpsLocationHelper.calculateDistanceMeters(
                    loc.latitude, loc.longitude, savedCampusLat, savedCampusLon
                )
                val isInside = dist <= radius
                
                android.util.Log.d("CampusPulse", "[Worker] Fast Fix: Dist=${dist}m (Target: ${radius}m), Inside=${isInside}, Loc=${loc.latitude},${loc.longitude}")
                prefs.edit { putBoolean("is_inside_campus", isInside) }

                if (activeClass != null) {
                    recordPeriodAttendance(db, activeClass, isInside, loc.latitude, loc.longitude, dist)
                }
            } else {
                android.util.Log.w("CampusPulse", "[Worker] Failed to acquire GPS fix in background.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun recordPeriodAttendance(
        db: AppDatabase,
        currentClass: com.example.data.TimetableEntry,
        isInside: Boolean,
        lat: Double,
        lon: Double,
        dist: Double
    ) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val dateStr = sdf.format(java.util.Date())
        
        val startMins = AutoSilentManager.timeStringToMinutes(currentClass.startTime)
        val endMins = AutoSilentManager.timeStringToMinutes(currentClass.endTime)
        val cal = Calendar.getInstance()
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        
        // BUCKET LOGIC:
        val elapsed = currentMins - startMins
        if (elapsed < -2) return // Too early
        
        val bucketInterval = (elapsed / 10) * 10
        // Allow a 5-minute buffer after the class ends to record the final bucket.
        // This ensures that if the 10:50 alarm triggers slightly late, it's still captured.
        if (bucketInterval > (endMins - startMins) + 5) return // Past class end

        val dao = db.periodAttendanceDao()
        val existing = dao.getRecordForPeriodAndDate(currentClass.id, dateStr)
        
        if (existing?.isConfirmedManually == true) return

        // Check if this specific 10-minute bucket already has a record
        if (existing != null && db.periodGpsCheckDao().checkExists(existing.id, bucketInterval)) {
            android.util.Log.d("CampusPulse", "[Worker] Bucket $bucketInterval already recorded. Skipping duplicate.")
            return
        }

        val duration = endMins - startMins
        val expectedTotal = if (duration > 0) (duration / 10) + 1 else 1

        val tempRecord = com.example.data.PeriodAttendance(
            id = existing?.id ?: 0,
            timetableEntryId = currentClass.id,
            date = dateStr,
            subjectName = currentClass.subjectName,
            periodTime = "${currentClass.startTime} - ${currentClass.endTime}",
            status = "PENDING",
            checksInside = (existing?.checksInside ?: 0),
            totalChecks = expectedTotal,
            isConfirmedManually = false,
            timestamp = System.currentTimeMillis()
        )
        val attendanceId = dao.insertPeriodRecord(tempRecord)
        
        val timeSdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        db.periodGpsCheckDao().insertCheck(
            com.example.data.PeriodGpsCheck(
                periodAttendanceId = attendanceId,
                intervalMinute = bucketInterval,
                checkTime = timeSdf.format(java.util.Date()),
                isInside = isInside,
                latitude = lat,
                longitude = lon,
                distance = dist
            )
        )

        val finalInside = db.periodGpsCheckDao().getInsideChecksCount(attendanceId)
        val isPresent = finalInside >= (expectedTotal + 1) / 2
        
        val isClassEnding = currentMins >= (endMins - 5) // Last 5 mins of class or later

        val status = if (isPresent) "PRESENT" else if (isClassEnding) "PENDING" else "PENDING"
        
        dao.insertPeriodRecord(
            tempRecord.copy(
                id = attendanceId,
                checksInside = finalInside,
                totalChecks = expectedTotal,
                status = status
            )
        )

        if (isClassEnding && !isPresent && !tempRecord.isConfirmedManually) {
            // Class is ending and user is not marked PRESENT automatically.
            // Trigger clarification notification.
            NotificationHelper.showAttendanceClarification(
                appContext,
                attendanceId,
                currentClass.subjectName,
                "${currentClass.startTime} - ${currentClass.endTime}"
            )
        }

        android.util.Log.d("CampusPulse", "[Worker] Recorded bucket $bucketInterval. Session: $finalInside/$expectedTotal")
    }
}
