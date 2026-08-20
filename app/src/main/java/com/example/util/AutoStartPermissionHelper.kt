package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.edit
import androidx.core.net.toUri

object AutoStartPermissionHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else {
            true
        }
    }

    fun requestUnrestrictedBatteryUsage(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val batteryIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(batteryIntent)
                return
            } catch (e: Throwable) {
                try {
                    val settingsIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return
                } catch (ex: Throwable) {
                    openAppDetailsSettings(context)
                }
            }
        } else {
            openAppDetailsSettings(context)
        }
    }

    fun openAutoStartSettings(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val candidateIntents = mutableListOf<Intent>()

        when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                candidateIntents.add(Intent().apply {
                    component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                })
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                candidateIntents.add(Intent().apply {
                    component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                })
                candidateIntents.add(Intent().apply {
                    component = ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
                })
            }
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                candidateIntents.add(Intent().apply {
                    component = ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")
                })
                candidateIntents.add(Intent().apply {
                    component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                })
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                candidateIntents.add(Intent().apply {
                    component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
                })
            }
            manufacturer.contains("samsung") -> {
                candidateIntents.add(Intent().apply {
                    component = ComponentName("com.samsung.android.looper", "com.samsung.android.looper.AutoRunActivity")
                })
            }
        }

        for (intent in candidateIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (context.packageManager.resolveActivity(intent, 0) != null) {
                    context.startActivity(intent)
                    return
                }
            } catch (e: Throwable) {
                // Ignore security exceptions
            }
        }

        openAppDetailsSettings(context)
    }

    fun isOemAutostartConfirmed(context: Context): Boolean {
        val prefs = context.getSharedPreferences("campus_companion_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("oem_autostart_confirmed", false)
    }

    fun setOemAutostartConfirmed(context: Context, confirmed: Boolean) {
        val prefs = context.getSharedPreferences("campus_companion_prefs", Context.MODE_PRIVATE)
        prefs.edit { putBoolean("oem_autostart_confirmed", confirmed) }
    }

    fun isBatteryOptExemptConfirmed(context: Context): Boolean {
        val prefs = context.getSharedPreferences("campus_companion_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("battery_opt_exempt_confirmed", false)
    }

    fun setBatteryOptExemptConfirmed(context: Context, confirmed: Boolean) {
        val prefs = context.getSharedPreferences("campus_companion_prefs", Context.MODE_PRIVATE)
        prefs.edit { putBoolean("battery_opt_exempt_confirmed", confirmed) }
    }

    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun openLocationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            openAppDetailsSettings(context)
        }
    }
}
