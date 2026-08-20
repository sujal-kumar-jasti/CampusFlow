package com.example.ui.screens

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainActivity
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AppColors
import com.example.ui.theme.TrendGreen
import com.example.ui.theme.TrendGreenDark
import com.example.util.AutoSilentManager
import com.example.util.AutoStartPermissionHelper
import com.example.util.IIESTAcademicCalendar
import com.example.widget.LiveClassWidgetProvider
import com.example.widget.ShortcutsWidgetProvider

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToAttendance: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToAcademicCalendar: (String?) -> Unit
) {
    val context = LocalContext.current
    val isInside by viewModel.isInsideCampus.collectAsStateWithLifecycle()
    val distance by viewModel.distanceToCampus.collectAsStateWithLifecycle()
    val campusLoc by viewModel.campusLocation.collectAsStateWithLifecycle()
    val todayRecord by viewModel.todayRecord.collectAsStateWithLifecycle()
    val systemActiveClassState by viewModel.systemActiveClassState.collectAsStateWithLifecycle()
    val userLat by viewModel.userLatitude.collectAsStateWithLifecycle()
    val userLon by viewModel.userLongitude.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val unresolvedSessions by viewModel.unresolvedSessions.collectAsStateWithLifecycle()

    val locale = LocalConfiguration.current.locales[0]

    var showConfigDialog by remember { mutableStateOf(false) }
    var hasDndState by remember { mutableStateOf(AutoSilentManager.hasDndPermission(context)) }
    var hasBatteryOptState by remember { mutableStateOf(AutoStartPermissionHelper.isIgnoringBatteryOptimizations(context)) }

    val subjectSummaries by viewModel.groupedSubjectSummaries.collectAsStateWithLifecycle()
    val isEndOfSem = subjectSummaries.any { it.isEndOfSemester }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasDndState = AutoSilentManager.hasDndPermission(context)
                hasBatteryOptState = AutoStartPermissionHelper.isIgnoringBatteryOptimizations(context)
                viewModel.refreshActiveClassState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (unresolvedSessions.isNotEmpty()) {
            item {
                UnresolvedSessionsCard(
                    sessions = unresolvedSessions,
                    onResolve = { id, status -> viewModel.resolveSession(id, status) }
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_campus),
                        contentDescription = "Campus Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Black.copy(alpha = 0.9f)
                                    )
                                )
                            )
                    )
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                text = campusLoc.name.uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Campus Hub",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            lineHeight = 36.sp
                        )
                        Text(
                            text = "Precision academic monitoring",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        item {
            PermissionsOverviewCard(context = context, viewModel = viewModel)
        }

        if (isEndOfSem) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text("Semester Transition", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Semester 7 has concluded. It's time to prepare for Semester 8! You can clear your current timetable or update it when ready.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onNavigateToTimetable() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Go to Timetable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            LiveInsightCard(
                isInside = isInside,
                distance = distance,
                userLat = userLat,
                userLon = userLon,
                todayRecord = todayRecord,
                activeClassState = systemActiveClassState,
                viewModel = viewModel,
                onConfigClick = { showConfigDialog = true },
                onAttendanceClick = onNavigateToAttendance,
                onTimetableClick = onNavigateToTimetable
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, null, tint = AmberWarning, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(text = "Upcoming Reminders", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                TextButton(onClick = onNavigateToReminders) {
                    Text("View All", fontSize = 12.sp)
                }
            }
        }

        if (reminders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        "No upcoming tasks or exams.",
                        modifier = Modifier.padding(20.dp),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val pendingReminders = reminders.asSequence().filter { !it.isCompleted }.take(3).toList()
            items(pendingReminders, key = { "dash_rem_${it.id}" }) { reminder ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onNavigateToReminders() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when(reminder.priority) {
                                "HIGH" -> Color(0xFFEF4444)
                                "MEDIUM" -> Color(0xFFF59E0B)
                                else -> Color(0xFF10B981)
                            },
                            modifier = Modifier.size(12.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(reminder.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${reminder.dueDate} at ${reminder.dueTime}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Checkbox(
                            checked = reminder.isCompleted,
                            onCheckedChange = { viewModel.toggleReminderCompleted(reminder) }
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
                Text(
                    text = "Campus Timeline",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                IconButton(onClick = { onNavigateToAcademicCalendar(null) }) {
                    Icon(Icons.Default.EditCalendar, "View Academic Calendar", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToAcademicCalendar(null) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    IIESTAcademicCalendar.getUpcomingEvents(3).forEachIndexed { index, event ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onNavigateToAcademicCalendar(event.date) }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = when(event.type) {
                                    IIESTAcademicCalendar.EventType.HOLIDAY -> Color(0xFFF59E0B)
                                    IIESTAcademicCalendar.EventType.EXAM -> Color(0xFFEF4444)
                                    IIESTAcademicCalendar.EventType.FEST -> Color(0xFF3B82F6) // Blue
                                    else -> MaterialTheme.colorScheme.primary
                                }.copy(alpha = 0.1f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = when(event.type) {
                                        IIESTAcademicCalendar.EventType.HOLIDAY -> Icons.Default.School
                                        IIESTAcademicCalendar.EventType.EXAM -> Icons.Default.EditCalendar
                                        IIESTAcademicCalendar.EventType.FEST -> Icons.Default.CheckCircle
                                        else -> Icons.Default.Schedule
                                    },
                                    contentDescription = null,
                                    tint = when(event.type) {
                                        IIESTAcademicCalendar.EventType.HOLIDAY -> Color(0xFFD97706)
                                        IIESTAcademicCalendar.EventType.EXAM -> Color(0xFFB91C1C)
                                        IIESTAcademicCalendar.EventType.FEST -> Color(0xFF3B82F6) // Blue
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.padding(8.dp).size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(event.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(event.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (index < 2) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Quick Tools",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ToolCard(
                    title = "Reminders",
                    subtitle = "Alarms & Tasks",
                    icon = Icons.Default.NotificationsActive,
                    badgeColor = Color(0xFFF59E0B),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tool_reminders_btn"),
                    onClick = onNavigateToReminders
                )

                ToolCard(
                    title = "Scanner",
                    subtitle = "AI OCR & PDF",
                    icon = Icons.Default.DocumentScanner,
                    badgeColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tool_scanner_btn"),
                    onClick = onNavigateToScanner
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ToolCard(
                    title = "AI Timetable",
                    subtitle = "Schedule Sync",
                    icon = Icons.Default.EditCalendar,
                    badgeColor = Color(0xFF8B5CF6),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tool_timetable_btn"),
                    onClick = onNavigateToTimetable
                )

                ToolCard(
                    title = "Attendance",
                    subtitle = "GPS Logs",
                    icon = Icons.Default.PinDrop,
                    badgeColor = Color(0xFF10B981),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tool_attendance_btn"),
                    onClick = onNavigateToAttendance
                )
            }
        }

        item {
            HomeScreenWidgetsCard(context = context)
        }
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
            },
            onFetchLocation = { viewModel.fetchCurrentLocationAndDetectCampus(context) }
        )
    }
}

@Composable
fun UnresolvedSessionsCard(
    sessions: List<com.example.data.PeriodAttendance>,
    onResolve: (Long, String) -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val session = sessions.getOrNull(currentIndex) ?: return
    
    val cardGradient = if (isSystemInDarkTheme()) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                MaterialTheme.colorScheme.error.copy(alpha = 0.04f),
                Color.Transparent
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFF7ED), // Very Light Orange Mist
                Color.White
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSystemInDarkTheme()) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else Color(0xFFFED7AA))
    ) {
        Column(
            modifier = Modifier
                .background(cardGradient)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "REQUIRES REVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (sessions.size > 1) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${currentIndex + 1} OF ${sessions.size}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Unmarked Session: ${session.subjectName}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Held on ${session.date} at ${session.periodTime}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onResolve(session.id, "PRESENT") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("I Was Present", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                
                OutlinedButton(
                    onClick = { onResolve(session.id, "ABSENT") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("I Missed It", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
            
            TextButton(
                onClick = { onResolve(session.id, "NOT_CONDUCTED") },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Text("Class Was Cancelled", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LiveInsightCard(
    isInside: Boolean,
    distance: Double,
    userLat: Double,
    userLon: Double,
    todayRecord: com.example.data.AttendanceRecord?,
    activeClassState: AutoSilentManager.ActiveClassState,
    viewModel: MainViewModel,
    onConfigClick: () -> Unit,
    onAttendanceClick: () -> Unit,
    onTimetableClick: () -> Unit
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    
    val locationManager = remember { context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager }
    var isGpsEnabled by remember { mutableStateOf(locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) }
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    val insightGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.02f),
            Color.Transparent
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .background(insightGradient)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LIVE INSIGHTS", 
                    fontWeight = FontWeight.Black, 
                    fontSize = 12.sp, 
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onConfigClick, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // GPS & Boundary Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { 
                    if (!isGpsEnabled) {
                        AutoStartPermissionHelper.openLocationSettings(context)
                    }
                }
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isInside) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isInside) Icons.Default.LocationOn else Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = if (isInside) {if(isSystemInDarkTheme())TrendGreenDark else TrendGreen} else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    val statusText = when {
                        !isGpsEnabled -> "GPS Disabled"
                        userLat == 0.0 -> "Locating..."
                        isInside && activeClassState.isClassActive -> "Inside Class"
                        isInside -> "Inside Campus"
                        else -> "Outside Campus"
                    }
                    val darkTheme = false
                    val statusColor = if (!isGpsEnabled || !isInside) MaterialTheme.colorScheme.error else { if (isSystemInDarkTheme()) TrendGreen else TrendGreenDark }
                    
                    Text(
                        text = statusText,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = statusColor
                    )
                    Text(
                        text = if (!isGpsEnabled) "Check settings" else if (userLat == 0.0) "Awaiting signal" else "${String.format(locale, "%.1f", distance)}m away",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(14.dp))

            // Active Session or Today's Status
            if (activeClassState.isClassActive) {
                SessionInsightItem(activeClassState, viewModel, onTimetableClick)
            } else {
                TodayStatusItem(todayRecord, viewModel, onAttendanceClick)
            }
        }
    }
}

@Composable
fun SessionInsightItem(
    state: AutoSilentManager.ActiveClassState,
    viewModel: MainViewModel,
    onTimetableClick: () -> Unit
) {
    val insideCount by viewModel.classInsideCount.collectAsStateWithLifecycle()
    val isConfirmed by viewModel.isConfirmedManually.collectAsStateWithLifecycle()
    val expected = state.expectedTotalChecks
    
    Column(modifier = Modifier.clickable { onTimetableClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = state.currentClass?.subjectName ?: "Active Session",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$insideCount of $expected checks inside",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val summary = viewModel.getSubjectAttendanceSummary(state.currentClass!!)
            AttendancePieChart(
                percentage = summary.percentage,
                totalConducted = summary.totalConducted,
                modifier = Modifier.size(48.dp),
                strokeWidth = 2f
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            color = if (isConfirmed) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (isConfirmed) "SYNCED" else "TRACKING",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = if (isConfirmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun TodayStatusItem(
    record: com.example.data.AttendanceRecord?,
    viewModel: MainViewModel,
    onAttendanceClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onAttendanceClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Today's Attendance",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = record?.let { "Checked in at ${it.checkInTime}" } ?: "No logs for today",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (record == null) {
            Button(
                onClick = { viewModel.manualCheckIn() },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Check In", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
        } else {
            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun HomeScreenWidgetsCard(context: android.content.Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_screen_widgets_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp).size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Home Screen Widgets",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Add dynamic Pixel widgets to your launcher",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { pinWidgetToHomeScreen(context, LiveClassWidgetProvider::class.java) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pin_live_class_widget_btn"),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pin Live", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { pinWidgetToHomeScreen(context, ShortcutsWidgetProvider::class.java) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pin_shortcuts_widget_btn"),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp)
                ) {
                    Icon(imageVector = Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pin Shortcuts", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun pinWidgetToHomeScreen(context: android.content.Context, providerClass: Class<*>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val myProvider = ComponentName(context, providerClass)
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            // Passing null for successCallback prevents the app from restarting/navigating
            // when the widget is successfully pinned. The system handles confirmation.
            appWidgetManager.requestPinAppWidget(myProvider, null, null)
            Toast.makeText(context, "Opening Home Screen...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Launcher does not support pinning.", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun ToolCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val toolGradient = Brush.linearGradient(
        colors = listOf(
            badgeColor.copy(alpha = 0.15f),
            badgeColor.copy(alpha = 0.05f),
            Color.Transparent
        )
    )
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = if (isDark) Color(0xFF1A1A1A) else Color.White,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(toolGradient)
                .padding(20.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.2f),
                modifier = Modifier.size(44.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = badgeColor,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun PermissionsOverviewCard(context: android.content.Context, viewModel: MainViewModel) {
    val isDark = isSystemInDarkTheme()
    var hasFineLoc by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasBgLoc by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var hasDnd by remember {
        mutableStateOf(AutoSilentManager.hasDndPermission(context))
    }

    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasBatteryOptExempt by remember {
        mutableStateOf(
            AutoStartPermissionHelper.isIgnoringBatteryOptimizations(context) || 
            AutoStartPermissionHelper.isBatteryOptExemptConfirmed(context)
        )
    }

    var hasOemAutostartConfirmed by remember {
        mutableStateOf(AutoStartPermissionHelper.isOemAutostartConfirmed(context))
    }

    var hasNotificationPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var hasAlarmPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                alarmManager.canScheduleExactAlarms()
            } else true
        )
    }

    var showOemConfirmDialog by remember { mutableStateOf(false) }
    var showBatteryConfirmDialog by remember { mutableStateOf(false) }

    fun checkAllPermissions() {
        val fineLocNow = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fineLocNow && !hasFineLoc) {
            viewModel.fetchCurrentLocationAndDetectCampus(context)
        }
        hasFineLoc = fineLocNow
        
        hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasBgLoc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        hasDnd = AutoSilentManager.hasDndPermission(context)
        
        // RE-EVALUATE STRICTLY & SEPARATELY
        val batteryExemptSys = AutoStartPermissionHelper.isIgnoringBatteryOptimizations(context)
        val batteryExemptManual = AutoStartPermissionHelper.isBatteryOptExemptConfirmed(context)
        hasBatteryOptExempt = batteryExemptSys || batteryExemptManual
        
        hasOemAutostartConfirmed = AutoStartPermissionHelper.isOemAutostartConfirmed(context)
        
        hasNotificationPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        
        hasAlarmPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            am.canScheduleExactAlarms()
        } else true
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkAllPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val reqMultiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        checkAllPermissions()
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            viewModel.fetchCurrentLocationAndDetectCampus(context)
        }
    }

    val reqSinglePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        checkAllPermissions()
    }

    val allGranted = hasFineLoc && hasBgLoc && hasDnd && hasCamera && hasBatteryOptExempt && hasOemAutostartConfirmed && hasNotificationPerm && hasAlarmPerm

    if (allGranted) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "System Security",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enable required automation protocols",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val perms = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.CAMERA
                    )
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                        perms.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    reqMultiplePermissionsLauncher.launch(perms.toTypedArray())

                    // Alarm permission must be requested via intent separately
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val am = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                        if (!am.canScheduleExactAlarms()) {
                            Toast.makeText(context, "Please enable 'Alarms & Reminders' for background checks", Toast.LENGTH_LONG).show()
                            context.startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("Authorize All Systems", fontSize = 13.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PermissionStatusChip(
                        label = "Fine GPS",
                        isGranted = hasFineLoc,
                        modifier = Modifier.weight(1f)
                    ) {
                        reqMultiplePermissionsLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }
                    PermissionStatusChip(
                        label = "Camera OCR",
                        isGranted = hasCamera,
                        modifier = Modifier.weight(1f)
                    ) {
                        reqSinglePermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PermissionStatusChip(
                        label = "Bg GPS",
                        isGranted = hasBgLoc,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                try {
                                    reqSinglePermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                } catch (e: Exception) {
                                    AutoStartPermissionHelper.openAppDetailsSettings(context)
                                }
                            } else {
                                reqMultiplePermissionsLauncher.launch(arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ))
                            }
                        } else {
                            AutoStartPermissionHelper.openAppDetailsSettings(context)
                        }
                    }

                    PermissionStatusChip(
                        label = "Auto-Silent",
                        isGranted = hasDnd,
                        modifier = Modifier.weight(1f)
                    ) {
                        try {
                            context.startActivity(AutoSilentManager.openDndSettingsIntent())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PermissionStatusChip(
                        label = "Battery",
                        isGranted = hasBatteryOptExempt,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!hasBatteryOptExempt) {
                            AutoStartPermissionHelper.requestUnrestrictedBatteryUsage(context)
                            showBatteryConfirmDialog = true
                        }
                    }

                    PermissionStatusChip(
                        label = "Autostart",
                        isGranted = hasOemAutostartConfirmed,
                        modifier = Modifier.weight(1f)
                    ) {
                        AutoStartPermissionHelper.openAutoStartSettings(context)
                        showOemConfirmDialog = true
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PermissionStatusChip(
                        label = "Alerts",
                        isGranted = hasNotificationPerm,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            reqSinglePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    PermissionStatusChip(
                        label = "Alarms",
                        isGranted = hasAlarmPerm,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        }
                    }
                }
            }
        }
    }

    if (showBatteryConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryConfirmDialog = false },
            title = { Text("Confirm Battery Mode") },
            text = { Text("Did you set the app to 'No Restrictions' / 'Unrestricted' in battery settings?") },
            confirmButton = {
                Button(
                    onClick = {
                        AutoStartPermissionHelper.setBatteryOptExemptConfirmed(context, true)
                        hasBatteryOptExempt = true
                        showBatteryConfirmDialog = false
                    }
                ) {
                    Text("Yes, I've set it")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryConfirmDialog = false }) {
                    Text("Not Yet")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showOemConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showOemConfirmDialog = false },
            title = { Text("Confirm Autostart") },
            text = { Text("Did you enable Autostart / Background Auto-Run in settings?") },
            confirmButton = {
                Button(
                    onClick = {
                        AutoStartPermissionHelper.setOemAutostartConfirmed(context, true)
                        hasOemAutostartConfirmed = true
                        showOemConfirmDialog = false
                    }
                ) {
                    Text("Yes, Enabled")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOemConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
fun PermissionStatusChip(
    label: String,
    isGranted: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isGranted) {
        if (isDark) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFECFDF5)
    } else {
        if (isDark) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else Color(0xFFFEF2F2)
    }
    
    val contentColor = if (isGranted) {
        if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    } else {
        if (isDark) MaterialTheme.colorScheme.error else Color(0xFFDC2626)
    }

    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}
