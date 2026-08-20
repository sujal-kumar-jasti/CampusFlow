package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.theme.LightLavenderMist
import com.example.ui.theme.LightSkyMist
import com.example.util.ExportUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: MainViewModel,
    onNavigateToCalendar: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val campusLoc by viewModel.campusLocation.collectAsStateWithLifecycle()
    val userLat by viewModel.userLatitude.collectAsStateWithLifecycle()
    val userLon by viewModel.userLongitude.collectAsStateWithLifecycle()
    val periodRecords by viewModel.allPeriodAttendance.collectAsStateWithLifecycle()
    val subjectSummaries by viewModel.groupedSubjectSummaries.collectAsStateWithLifecycle()
    val systemActiveClassState by viewModel.systemActiveClassState.collectAsStateWithLifecycle()
    val classInsideCount by viewModel.classInsideCount.collectAsStateWithLifecycle()
    val isConfirmedManually by viewModel.isConfirmedManually.collectAsStateWithLifecycle()

    var showConfigDialog by remember { mutableStateOf(false) }
    var selectedSubjectSummary by remember { mutableStateOf<MainViewModel.SubjectAttendanceSummary?>(null) }
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

    DisposableEffect(Unit) {
        onDispose {}
    }
    
    LaunchedEffect(Unit) {
        ExportUtils.clearTempCache(context)
    }

    val totalConductedAll = subjectSummaries.sumOf { it.totalConducted }
    val totalAttendedAll = subjectSummaries.sumOf { it.attended }
    val overallPercentage = if (totalConductedAll > 0) (totalAttendedAll.toFloat() / totalConductedAll.toFloat()) * 100f else 0f
    val safeCount = subjectSummaries.count { it.isSafe }
    val riskCount = subjectSummaries.count { !it.isSafe }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "Attendance", 
                            fontWeight = FontWeight.Black, 
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = campusLoc.name.uppercase(), 
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 9.sp, 
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToCalendar?.invoke() }) {
                        Icon(Icons.Default.CalendarMonth, "Calendar")
                    }
                    IconButton(onClick = {
                        val pdfFile = ExportUtils.createTimetableSubjectPdfReport(context, subjectSummaries, campusLoc.name)
                        pdfFile?.let { ExportUtils.shareFile(context, it, "application/pdf") }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, "Export")
                    }
                    IconButton(onClick = { showConfigDialog = true }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets(top = 30.dp)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            item {
                AttendanceOverviewCard(
                    percentage = overallPercentage,
                    attended = totalAttendedAll,
                    total = totalConductedAll,
                    safeCount = safeCount,
                    riskCount = riskCount
                )
            }

            item {
                if (systemActiveClassState.isClassActive && systemActiveClassState.currentClass != null) {
                    val currentClass = systemActiveClassState.currentClass!!
                    val liveGradient = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .background(liveGradient)
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LIVE SESSION TRACKING", 
                                    fontWeight = FontWeight.Black, 
                                    fontSize = 11.sp, 
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                                if (!isConfirmedManually) {
                                    Button(
                                        onClick = { showManualStatusDialog = currentClass.id },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Mark Now", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                } else {
                                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", locale).format(java.util.Date())
                                    val manualRecord = periodRecords.find { it.timetableEntryId == currentClass.id && it.date == today }
                                    val statusText = if (manualRecord?.status == "PRESENT") "MARKED: PRESENT" else "MARKED: ABSENT"
                                    val badgeColor = if (manualRecord?.status == "PRESENT") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

                                    Surface(color = badgeColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                                        Text(statusText, color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(currentClass.subjectName, fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                            
                            val expectedTotal = systemActiveClassState.expectedTotalChecks
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Icon(Icons.Default.PinDrop, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "$classInsideCount of $expectedTotal checks secure", 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            val isThresholdMet = expectedTotal > 0 && classInsideCount >= (expectedTotal + 1) / 2
                            
                            if (!isConfirmedManually) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Surface(
                                    color = if (isThresholdMet) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isThresholdMet) Icons.Default.CheckCircle else Icons.Default.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isThresholdMet) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (isThresholdMet) "THRESHOLD MET" else "VERIFICATION IN PROGRESS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isThresholdMet) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "GPS checks run strictly during active timetable period slots.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Subject Attendance Breakdown", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Tap to view logs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (subjectSummaries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Timetable Entries Configured", fontWeight = FontWeight.Bold)
                            Text("Add classes to your timetable to track subject-by-subject attendance.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(subjectSummaries, key = { "grouped_${it.subjectName}" }) { summary ->
                    val subjectGradient = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        )
                    )
                    Surface(
                        onClick = { selectedSubjectSummary = summary },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier
                                .background(subjectGradient)
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AttendancePieChart(
                                            percentage = summary.percentage,
                                            totalConducted = summary.totalConducted,
                                            modifier = Modifier.size(52.dp),
                                            strokeWidth = 2.5f
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Surface(
                                                color = if (summary.subjectName.contains("Lab", true)) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (summary.subjectName.contains("Lab", true)) "PRACTICAL" else "LECTURE",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (summary.subjectName.contains("Lab", true)) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                            Text(
                                                text = summary.subjectName, 
                                                fontWeight = FontWeight.Black, 
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = summary.periodTime,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(start = 68.dp)
                                    )
                                    
                                    if (summary.totalConducted > 0) {
                                        Text(
                                            text = if (summary.percentage < 75f) "Attend next ${summary.requiredToReach75} classes to hit 75%" 
                                                   else if (summary.canSkip > 0) "You can safely skip ${summary.canSkip} classes"
                                                   else "On the edge! Don't skip next class.",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (summary.percentage < 75f) Color(0xFFDC2626) else Color(0xFF059669),
                                            modifier = Modifier.padding(start = 68.dp, top = 4.dp)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (summary.totalConducted > 0) "${String.format(locale, "%.1f", summary.percentage)}%" else "0%",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (summary.isSafe) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                    )

                                    Surface(
                                        color = if (summary.isSafe) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                        shape = CircleShape
                                    ) {
                                        val attendedTime = if (summary.attended >= 60) "${summary.attended / 60}h" else "${summary.attended}m"
                                        val totalTime = if (summary.totalConducted >= 60) "${summary.totalConducted / 60}h" else "${summary.totalConducted}m"
                                        Text(
                                            text = "$attendedTime/$totalTime",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (summary.isSafe) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedSubjectSummary?.let { summary ->
        GroupedSubjectAttendanceDetailDialog(
            viewModel = viewModel,
            summary = summary,
            onDismiss = { selectedSubjectSummary = null }
        )
    }

    if (showConfigDialog) {
        MapCampusConfigDialog(
            currentLoc = campusLoc,
            userLat = userLat,
            userLon = userLon,
            onDismiss = { showConfigDialog = false },
            onSave = { name, lat, lon, radius ->
                viewModel.updateCampusSettings(name, lat, lon, radius)
                showConfigDialog = false
            }
        )
    }
}


@Composable
fun GroupedSubjectAttendanceDetailDialog(
    viewModel: MainViewModel,
    summary: MainViewModel.SubjectAttendanceSummary,
    onDismiss: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    var showManualAdd by remember { mutableStateOf(false) }
    var selectedPeriodForHistory by remember { mutableStateOf<com.example.data.PeriodAttendance?>(null) }

    val isDark = isSystemInDarkTheme()
    val dialogGradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.02f),
                Color.Transparent
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                LightLavenderMist,
                LightSkyMist.copy(alpha = 0.4f),
                Color.Transparent
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(summary.subjectName, fontWeight = FontWeight.Black, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "${summary.periodTime} (${summary.dayOfWeek})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else LightSkyMist,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Detailed session metrics and retrospective log history.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        if (isDark) 1.dp else 0.5.dp, 
                        if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0xFFF1F5F9)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .background(if (isDark) dialogGradient else Brush.linearGradient(listOf(LightSkyMist, Color.Transparent)))
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Subject Mastery", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${String.format(locale, "%.1f", summary.percentage)}%",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = if (summary.isSafe) Color(0xFF059669) else Color(0xFFDC2626)
                            )
                            val attendedTime = if (summary.attended >= 60) "${summary.attended / 60}h ${summary.attended % 60}m" else "${summary.attended}m"
                            val totalTime = if (summary.totalConducted >= 60) "${summary.totalConducted / 60}h ${summary.totalConducted % 60}m" else "${summary.totalConducted}m"
                            Text("$attendedTime of $totalTime attended", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Surface(
                            color = if (summary.isSafe) Color(0xFF10B981) else Color(0xFFEF4444),
                            shape = CircleShape
                        ) {
                            Text(
                                text = if (summary.isSafe) "SAFE" else "ALERT",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Verified Sessions", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    TextButton(onClick = { showManualAdd = true }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Manual", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (summary.recentLogs.isEmpty()) {
                    Text("No records captured for this subject yet.", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(summary.recentLogs, key = { it.id }) { rec ->
                            Surface(
                                onClick = { selectedPeriodForHistory = rec },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                tonalElevation = if (isDark) 0.dp else 1.dp,
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp, 
                                    if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isDark) dialogGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(rec.date, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                        Text("${rec.checksInside}/${rec.totalChecks} Checks • GPS Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(
                                        color = if (rec.status == "PRESENT") Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = rec.status,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (rec.status == "PRESENT") Color(0xFF059669) else Color(0xFFDC2626),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("Close Panel", fontWeight = FontWeight.Bold) }
        },
        shape = RoundedCornerShape(28.dp)
    )

    if (showManualAdd) {
        ManualAttendanceDialog(
            subjectName = summary.subjectName,
            onDismiss = { showManualAdd = false },
            onConfirm = { date, status ->
                viewModel.addManualAttendanceRecord(summary.subjectName, date, status)
                showManualAdd = false
            }
        )
    }

    selectedPeriodForHistory?.let { period ->
        PeriodCheckHistoryDialog(
            viewModel = viewModel,
            period = period,
            onDismiss = { selectedPeriodForHistory = null }
        )
    }
}

@Composable
fun AttendanceOverviewCard(
    percentage: Float,
    attended: Int,
    total: Int,
    safeCount: Int,
    riskCount: Int
) {
    val locale = LocalConfiguration.current.locales[0]
    val isInitializing = total == 0
    val isSafe = percentage >= 75f
    
    val statusColor = if (isInitializing) Color(0xFFF59E0B)
                      else if (isSafe) Color(0xFF10B981)
                      else Color(0xFFEF4444)

    val gradient = Brush.linearGradient(
        colors = listOf(
            statusColor.copy(alpha = 0.1f),
            Color.Transparent
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(top=10.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .background(gradient)
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AttendancePieChart(
                percentage = percentage,
                totalConducted = total,
                modifier = Modifier.size(76.dp),
                strokeWidth = 3f
            )
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isInitializing) "AWAITING LOGS" else "Overall Attendance",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isInitializing) "0.0%" else "${String.format(locale, "%.1f", percentage)}%",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isInitializing) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else if (isSafe) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                )
                val attendedTime = if (attended >= 60) "${attended / 60}h ${attended % 60}m" else "${attended}m"
                val totalTime = if (total >= 60) "${total / 60}h ${total % 60}m" else "${total}m"
                Text(
                    text = "$attendedTime / $totalTime attended",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = statusColor,
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isInitializing) "INIT" else if (isSafe) "SAFE" else "RISK",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnalyticsMiniItem("Safe", safeCount.toString(), MaterialTheme.colorScheme.tertiary)
                    AnalyticsMiniItem("Risk", riskCount.toString(), MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun ManualAttendanceDialog(
    subjectName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    var date by remember { mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", locale).format(java.util.Date())) }
    var status by remember { mutableStateOf("PRESENT") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Attendance Entry", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Adding record for $subjectName", fontSize = 14.sp)
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status: ", fontWeight = FontWeight.Bold)
                    RadioButton(selected = status == "PRESENT", onClick = { status = "PRESENT" })
                    Text("Present")
                    Spacer(modifier = Modifier.width(12.dp))
                    RadioButton(selected = status == "ABSENT", onClick = { status = "ABSENT" })
                    Text("Absent")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(date, status) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun AnalyticsMiniItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Black, fontSize = 20.sp, color = color)
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

