package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ScannerViewModel
import com.example.util.ExportUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(viewModel: ScannerViewModel, onNavigateToEdit: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val scannedPages by viewModel.scannedPages.collectAsStateWithLifecycle()
    val isOcrProcessing by viewModel.isOcrProcessing.collectAsStateWithLifecycle()
    val isGeneratingPdf by viewModel.isGeneratingPdf.collectAsStateWithLifecycle()
    val ocrOutputText by viewModel.ocrOutputText.collectAsStateWithLifecycle()
    val savedDocs by viewModel.savedDocuments.collectAsStateWithLifecycle()
    
    var docTitle by remember { mutableStateOf("") }
    var docCategory by remember { mutableStateOf("Notes") }
    var showSaveDialog by remember { mutableStateOf(false) }
    
    val pagerState = rememberPagerState(pageCount = { scannedPages.size })

    // Camera Launcher
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                viewModel.addPagesFromUris(listOf(uri))
            }
        }
    }

    // Gallery Launcher (Multiple)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addPagesFromUris(uris)
        }
    }

    val filters = listOf("ORIGINAL", "MAGIC_COLOR", "B&W", "HIGH_CONTRAST")

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { 
                TopAppBar(
                    title = { 
                        Text(
                            text = "AI Scanner Lab", 
                            fontWeight = FontWeight.ExtraBold, 
                            fontSize = 20.sp 
                        ) 
                    },
                    actions = {
                        IconButton(onClick = { 
                            tempCameraUri = ExportUtils.getCameraTempUri(context)
                            cameraLauncher.launch(tempCameraUri!!)
                        }) { Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Camera", tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { 
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                        }) { Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.primary) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    windowInsets = WindowInsets(top = 28.dp)
                ) 
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp), 
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(28.dp), 
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (scannedPages.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(28.dp)).background(Color.Black)) {
                                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { idx -> 
                                        Image(bitmap = scannedPages[idx].asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit) 
                                    }
                                    
                                    // Glassy Overlays
                                    Row(Modifier.align(Alignment.TopEnd).padding(16.dp), Arrangement.spacedBy(10.dp)) {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.5f), 
                                            shape = CircleShape,
                                            modifier = Modifier.size(44.dp),
                                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                                        ) { 
                                            IconButton(onClick = { 
                                                viewModel.startEditing(pagerState.currentPage)
                                                onNavigateToEdit()
                                            }) { Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(20.dp)) } 
                                        }
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.5f), 
                                            shape = CircleShape,
                                            modifier = Modifier.size(44.dp),
                                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                                        ) { 
                                            IconButton(onClick = { viewModel.removePage(pagerState.currentPage) }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp)) } 
                                        }
                                    }
                                    
                                    Surface(
                                        Modifier.align(Alignment.BottomCenter).padding(16.dp), 
                                        RoundedCornerShape(16.dp), 
                                        Color.Black.copy(alpha = 0.5f)
                                    ) { 
                                        Text(
                                            text = "${pagerState.currentPage + 1} OF ${scannedPages.size}", 
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), 
                                            color = Color.White, 
                                            fontSize = 11.sp, 
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        ) 
                                    }
                                }
                                Spacer(Modifier.height(20.dp))
                                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                                    filters.forEach { f -> 
                                        Surface(
                                            modifier = Modifier.weight(1f).clickable { viewModel.setFilter(f, pagerState.currentPage) }, 
                                            shape = RoundedCornerShape(12.dp), 
                                            color = MaterialTheme.colorScheme.surface, 
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        ) { 
                                            Text(
                                                text = f.replace("_", " "), 
                                                modifier = Modifier.padding(vertical = 10.dp), 
                                                textAlign = TextAlign.Center, 
                                                fontSize = 9.sp, 
                                                fontWeight = FontWeight.Black, 
                                                color = MaterialTheme.colorScheme.primary
                                            ) 
                                        } 
                                    }
                                }
                                Spacer(Modifier.height(20.dp))
                                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                                    Button(onClick = { viewModel.runOcr(pagerState.currentPage) }, Modifier.weight(1f), enabled = !isGeneratingPdf) { Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("AI OCR", fontSize = 12.sp) }
                                    Button(onClick = { 
                                        viewModel.setGeneratingPdf(true)
                                        scope.launch { 
                                            val f = ExportUtils.createPdfFromBitmaps(context, "Scan", scannedPages, isPersistent = false)
                                            if (f != null) { 
                                                ExportUtils.shareFile(context, f, "application/pdf")
                                                // AUTOMATICALLY CLEAR ALL AFTER SHARING
                                                viewModel.clearAll(context)
                                            } 
                                            viewModel.setGeneratingPdf(false)
                                        } 
                                    }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), enabled = !isGeneratingPdf) { Icon(Icons.Default.PictureAsPdf, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("PDF", fontSize = 12.sp) }
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { showSaveDialog = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(vertical = 12.dp), // Taller
                                        enabled = !isGeneratingPdf
                                    ) { 
                                        Text("Save Library", fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1) 
                                    }
                                    
                                    OutlinedButton(
                                        onClick = { viewModel.clearAll(context) }, 
                                        modifier = Modifier.weight(1f), // EQUAL WEIGHT
                                        enabled = !isGeneratingPdf,
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(vertical = 12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) { 
                                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1) 
                                    }
                                }
                            } else {
                                Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(80.dp)) { Icon(Icons.Default.DocumentScanner, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(20.dp)) }
                                    Spacer(Modifier.height(16.dp))
                                    Text("Ready to Scan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(24.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Button(onClick = { 
                                            tempCameraUri = ExportUtils.getCameraTempUri(context)
                                            cameraLauncher.launch(tempCameraUri!!)
                                        }) { Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Camera") }
                                        Button(onClick = { 
                                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha=2.0f))) { Icon(Icons.Default.PhotoLibrary, null); Spacer(Modifier.width(8.dp)); Text("Gallery") }
                                    }
                                }
                            }
                        }
                    }
                }
                if (ocrOutputText.isNotBlank()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("AI INTELLIGENCE RESULT", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                                    Row {
                                        IconButton(onClick = { 
                                            scope.launch {
                                                val clipData = android.content.ClipData.newPlainText("OCR", ocrOutputText)
                                                clipboard.setClipEntry(androidx.compose.ui.platform.ClipEntry(clipData))
                                            }
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = {
                                            val f = ExportUtils.createTextFile(context, "OCR", ocrOutputText)
                                            if (f != null) ExportUtils.shareFile(context, f, "text/plain")
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Share, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(ocrOutputText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp)
                            }
                        }
                    }
                }
                item { 
                    Text(
                        "Digital Archives", 
                        fontWeight = FontWeight.ExtraBold, 
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    ) 
                }
                if (savedDocs.isEmpty()) { 
                    item { 
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        ) {
                            Text("No saved scans in your library.", modifier = Modifier.padding(24.dp), fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center) 
                        }
                    } 
                }
                else { 
                    items(savedDocs) { doc -> 
                        Surface(
                            modifier = Modifier.fillMaxWidth(), 
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) { 
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { 
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) { 
                                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp)) 
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) { 
                                    Text(doc.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) 
                                    Text("${doc.category} • ${doc.pageCount} Pages", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                                }
                                Row {
                                    IconButton(onClick = { viewModel.shareSavedDocument(context, doc) }) { 
                                        Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) 
                                    }
                                    IconButton(onClick = { viewModel.deleteDocument(doc.id) }) { 
                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(22.dp)) 
                                    } 
                                }
                            } 
                        } 
                    } 
                }
            }
        }
        if (isGeneratingPdf || isOcrProcessing) { 
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .pointerInput(Unit) { /* Block all touches */ }, 
                contentAlignment = Alignment.Center
            ) { 
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) { 
                    Column(
                        Modifier.padding(32.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) { 
                        CircularProgressIndicator(strokeWidth = 4.dp)
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = if (isGeneratingPdf) "Processing Document..." else "AI OCR in Progress...", 
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Please wait, this may take a moment", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } 
                } 
            } 
        }
    }
    if (showSaveDialog) { 
        AlertDialog(
            onDismissRequest = { showSaveDialog = false }, 
            title = { Text("Save to Library", fontWeight = FontWeight.Black) }, 
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { 
                    OutlinedTextField(value = docTitle, onValueChange = { docTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = docCategory, onValueChange = { docCategory = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth()) 
                } 
            }, 
            confirmButton = { Button(onClick = { viewModel.saveDocument(context, docTitle, docCategory); showSaveDialog = false }) { Text("Save") } }, 
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") } },
            shape = RoundedCornerShape(28.dp)
        ) 
    }
}
