package com.example.util

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.core.content.edit
import com.example.data.TimetableEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AutoSilentManager {

    enum class SoundMode {
        SILENT, VIBRATE, NORMAL
    }

    data class ActiveClassState(
        val isClassActive: Boolean,
        val currentClass: TimetableEntry? = null,
        val nextClass: TimetableEntry? = null,
        val minutesRemaining: Int = 0,
        val soundModeSet: SoundMode = SoundMode.NORMAL,
        val isInitialized: Boolean = true,
        val isConfirmedManually: Boolean = false,
        val expectedTotalChecks: Int = 1
    )

    fun getCurrentDayName(): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
    }

    fun getCurrentTime24h(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
    }

    /**
     * Checks if current time is within class start & end time
     */
    fun checkActiveClasses(
        timetableEntries: List<TimetableEntry>,
        forcedCurrentDay: String? = null,
        forcedCurrentTime: String? = null
    ): ActiveClassState {
        val today = forcedCurrentDay ?: getCurrentDayName()
        val currentTimeStr = forcedCurrentTime ?: getCurrentTime24h()

        val todayClasses = timetableEntries.asSequence()
            .filter { it.dayOfWeek.equals(today, ignoreCase = true) }
            .toList()
            .sortedBy { it.startTime }

        var activeClass: TimetableEntry? = null
        var nextClass: TimetableEntry? = null
        var remainingMins = 0

        val currentMins = timeStringToMinutes(currentTimeStr)

        for (entry in todayClasses) {
            val startMins = timeStringToMinutes(entry.startTime)
            val endMins = timeStringToMinutes(entry.endTime)

            // A class is active if current time is >= start AND <= end.
            // This ensures that the final check at the exact end minute (e.g. 10:50) is recorded.
            if (currentMins in startMins..endMins) {
                activeClass = entry
                remainingMins = endMins - currentMins
                break
            } else if (startMins >= currentMins && nextClass == null) {
                nextClass = entry
            }
        }

        val soundMode = if (activeClass != null && activeClass.autoSilentEnabled) {
            SoundMode.SILENT
        } else {
            SoundMode.NORMAL
        }

        var expectedChecks = 1
        activeClass?.let {
            val duration = timeStringToMinutes(it.endTime) - timeStringToMinutes(it.startTime)
            expectedChecks = if (duration > 0) (duration / 10) + 1 else 1
        }

        return ActiveClassState(
            isClassActive = activeClass != null,
            currentClass = activeClass,
            nextClass = nextClass,
            minutesRemaining = remainingMins,
            soundModeSet = soundMode,
            expectedTotalChecks = expectedChecks
        )
    }

    fun timeStringToMinutes(timeStr: String): Int {
        return try {
            val clean = timeStr.trim().uppercase()
            val isPm = clean.contains("PM")
            val isAm = clean.contains("AM")
            val digitsOnly = clean.replace("AM", "").replace("PM", "").trim()
            val parts = digitsOnly.split(":")
            var h = parts[0].trim().toInt()
            val m = if (parts.size > 1) parts[1].trim().toInt() else 0

            if (isPm && h < 12) {
                h += 12
            } else if (isAm && h == 12) {
                h = 0
            }
            h * 60 + m
        } catch (e: Exception) {
            0
        }
    }

    fun hasDndPermission(context: Context): Boolean {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.isNotificationPolicyAccessGranted
        } catch (e: Exception) {
            false
        }
    }

    fun openDndSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    }

    /**
     * Applies sound mode ONCE when entering a class session or exiting a class session.
     * Prevents overriding user's manual sound toggle when they re-open the app during class.
     */
    fun applySoundModeForClass(context: Context, state: ActiveClassState, force: Boolean = false): Boolean {
        val prefs = context.getSharedPreferences("campus_companion_prefs", Context.MODE_PRIVATE)
        val lastAppliedClassId = prefs.getLong("last_applied_class_id", -1L)
        val currentClassId = if (state.isClassActive) state.currentClass?.id ?: -2L else -1L

        if (lastAppliedClassId == currentClassId && !force) {
            android.util.Log.d("CampusPulse", "[Sound] Class state $currentClassId already handled. Skipping.")
            return true
        }

        if (state.isClassActive && state.currentClass != null) {
            android.util.Log.d("CampusPulse", "[Sound] Class START: ${state.currentClass.subjectName}. Applying mode.")
            val success = applySoundMode(context, state.soundModeSet, force = force)
            if (success) {
                prefs.edit { putLong("last_applied_class_id", currentClassId) }
            }
            return success
        } else {
            // Class is NOT active. Ensure we return to normal if we were in a class previously.
            android.util.Log.d("CampusPulse", "[Sound] Class END or NONE: Returning to NORMAL.")
            val success = applySoundMode(context, SoundMode.NORMAL, force = force)
            if (success || force) {
                prefs.edit { putLong("last_applied_class_id", -1L) }
            }
            return success
        }
    }

    /**
     * Attempts to set the device sound mode via AudioManager
     */
    fun applySoundMode(context: Context, mode: SoundMode, force: Boolean = false): Boolean {
        android.util.Log.d("CampusPulse", "[Sound] Requesting mode $mode (force=$force)")
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentRinger = audioManager.ringerMode
            val prefs = context.getSharedPreferences("campus_companion_prefs", Context.MODE_PRIVATE)
            val wasAutoSilenced = prefs.getBoolean("was_auto_silenced", false)

            val targetRinger = when (mode) {
                SoundMode.SILENT -> if (hasDndPermission(context)) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_VIBRATE
                SoundMode.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
                SoundMode.NORMAL -> AudioManager.RINGER_MODE_NORMAL
            }

            // CONSERVATIVE LOGIC: 
            // 1. If we are trying to go to SILENT/VIBRATE, but the phone is already in VIBRATE or SILENT, 
            //    then skip. This respects manual silence and stops "Silent -> Silent" notifications.
            if ((mode == SoundMode.SILENT || mode == SoundMode.VIBRATE) && 
                (currentRinger == AudioManager.RINGER_MODE_VIBRATE || currentRinger == AudioManager.RINGER_MODE_SILENT)) {
                android.util.Log.d("CampusPulse", "[Sound] Skipped SILENCE: Phone already non-normal ($currentRinger)")
                return true
            }

            // 2. If we are trying to go to NORMAL, ONLY do it if the app was the one that silenced it.
            //    This ensures we don't turn off a user's manual silent mode.
            if (mode == SoundMode.NORMAL && !wasAutoSilenced && !force) {
                android.util.Log.d("CampusPulse", "[Sound] Skipped NORMAL: Not auto-silenced previously.")
                return true
            }

            // 3. Prevent immediate actions on app startup if they might be flickering
            // (Handled by the 1s delay in ViewModel, but we can be extra safe here)

            if (!force && currentRinger == targetRinger) {
                android.util.Log.d("CampusPulse", "[Sound] Skipped: Mode already matches ($currentRinger)")
                return true
            }

            android.util.Log.d("CampusPulse", "[Sound] Executing update: $currentRinger -> $targetRinger")
            when (mode) {
                SoundMode.SILENT -> {
                    audioManager.ringerMode = targetRinger
                    if (!wasAutoSilenced) prefs.edit { putBoolean("was_auto_silenced", true) }
                }
                SoundMode.VIBRATE -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    if (!wasAutoSilenced) prefs.edit { putBoolean("was_auto_silenced", true) }
                }
                SoundMode.NORMAL -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    prefs.edit { putBoolean("was_auto_silenced", false) }
                }
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("CampusPulse", "[Sound] Error: ${e.message}")
            false
        }
    }
}
