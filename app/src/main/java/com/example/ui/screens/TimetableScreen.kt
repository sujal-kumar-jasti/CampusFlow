package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TimetableEntry
import com.example.ui.MainViewModel
import com.example.util.AutoSilentManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(viewModel: MainViewModel) {
    val timetableEntries by viewModel.timetableEntries.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val activeClassState by viewModel.activeClassState.collectAsStateWithLifecycle()
    val isParsing by viewModel.isParsingTimetable.collectAsStateWithLifecycle()
    
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    
    val dayFilteredEntries = remember(timetableEntries, selectedDay) {
        timetableEntries.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
            .sortedBy { it.startTime }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TimetableEntry?>(null) }
    var selectedEntryForDetail by remember { mutableStateOf<TimetableEntry?>(null) }
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
                viewModel.parseTimetableFromBitmap(bitmap)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isDark = isSystemInDarkTheme()
                    val headerGradient = if (isDark) {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFE2E8F0), Color.White)
                        )
                    }
                    Column(modifier = Modifier.background(headerGradient).statusBarsPadding()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Academic Schedule",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconButton(onClick = { imagePickerLauncher.launch("image/*") }, modifier = Modifier.size(36.dp)) { 
                                    Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = "Scan", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) 
                                }
                                IconButton(onClick = { showAddDialog = true }, modifier = Modifier.size(36.dp)) { 
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) 
                                }
                            }
                        }
                        
                        PrimaryScrollableTabRow(
                            selectedTabIndex = daysOfWeek.indexOf(selectedDay).coerceAtLeast(0),
                            edgePadding = 16.dp,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                            divider = {}
                        ) {
                            daysOfWeek.forEach { day ->
                                Tab(
                                    selected = selectedDay == day,
                                    onClick = { viewModel.selectDay(day) },
                                    text = { 
                                        Text(
                                            text = day.take(3).uppercase(), 
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedDay == day) FontWeight.Black else FontWeight.Bold,
                                            color = if (selectedDay == day) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ) 
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (dayFilteredEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.EventNote, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(16.dp))
                            Text("No Classes Scheduled", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Tap '+' to add manually or use the scanner", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(dayFilteredEntries, key = { it.id }) { entry ->
                            val isToday = AutoSilentManager.getCurrentDayName() == selectedDay
                            val isCurrent = activeClassState.currentClass?.id == entry.id && isToday
                            val isDark = isSystemInDarkTheme()
                            val summary = viewModel.getSubjectAttendanceSummary(entry)
                            
                            val cardColor =  if (isDark) Color(0xFF2E2D31) else Color(0xFFF2F2F6)


                            Surface(
                                onClick = { selectedEntryForDetail = entry },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp), 
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = if (isCurrent) 6.dp else 2.dp,
                                border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) 
                                         else androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(cardColor)
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(55.dp)
                                    ) {
                                        Text(
                                            text = entry.startTime, 
                                            fontWeight = FontWeight.Black, 
                                            fontSize = 14.sp,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(14.dp)
                                                .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        )
                                        Text(
                                            text = entry.endTime, 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 11.sp, 
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = entry.subjectName, 
                                                fontWeight = FontWeight.Black, 
                                                fontSize = 16.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (isCurrent) {
                                                Spacer(Modifier.width(8.dp))
                                                PulseIndicator()
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.Place, 
                                                contentDescription = null, 
                                                modifier = Modifier.size(14.dp), 
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "Room ${entry.roomNumber}", 
                                                fontSize = 12.sp, 
                                                fontWeight = FontWeight.Black, 
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { editingEntry = entry }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                        }
                                        IconButton(onClick = { viewModel.deleteClassEntry(entry) }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        AttendancePieChart(
                                            percentage = summary.percentage,
                                            totalConducted = summary.totalConducted,
                                            modifier = Modifier.size(48.dp),
                                            strokeWidth = 2.5f
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isParsing) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                    Card(shape = RoundedCornerShape(24.dp)) {
                        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Decoding Timetable...", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddClassDialog(defaultDay = selectedDay, onDismiss = { showAddDialog = false }) { 
            viewModel.addManualClass(it)
            showAddDialog = false
        }
    }
    
    editingEntry?.let { entry ->
        AddClassDialog(existingEntry = entry, defaultDay = selectedDay, onDismiss = { editingEntry = null }) {
            viewModel.updateTimetableEntry(it)
            editingEntry = null
        }
    }

    selectedEntryForDetail?.let { entry ->
        SubjectAttendanceDetailDialog(entry = entry, viewModel = viewModel, onDismiss = { selectedEntryForDetail = null })
    }
}

@Composable
fun PulseIndicator() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )
    Surface(
        modifier = Modifier.size(8.dp).graphicsLayer(alpha = alpha),
        shape = CircleShape,
        color = Color(0xFF10B981)
    ) {}
}

@Composable
fun AttendancePieChart(percentage: Float, totalConducted: Int, modifier: Modifier = Modifier, strokeWidth: Float = 3f) {
    val isInitializing = totalConducted == 0
    val color = if (isInitializing) Color(0xFFF59E0B) else if (percentage >= 75f) Color(0xFF10B981) else Color(0xFFEF4444)
    val background = color.copy(alpha = 0.15f)
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = background, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth.dp.toPx()))
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = (percentage / 100f) * 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        Text(
            text = if (isInitializing) "0%" else "${percentage.toInt()}%", 
            fontSize = 11.sp, 
            fontWeight = FontWeight.Black, 
            color = color
        )
    }
}

@Composable
fun AddClassDialog(existingEntry: TimetableEntry? = null, defaultDay: String, onDismiss: () -> Unit, onAdd: (TimetableEntry) -> Unit) {
    var subject by remember { mutableStateOf(existingEntry?.subjectName ?: "") }
    var room by remember { mutableStateOf(existingEntry?.roomNumber ?: "") }
    var instructor by remember { mutableStateOf(existingEntry?.instructor ?: "") }
    var day by remember { mutableStateOf(existingEntry?.dayOfWeek ?: defaultDay) }
    var startTime by remember { mutableStateOf(existingEntry?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(existingEntry?.endTime ?: "10:30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingEntry != null) "Edit Class" else "Add Class", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Room") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = instructor, onValueChange = { instructor = it }, label = { Text("Instructor") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Start") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("End") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (subject.isNotBlank()) onAdd(TimetableEntry(id = existingEntry?.id ?: 0, subjectName = subject, roomNumber = room, dayOfWeek = day, startTime = startTime, endTime = endTime, instructor = instructor, autoSilentEnabled = existingEntry?.autoSilentEnabled ?: true)) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun SubjectAttendanceDetailDialog(entry: TimetableEntry, viewModel: MainViewModel, onDismiss: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val summary = remember(entry) { viewModel.getSubjectAttendanceSummary(entry) }
    val classInsideCount by viewModel.classInsideCount.collectAsStateWithLifecycle()
    val activeState by viewModel.systemActiveClassState.collectAsStateWithLifecycle()
    val isToday = AutoSilentManager.getCurrentDayName().equals(entry.dayOfWeek, ignoreCase = true)
    val isCurrent = activeState.isClassActive && activeState.currentClass?.id == entry.id && isToday
    var selectedLogForHistory by remember { mutableStateOf<com.example.data.PeriodAttendance?>(null) }
    var showManualStatusDialog by remember { mutableStateOf<Long?>(null) }

    if (showManualStatusDialog != null) {
        AlertDialog(
            onDismissRequest = { showManualStatusDialog = null },
            title = { Text("Mark Attendance", fontWeight = FontWeight.Black) },
            text = { Text("How would you like to mark your attendance for this class?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.confirmAttendanceManually(showManualStatusDialog!!, "PRESENT")
                    showManualStatusDialog = null
                }) { Text("Present") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.confirmAttendanceManually(showManualStatusDialog!!, "ABSENT")
                    showManualStatusDialog = null
                }) { Text("Absent", color = MaterialTheme.colorScheme.error) }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    val isDark = isSystemInDarkTheme()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(entry.subjectName, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text(text = "Subject Status Insight", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val insightColor = when {
                    summary.totalConducted == 0 -> if (isDark) Color(0xFFF59E0B).copy(alpha = 0.08f) else Color(0xFFFFFBEB)
                    summary.isSafe -> if (isDark) Color(0xFF10B981).copy(alpha = 0.08f) else Color(0xFFF0FDF4)
                    else -> if (isDark) Color(0xFFEF4444).copy(alpha = 0.08f) else Color(0xFFFEF2F2)
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = insightColor,
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    tonalElevation = 0.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        if (isDark) 1.dp else 0.5.dp, 
                        if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color(0xFFE2E8F0)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AttendancePieChart(percentage = summary.percentage, totalConducted = summary.totalConducted, modifier = Modifier.size(64.dp), strokeWidth = 3f)
                        Spacer(Modifier.width(20.dp))
                        Column {
                            Text(
                                text = "${String.format(locale, "%.1f", summary.percentage)}%", 
                                fontSize = 28.sp, 
                                fontWeight = FontWeight.Black, 
                                color = when {
                                    summary.totalConducted == 0 -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                                    summary.isSafe -> Color(0xFF059669)
                                    else -> Color(0xFFDC2626)
                                }
                            )
                            val attendedTime = if (summary.attended >= 60) "${summary.attended / 60}h ${summary.attended % 60}m" else "${summary.attended}m"
                            val totalTime = if (summary.totalConducted >= 60) "${summary.totalConducted / 60}h ${summary.totalConducted % 60}m" else "${summary.totalConducted}m"
                            Text(text = "$attendedTime / $totalTime attended", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (isCurrent) {
                    val isConfirmed by viewModel.isConfirmedManually.collectAsStateWithLifecycle()
                    if (!isConfirmed) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFF8FAFC),
                            shadowElevation = if (isDark) 0.dp else 2.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isDark) 1.dp else 0.5.dp, 
                                if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("LIVE VERIFICATION", fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary)
                                    Button(
                                        onClick = { showManualStatusDialog = entry.id }, 
                                        shape = RoundedCornerShape(8.dp), 
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), 
                                        modifier = Modifier.height(28.dp),
                                        colors = if (isDark) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary)
                                         else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), contentColor = Color.White)
                                    ) {
                                        Text("Mark Now", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "$classInsideCount of ${activeState.expectedTotalChecks} secure", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        val currentLogs by viewModel.allPeriodAttendance.collectAsStateWithLifecycle()
                        val today = java.text.SimpleDateFormat("yyyy-MM-dd", locale).format(java.util.Date())
                        val manualRecord = currentLogs.find { it.timetableEntryId == entry.id && it.date == today }
                        val statusText = if (manualRecord?.status == "PRESENT") "MARKED PRESENT" else "MARKED ABSENT"
                        val badgeColor = if (manualRecord?.status == "PRESENT") Color(0xFF10B981) else Color(0xFFEF4444)

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = badgeColor.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(if (manualRecord?.status == "PRESENT") Icons.Default.CheckCircle else Icons.Default.Cancel, null, tint = badgeColor, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(statusText, fontSize = 16.sp, color = badgeColor, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Text("Session History", fontWeight = FontWeight.Black, fontSize = 16.sp)
                if (summary.recentLogs.isEmpty()) {
                    Text("No records captured yet.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp), 
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(summary.recentLogs) { log ->
                            val logStatusColor = if (isDark) Color(0xFF242424) else Color(0xFFF1F5F9)
                            Surface(
                                onClick = { selectedLogForHistory = log },
                                shape = RoundedCornerShape(16.dp),
                                color = logStatusColor,
                                shadowElevation = if (isDark) 0.dp else 1.dp,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f) else Color(0xFFF1F5F9))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp), 
                                    horizontalArrangement = Arrangement.SpaceBetween, 
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(log.date, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${log.checksInside}/${log.totalChecks} Checks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(color = if (log.status == "PRESENT") Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f), shape = CircleShape) {
                                        Text(text = log.status, fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (log.status == "PRESENT") Color(0xFF059669) else Color(0xFFDC2626), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("Done", fontWeight = FontWeight.Bold) } },
        shape = RoundedCornerShape(28.dp)
    )

    selectedLogForHistory?.let { log ->
        PeriodCheckHistoryDialog(
            viewModel = viewModel,
            period = log,
            onDismiss = { selectedLogForHistory = null }
        )
    }
}
