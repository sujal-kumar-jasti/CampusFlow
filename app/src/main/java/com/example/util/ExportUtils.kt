package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.ui.MainViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    fun getCameraTempUri(context: Context): Uri {
        val cacheDir = File(context.cacheDir, "camera")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val file = File(cacheDir, "temp_camera_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun createPdfFromBitmaps(context: Context, fileName: String, bitmaps: List<Bitmap>, isPersistent: Boolean = false): File? {
        if (bitmaps.isEmpty()) return null
        val pdfDocument = PdfDocument()
        
        try {
            bitmaps.forEachIndexed { index, bitmap ->
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
            }

            val dir = if (isPersistent) {
                val d = File(context.filesDir, "documents")
                if (!d.exists()) d.mkdirs()
                d
            } else {
                val d = File(context.cacheDir, "temp_pdf")
                if (!d.exists()) d.mkdirs()
                d
            }

            val file = File(dir, "$fileName.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Document"))
    }

    fun deleteFile(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists()) file.delete() else true
        } catch (_: Exception) {
            false
        }
    }

    fun clearTempCache(context: Context) {
        try {
            // Clear temp PDFs
            val tempPdfDir = File(context.cacheDir, "temp_pdf")
            if (tempPdfDir.exists()) tempPdfDir.deleteRecursively()
            
            // Clear temp camera images
            val cameraDir = File(context.cacheDir, "camera")
            if (cameraDir.exists()) cameraDir.deleteRecursively()
            
            // Clear reports
            val reportsDir = File(context.cacheDir, "reports")
            if (reportsDir.exists()) reportsDir.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createTextFile(context: Context, fileName: String, textContent: String, extension: String = "txt"): File? {
        return try {
            val fileNameWithExt = "$fileName-${System.currentTimeMillis()}.$extension"
            val file = File(context.cacheDir, fileNameWithExt)
            file.writeText(textContent)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createTimetableSubjectPdfReport(
        context: Context,
        subjectSummaries: List<MainViewModel.SubjectAttendanceSummary>,
        campusName: String,
    ): File? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 size in points (72 dpi)
        val pageHeight = 842
        
        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val headerPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
            color = Color.DKGRAY
        }
        val bodyPaint = Paint().apply {
            textSize = 10f
            color = Color.BLACK
        }

        var pageNumber = 1
        var myPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var myPage = pdfDocument.startPage(myPageInfo)
        var canvas = myPage.canvas

        canvas.drawText("Academic Attendance Report", 40f, 50f, titlePaint)
        canvas.drawText("Campus: $campusName | Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 40f, 75f, bodyPaint)
        
        canvas.drawLine(40f, 90f, pageWidth - 40f, 90f, bodyPaint)

        // Headers
        canvas.drawText("Subject Name", 40f, 110f, headerPaint)
        canvas.drawText("Cond.", 240f, 110f, headerPaint)
        canvas.drawText("Attd.", 320f, 110f, headerPaint)
        canvas.drawText("Rate (%)", 400f, 110f, headerPaint)
        canvas.drawText("Status", 500f, 110f, headerPaint)

        var yPos = 140f

        subjectSummaries.forEach { summary ->
            if (yPos > (pageHeight - 50)) {
                pdfDocument.finishPage(myPage)
                pageNumber++
                myPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                myPage = pdfDocument.startPage(myPageInfo)
                canvas = myPage.canvas
                yPos = 50f
            }

            canvas.drawText(summary.subjectName.take(30), 40f, yPos, bodyPaint)
            canvas.drawText(summary.totalConducted.toString(), 240f, yPos, bodyPaint)
            canvas.drawText(summary.attended.toString(), 320f, yPos, bodyPaint)
            canvas.drawText(String.format(Locale.getDefault(), "%.1f%%", summary.percentage), 400f, yPos, bodyPaint)
            canvas.drawText(if (summary.isSafe) "SAFE" else "SHORTAGE", 500f, yPos, bodyPaint)
            
            yPos += 25f
        }

        pdfDocument.finishPage(myPage)

        val file = try {
            val baseDir = if (campusName.contains("ARCHIVE")) File(context.filesDir, "archives") else File(context.cacheDir, "reports")
            if (!baseDir.exists()) baseDir.mkdirs()
            File(baseDir, "${campusName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (file != null) {
            try {
                pdfDocument.writeTo(FileOutputStream(file))
            } catch (e: Exception) {
                e.printStackTrace()
                pdfDocument.close()
                return null
            }
        }
        
        pdfDocument.close()
        return file
    }
}
