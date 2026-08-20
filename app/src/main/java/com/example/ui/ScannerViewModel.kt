package com.example.ui

import android.app.Application
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.ScannedDocument
import com.example.service.GeminiApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

@Suppress("DEPRECATION")
class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(AppDatabase.getDatabase(application))

    init {
        com.example.util.ExportUtils.clearTempCache(application)
    }

    data class ScanPage(val original: Bitmap, val processed: Bitmap, val filter: String = "ORIGINAL")
    
    private val _pages = MutableStateFlow<List<ScanPage>>(emptyList())
    val scannedPages: StateFlow<List<Bitmap>> = _pages.map { list -> list.map { it.processed } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf: StateFlow<Boolean> = _isGeneratingPdf.asStateFlow()

    private val _isOcrProcessing = MutableStateFlow(false)
    val isOcrProcessing: StateFlow<Boolean> = _isOcrProcessing.asStateFlow()

    private val _ocrOutputText = MutableStateFlow("")
    val ocrOutputText: StateFlow<String> = _ocrOutputText.asStateFlow()

    private val _editingPageIndex = MutableStateFlow(-1)
    val editingPageIndex: StateFlow<Int> = _editingPageIndex.asStateFlow()

    val savedDocuments: StateFlow<List<ScannedDocument>> = repository.allScannedDocuments
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addPagesFromUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val newPages = uris.mapNotNull { uri ->
                try {
                    val original = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    val downscaled = downscaleBitmap(original)
                    if (original != downscaled) original.recycle()
                    ScanPage(downscaled, downscaled)
                } catch (e: Exception) { null }
            }
            withContext(Dispatchers.Main) {
                _pages.value += newPages
            }
        }
    }

    private fun downscaleBitmap(bitmap: Bitmap, maxDimension: Int = 1920): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return bitmap.scale(newWidth, newHeight)
    }

    fun removePage(index: Int) {
        val current = _pages.value.toMutableList()
        if (index in current.indices) {
            val page = current.removeAt(index)
            page.original.recycle()
            if (page.original != page.processed) page.processed.recycle()
            _pages.value = current
        }
    }

    fun startEditing(index: Int) { _editingPageIndex.value = index }
    fun stopEditing() { _editingPageIndex.value = -1 }

    fun rotatePage(index: Int) {
        val current = _pages.value.toMutableList()
        if (index in current.indices) {
            val page = current[index]
            val matrix = Matrix().apply { postRotate(90f) }
            val newOriginal = Bitmap.createBitmap(page.original, 0, 0, page.original.width, page.original.height, matrix, true)
            val newProcessed = applyScannerFilter(newOriginal, page.filter)
            
            if (page.original != page.processed) page.processed.recycle()
            page.original.recycle()
            
            current[index] = page.copy(original = newOriginal, processed = newProcessed)
            _pages.value = current
        }
    }

    fun cropPage(index: Int, left: Float, top: Float, right: Float, bottom: Float) {
        val current = _pages.value.toMutableList()
        if (index in current.indices) {
            val page = current[index]
            val w = page.original.width
            val h = page.original.height
            
            val x = (left * w).toInt().coerceIn(0, w - 1)
            val y = (top * h).toInt().coerceIn(0, h - 1)
            val width = ((right - left) * w).toInt().coerceIn(1, w - x)
            val height = ((bottom - top) * h).toInt().coerceIn(1, h - y)
            
            try {
                val newOriginal = Bitmap.createBitmap(page.original, x, y, width, height)
                val newProcessed = applyScannerFilter(newOriginal, page.filter)
                
                if (page.original != page.processed) page.processed.recycle()
                page.original.recycle()
                
                current[index] = page.copy(original = newOriginal, processed = newProcessed)
                _pages.value = current
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun setFilter(filter: String, index: Int) {
        val current = _pages.value.toMutableList()
        if (index in current.indices) {
            val page = current[index]
            viewModelScope.launch {
                val newProcessed = applyScannerFilter(page.original, filter)
                if (page.processed != page.original) page.processed.recycle()
                current[index] = page.copy(processed = newProcessed, filter = filter)
                _pages.value = current
            }
        }
    }

    fun setGeneratingPdf(loading: Boolean) { _isGeneratingPdf.value = loading }

    fun runOcr(index: Int) {
        val current = _pages.value
        if (index !in current.indices) return
        viewModelScope.launch {
            _isOcrProcessing.value = true
            val res = GeminiApiService.performOcrAndAnalyze(current[index].processed)
            _ocrOutputText.value = if (res.isSuccess) res.getOrDefault("No text found.") else "OCR Error"
            _isOcrProcessing.value = false
        }
    }

    fun saveDocument(context: android.content.Context, title: String, category: String) {
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            
            val bitmaps = _pages.value.map { it.processed }
            val pdfFile = withContext(Dispatchers.IO) {
                com.example.util.ExportUtils.createPdfFromBitmaps(context, title, bitmaps, isPersistent = true)
            }
            
            repository.saveScannedDocument(ScannedDocument(
                title = title.ifBlank { "Scanned Doc ${System.currentTimeMillis() % 1000}" },
                pageCount = _pages.value.size,
                filterType = "Mixed",
                imagePath = pdfFile?.absolutePath ?: "",
                ocrText = _ocrOutputText.value,
                category = category
            ))
            
            clearAll()
            _isGeneratingPdf.value = false
        }
    }

    fun shareSavedDocument(context: android.content.Context, doc: ScannedDocument) {
        if (doc.imagePath.isNotBlank()) {
            val file = java.io.File(doc.imagePath)
            if (file.exists()) {
                com.example.util.ExportUtils.shareFile(context, file, "application/pdf")
            } else {
                android.widget.Toast.makeText(context, "File not found", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            val doc = savedDocuments.value.find { it.id == id }
            if (doc != null && doc.imagePath.isNotBlank()) {
                com.example.util.ExportUtils.deleteFile(doc.imagePath)
            }
            repository.deleteScannedDocument(id)
        }
    }

    fun clearAll(context: android.content.Context? = null) {
        _pages.value.forEach { 
            it.original.recycle()
            if (it.original != it.processed) it.processed.recycle()
        }
        _pages.value = emptyList()
        _ocrOutputText.value = ""
        _editingPageIndex.value = -1
        context?.let { com.example.util.ExportUtils.clearTempCache(it) }
    }

    private fun applyScannerFilter(original: Bitmap, filter: String): Bitmap {
        if (filter == "ORIGINAL") return original
        val result = createBitmap(original.width, original.height)
        val canvas = Canvas(result)
        val paint = Paint()
        val cm = ColorMatrix()

        when (filter) {
            "MAGIC_COLOR" -> {
                cm.setSaturation(1.4f)
                val contrast = 1.1f
                val translate = (-.5f * contrast + .5f) * 255f
                cm.postConcat(ColorMatrix(floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            "B&W" -> {
                cm.setSaturation(0f)
                val contrast = 1.6f
                val translate = (-.5f * contrast + .5f) * 255f
                cm.postConcat(ColorMatrix(floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            "HIGH_CONTRAST" -> {
                val contrast = 1.8f
                val translate = (-.5f * contrast + .5f) * 255f
                cm.set(floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            else -> return original
        }
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(original, 0f, 0f, paint)
        return result
    }
}
