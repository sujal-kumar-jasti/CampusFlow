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
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HistoryEdu
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.util.IIESTAcademicCalendar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicCalendarScreen(initialDate: String? = null, onNavigateBack: () -> Unit) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val dateSdf = remember { SimpleDateFormat("yyyy-MM-dd", locale) }
    
    var calendar by remember { 
        val cal = Calendar.getInstance()
        initialDate?.let { 
            try { cal.time = dateSdf.parse(it)!! } catch (_: Exception) {}
        }
        mutableStateOf(cal) 
    }
    
    var selectedDate by remember { 
        val cal = Calendar.getInstance()
        initialDate?.let { 
            try { cal.time = dateSdf.parse(it)!! } catch (_: Exception) {}
        }
        mutableStateOf(cal) 
    }

    LaunchedEffect(initialDate) {
        initialDate?.let { dateStr ->
            try {
                val date = dateSdf.parse(dateStr)!!
                val newCal = Calendar.getInstance().apply { time = date }
                calendar = newCal.clone() as Calendar
                selectedDate = newCal
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(initialDate) {
        initialDate?.let { dateStr ->
            try {
                val date = dateSdf.parse(dateStr)!!
                val newCal = Calendar.getInstance().apply { time = date }
                calendar = newCal.clone() as Calendar
                selectedDate = newCal
            } catch (_: Exception) {}
        }
    }
    
    val currentMonthName = SimpleDateFormat("MMMM yyyy", locale).format(calendar.time)
    val selectedDateStr = dateSdf.format(selectedDate.time)
    val dayOfWeekName = SimpleDateFormat("EEEE", locale).format(selectedDate.time)
    
    val eventsForDay = remember(selectedDateStr) { IIESTAcademicCalendar.getEventsForDate(selectedDateStr) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Academic Schedule", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                ,windowInsets = WindowInsets(top = 28.dp)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Month Selector
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                            fontSize = 14.sp,
                            letterSpacing = 1.sp,
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
                AcademicMonthGrid(
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
            
            if (eventsForDay.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No academic events for this day.", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(eventsForDay) { event ->
                    AcademicEventItem(event)
                }
            }
        }
    }
}

@Composable
fun AcademicMonthGrid(
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
                        
                        val dayEvents = IIESTAcademicCalendar.getEventsForDate(dateSdf.format(dayCal.time))
                        val hasEvent = dayEvents.isNotEmpty()

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
                                if (hasEvent && !isSelected) {
                                    Row(horizontalArrangement = Arrangement.Center) {
                                        dayEvents.take(3).forEach { event ->
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 1.dp)
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(getEventColor(event.type))
                                            )
                                        }
                                    }
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

@Composable
fun AcademicEventItem(event: IIESTAcademicCalendar.CalendarEvent) {
    val color = getEventColor(event.type)
    
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top=10.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = when(event.type) {
                        IIESTAcademicCalendar.EventType.HOLIDAY -> Icons.Default.WbSunny
                        IIESTAcademicCalendar.EventType.EXAM -> Icons.Default.HistoryEdu
                        IIESTAcademicCalendar.EventType.FEST -> Icons.Default.Celebration
                        IIESTAcademicCalendar.EventType.REGISTRATION -> Icons.Default.AppRegistration
                        else -> Icons.Default.Event
                    },
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(event.name, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text(event.type.name, fontSize = 11.sp, fontWeight = FontWeight.Black, color = color.copy(alpha = 0.8f))
            }
        }
    }
}

fun getEventColor(type: IIESTAcademicCalendar.EventType): Color {
    return when(type) {
        IIESTAcademicCalendar.EventType.HOLIDAY -> Color(0xFFF59E0B)
        IIESTAcademicCalendar.EventType.EXAM -> Color(0xFFEF4444)
        IIESTAcademicCalendar.EventType.FEST -> Color(0xFF8B5CF6)
        IIESTAcademicCalendar.EventType.REGISTRATION -> Color(0xFF10B981)
        IIESTAcademicCalendar.EventType.ADMIN -> Color(0xFF3B82F6)
        else -> Color.Gray
    }
}
