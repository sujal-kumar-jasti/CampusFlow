package com.example.service

import android.graphics.Bitmap
import android.util.Base64
import androidx.core.graphics.scale
import com.example.BuildConfig
import com.example.data.TimetableEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiApiService {

    private const val PRIMARY_MODEL_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"
    private const val FALLBACK_MODEL_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        val maxDim = 2048
        val ratio = minOf(1.0f, maxDim.toFloat() / maxOf(bitmap.width, bitmap.height))
        val scaled = if (ratio < 1.0f) {
            bitmap.scale((bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun parseTimetableFromImage(bitmap: Bitmap): Result<List<TimetableEntry>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(Exception("Gemini API Key is missing. Please configure it in AI Studio Secrets panel."))
        }

        val prompt = """
            Analyze this class timetable schedule image, document, or table carefully.
            Extract all recurring class periods.
            
            GRID LAYOUT & ACCURACY RULES:
            1. STEP-BY-STEP GRID READING: First identify whether Days (Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday) are listed as ROWS (horizontal rows) or COLUMNS (vertical columns).
            2. STRICT DAY BOUNDARIES: Double check each class cell against its EXACT corresponding Day row or column header. DO NOT bleed or mix up classes across adjacent days (for example, DO NOT put Tuesday classes into Monday or Wednesday). Verify every single cell's day header before adding it.
            3. ELECTIVES & MULTI-BATCHES: If multiple elective groups or batches (e.g. "HX" and "HY", "Batch A" and "Batch B", or "Lab 1 / Lab 2") share the exact same time slot on the same day, COMBINE them into a single subjectName (e.g. "HX / HY Elective" or "Data Structures (Batch A / B)"). Do NOT generate duplicate overlapping entries.
            4. CONTINUOUS / MULTI-HOUR CLASSES: If a single class, lab, or workshop spans continuous hours or multiple consecutive time slots on the same day (e.g., 09:55 AM to 12:40 PM), extract it as ONE SINGLE entry with startTime "09:55" and endTime "12:40".
            
            For each period extract:
            - subjectName: Full subject name or combined elective names (e.g. "Physics Lab", "CS101", "HX / HY Elective")
            - roomNumber: Room/Lab/Hall identifier if printed, else ""
            - dayOfWeek: Full day name strictly matching that row/column ("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            - startTime: Start time in 24-hour format HH:mm (e.g. "09:55", "13:30")
            - endTime: End time in 24-hour format HH:mm (e.g. "12:40", "15:00")
            - instructor: Faculty or professor name if printed, else ""

            Return ONLY a valid JSON Array of objects with these exact keys. No markdown formatting, code block backticks, or commentary.
        """.trimIndent()

        val base64Image = bitmapToBase64(bitmap)

        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        try {
            val responseBody = executeGeminiRequest(jsonPayload.toString(), apiKey)
            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

            val parsedEntries = parseTimetableJsonArray(rawText)
            Result.success(parsedEntries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executeGeminiRequest(jsonString: String, apiKey: String): String {
        val request1 = Request.Builder()
            .url("$PRIMARY_MODEL_URL?key=$apiKey")
            .post(jsonString.toRequestBody("application/json".toMediaType()))
            .build()

        val response1 = okHttpClient.newCall(request1).execute()
        val body1 = response1.body.string()

        if (response1.isSuccessful && body1.isNotBlank()) {
            return body1
        }

        val request2 = Request.Builder()
            .url("$FALLBACK_MODEL_URL?key=$apiKey")
            .post(jsonString.toRequestBody("application/json".toMediaType()))
            .build()

        val response2 = okHttpClient.newCall(request2).execute()
        val body2 = response2.body.string()

        if (response2.isSuccessful && body2.isNotBlank()) {
            return body2
        }

        if (response1.code == 429 || response1.code == 503 || response2.code == 429 || response2.code == 503) {
            throw Exception("Gemini AI model is currently busy due to high demand. Please retry in a few seconds.")
        }

        throw Exception("Gemini API Request failed (${response1.code}/${response2.code}): ${body1.ifBlank { body2 }}")
    }

    private fun formatTo24Hr(timeStr: String): String {
        var t = timeStr.trim().uppercase()
        if (t.isEmpty()) return "09:00"
        val isPm = t.contains("PM")
        val isAm = t.contains("AM")
        t = t.replace("AM", "").replace("PM", "").trim()
        val parts = t.split(":")
        if (parts.size >= 2) {
            var hour = parts[0].toIntOrNull() ?: 9
            val minuteDigits = parts[1].filter { it.isDigit() }
            val minute = if (minuteDigits.length >= 2) minuteDigits.take(2) else minuteDigits.padStart(2, '0')
            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0
            if (!isPm && !isAm && hour in 1..7) hour += 12
            return "%02d:%s".format(hour, minute.ifEmpty { "00" })
        }
        return timeStr
    }

    private fun parseTimetableJsonArray(jsonStr: String): List<TimetableEntry> {
        val cleanJson = jsonStr.replace("```json", "").replace("```", "").trim()
        val list = mutableListOf<TimetableEntry>()
        val jsonArray = JSONArray(cleanJson)

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val subject = obj.optString("subjectName", "Class Period")
            val room = obj.optString("roomNumber", "")
            val day = obj.optString("dayOfWeek", "Monday").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val start = formatTo24Hr(obj.optString("startTime", "09:00"))
            val end = formatTo24Hr(obj.optString("endTime", "10:00"))
            val instructor = obj.optString("instructor", "")

            val newEntry = TimetableEntry(
                subjectName = subject,
                roomNumber = room,
                dayOfWeek = day,
                startTime = start,
                endTime = end,
                instructor = instructor,
                autoSilentEnabled = true
            )

            val existingIndex = list.indexOfFirst {
                it.dayOfWeek.equals(day, ignoreCase = true) &&
                ((it.startTime == start && it.endTime == end) ||
                 (it.startTime < end && start < it.endTime))
            }

            if (existingIndex >= 0) {
                val existing = list[existingIndex]
                if (!existing.subjectName.contains(subject, ignoreCase = true)) {
                    val combinedSubject = "${existing.subjectName} / $subject"
                    list[existingIndex] = existing.copy(subjectName = combinedSubject)
                }
            } else {
                list.add(newEntry)
            }
        }
        return list
    }

    suspend fun performOcrAndAnalyze(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(Exception("Gemini API Key is missing. Please configure it in AI Studio Secrets panel."))
        }

        val prompt = """
            Perform OCR text extraction on this scanned document.
            1. Extract all text accurately maintaining structure and headings.
            2. Add a brief "Key Summary" section at the beginning with 3 key bullet points.
            3. Followed by the complete extracted document text.
        """.trimIndent()

        val base64Image = bitmapToBase64(bitmap)

        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
        }

        try {
            val responseBody = executeGeminiRequest(jsonPayload.toString(), apiKey)
            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: "No text recognized."

            Result.success(rawText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
