package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TimetableEntry
import com.example.ui.MainViewModel
import com.example.util.IIESTAcademicCalendar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceCalendarScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val currentMonthName = SimpleDateFormat("MMMM yyyy", locale).format(calendar.time)
    
    val timetableEntries by viewModel.timetableEntries.collectAsStateWithLifecycle()
    val allAttendance by viewModel.allPeriodAttendance.collectAsStateWithLifecycle()
    
    val selectedDateStr = SimpleDateFormat("yyyy-MM-dd", locale).format(selectedDate.time)
    val dayOfWeekName = SimpleDateFormat("EEEE", locale).format(selectedDate.time)
    
    val classesForDay = timetableEntries.filter { it.dayOfWeek.equals(dayOfWeekName, ignoreCase = true) }
        .sortedBy { viewModel.parseTimeToMinutes(it.startTime) }

    // Use a date-only string for events check to be robust
    val eventsForDay = remember(selectedDateStr) { IIESTAcademicCalendar.getEventsForDate(selectedDateStr) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Records", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                windowInsets = WindowInsets(top = 28.dp)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Month Selector
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val newCal = calendar.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            calendar = newCal
                        }) {
                            Icon(Icons.Default.ChevronLeft, "Previous", tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        Text(
                            text = currentMonthName.uppercase(), 
                            fontWeight = FontWeight.Black, 
                            fontSize = 15.sp,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        IconButton(onClick = {
                            val newCal = calendar.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            calendar = newCal
                        }) {
                            Icon(Icons.Default.ChevronRight, "Next", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            
            // Calendar Grid
            item {
                Spacer(modifier = Modifier.height(6.dp))
                MonthCalendarView(
                    calendar = calendar,
                    selectedDate = selectedDate,
                    locale = locale,
                    onDateSelected = { selectedDate = it }
                )
            }
            
            // Date Title
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(4.dp).height(20.dp).clip(CircleShape)
                    ) {}
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "$dayOfWeekName, ${SimpleDateFormat("MMM d", locale).format(selectedDate.time)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            if (classesForDay.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No classes scheduled for this day.", color = Color.Gray)
                    }
                }
            } else {
                val today = Calendar.getInstance()
                val isFutureDay = selectedDate.after(today) && !isSameDay(selectedDate, today)
                
                // Semester 7 Bounds
                val semStart = "2026-07-20"
                val semEnd = "2026-11-13"
                val isOutOfSem = selectedDateStr !in semStart..semEnd

                items(classesForDay) { entry ->
                    val attendance = allAttendance.find { it.timetableEntryId == entry.id && it.date == selectedDateStr }
                    
                    // Specific time check for today
                    var isClassFuture = isFutureDay
                    if (isSameDay(selectedDate, today)) {
                        val classStartMins = viewModel.parseTimeToMinutes(entry.startTime)
                        val nowMins = today.get(Calendar.HOUR_OF_DAY) * 60 + today.get(Calendar.MINUTE)
                        if (classStartMins > nowMins) {
                            isClassFuture = true
                        }
                    }

                    CalendarClassItem(
                        entry = entry,
                        attendance = attendance,
                        isDisabled = isClassFuture || isOutOfSem,
                        onMarkStatus = { status ->
                            viewModel.addManualAttendanceForDate(selectedDateStr, entry, status)
                        }
                    )
                }
                
                if (isOutOfSem) {
                    item {
                        Text(
                            "Manual marking is restricted outside semester dates ($semStart to $semEnd).",
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Holiday/Special Day Detail at Bottom (MOVED OUTSIDE CLASSES CHECK)
            val holidayEvents = eventsForDay.filter { 
                it.type == IIESTAcademicCalendar.EventType.HOLIDAY || 
                it.type == IIESTAcademicCalendar.EventType.FEST ||
                it.type == IIESTAcademicCalendar.EventType.BREAK
            }
            
            val isWeekend = dayOfWeekName.equals("Saturday", ignoreCase = true) || 
                            dayOfWeekName.equals("Sunday", ignoreCase = true)

            if (holidayEvents.isNotEmpty() || isWeekend) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = if (holidayEvents.isNotEmpty()) Color(0xFFF97316).copy(alpha = 0.12f) 
                                else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            if (holidayEvents.isNotEmpty()) Color(0xFFD97706).copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (holidayEvents.isNotEmpty()) Icons.Default.WbSunny else Icons.Default.Info, 
                                    null, 
                                    tint = if (holidayEvents.isNotEmpty()) Color(0xFFD97706) else MaterialTheme.colorScheme.primary, 
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (holidayEvents.isNotEmpty()) "Holiday Detail" else "Weekend Note", 
                                    fontWeight = FontWeight.Black, 
                                    fontSize = 15.sp, 
                                    color = if (holidayEvents.isNotEmpty()) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                                )
                            }
                            if (isWeekend) {
                                Text(
                                    text = "• Weekly Off (Weekend)", 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    modifier = Modifier.padding(top = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            holidayEvents.forEach { e ->
                                Text(
                                    text = "• ${e.name}", 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Black, 
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = if (e.type == IIESTAcademicCalendar.EventType.HOLIDAY) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthCalendarView(
    calendar: Calendar,
    selectedDate: Calendar,
    locale: Locale,
    onDateSelected: (Calendar) -> Unit
) {
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthStartCal = calendar.clone() as Calendar
    monthStartCal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = monthStartCal.get(Calendar.DAY_OF_WEEK)
    
    val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")
    
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val rows = (daysInMonth + firstDayOfWeek - 2) / 7 + 1
        val dateSdf = SimpleDateFormat("yyyy-MM-dd", locale)

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 1..7) {
                    val dayNum = row * 7 + col - firstDayOfWeek + 1
                    if (dayNum in 1..daysInMonth) {
                        val dayCal = calendar.clone() as Calendar
                        dayCal.set(Calendar.DAY_OF_MONTH, dayNum)
                        
                        val isSelected = isSameDay(dayCal, selectedDate)
                        val isToday = isSameDay(dayCal, Calendar.getInstance())
                        
                        val events = IIESTAcademicCalendar.getEventsForDate(dateSdf.format(dayCal.time))
                        val isHoliday = events.any { it.type == IIESTAcademicCalendar.EventType.HOLIDAY || it.type == IIESTAcademicCalendar.EventType.FEST }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        isHoliday -> Color(0xFFF97316).copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onDateSelected(dayCal) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = when {
                                        isSelected -> Color.White
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (isHoliday && !isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFD97706))
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun CalendarClassItem(
    entry: TimetableEntry,
    attendance: com.example.data.PeriodAttendance?,
    isDisabled: Boolean,
    onMarkStatus: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.subjectName, 
                        fontWeight = FontWeight.Black, 
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${entry.startTime} - ${entry.endTime}", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (attendance != null) {
                    val color = when(attendance.status) {
                        "PRESENT" -> Color(0xFF10B981)
                        "ABSENT" -> Color(0xFFEF4444)
                        "NOT_CONDUCTED" -> Color(0xFFF59E0B)
                        "HOLIDAY" -> Color(0xFFD97706)
                        else -> Color.Gray
                    }
                    Surface(
                        color = color.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = attendance.status.replace("_", " "),
                            color = color,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            if (!isDisabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusButton(
                        text = "Present",
                        isActive = attendance?.status == "PRESENT",
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        onClick = { onMarkStatus("PRESENT") }
                    )
                    StatusButton(
                        text = "Absent",
                        isActive = attendance?.status == "ABSENT",
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f),
                        onClick = { onMarkStatus("ABSENT") }
                    )
                    StatusButton(
                        text = "Cancelled",
                        isActive = attendance?.status == "NOT_CONDUCTED",
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1.2f),
                        onClick = { onMarkStatus("NOT_CONDUCTED") }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusButton(
    text: String,
    isActive: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) color else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        border = if (!isActive) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = text, 
                fontSize = 11.sp, 
                fontWeight = FontWeight.ExtraBold,
                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
