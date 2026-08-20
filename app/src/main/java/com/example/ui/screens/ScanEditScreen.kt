package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ScannerViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanEditScreen(viewModel: ScannerViewModel, onNavigateBack: () -> Unit) {
    val editingIndex by viewModel.editingPageIndex.collectAsStateWithLifecycle()
    val pages by viewModel.scannedPages.collectAsStateWithLifecycle()
    
    if (editingIndex !in pages.indices) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

    val bitmap = pages[editingIndex]
    var cropRect by remember { mutableStateOf(Rect(0.05f, 0.05f, 0.95f, 0.95f)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crop & Rotate", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { 
                        viewModel.stopEditing()
                        onNavigateBack() 
                    }) {
                        Icon(Icons.Default.Close, "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.cropPage(editingIndex, cropRect.left, cropRect.top, cropRect.right, cropRect.bottom)
                        viewModel.stopEditing()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Check, "Apply", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Accessing constraints explicitly to satisfy potential lint/compiler requirements
                val maxWidthDp = this.maxWidth
                val maxHeightDp = this.maxHeight
                
                val cw = maxWidthDp.value
                val ch = maxHeightDp.value
                
                val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val containerRatio = cw / ch
                
                val dw: Float
                val dh: Float
                
                if (bitmapRatio > containerRatio) {
                    dw = cw
                    dh = cw / bitmapRatio
                } else {
                    dh = ch
                    dw = ch * bitmapRatio
                }

                Box(modifier = Modifier.size(dw.dp, dh.dp)) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    
                                    val dx = dragAmount.x / size.width
                                    val dy = dragAmount.y / size.height
                                    val tx = change.position.x / size.width
                                    val ty = change.position.y / size.height
                                    
                                    val h = 0.15f
                                    val isL = abs(tx - cropRect.left) < h
                                    val isR = abs(tx - cropRect.right) < h
                                    val isT = abs(ty - cropRect.top) < h
                                    val isB = abs(ty - cropRect.bottom) < h
                                    
                                    var nl = cropRect.left
                                    var nt = cropRect.top
                                    var nr = cropRect.right
                                    var nb = cropRect.bottom
                                    
                                    if (isL) nl = (nl + dx).coerceIn(0f, nr - 0.1f)
                                    if (isR) nr = (nr + dx).coerceIn(nl + 0.1f, 1f)
                                    if (isT) nt = (nt + dy).coerceIn(0f, nb - 0.1f)
                                    if (isB) nb = (nb + dy).coerceIn(nt + 0.1f, 1f)
                                    
                                    cropRect = Rect(nl, nt, nr, nb)
                                }
                            }
                    ) {
                        val r = Rect(
                            offset = Offset(cropRect.left * size.width, cropRect.top * size.height),
                            size = Size((cropRect.right - cropRect.left) * size.width, (cropRect.bottom - cropRect.top) * size.height)
                        )
                        
                        // White crop border
                        drawRect(
                            color = Color.White,
                            topLeft = r.topLeft,
                            size = r.size,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        
                        // Corner points
                        val rad = 8.dp.toPx()
                        drawCircle(Color.White, rad, r.topLeft)
                        drawCircle(Color.White, rad, Offset(r.right, r.top))
                        drawCircle(Color.White, rad, Offset(r.left, r.bottom))
                        drawCircle(Color.White, rad, r.bottomRight)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("PRECISION SCAN", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Fine-tune boundary detection", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = { viewModel.rotatePage(editingIndex) },
                            modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, "Rotate", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
