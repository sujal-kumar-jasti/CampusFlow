package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Reminder
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(viewModel: MainViewModel) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    var showAddDialog by remember { mutableStateOf(false) }

    var titleInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("Assignment") }
    var dueDateInput by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", locale).format(Date())) }
    var dueTimeInput by remember { mutableStateOf("18:00") }
    var priorityInput by remember { mutableStateOf("HIGH") }

    val categories = listOf("Assignment", "Exam", "Lab", "General")
    val priorities = listOf("HIGH", "MEDIUM", "LOW")

    val completedCount = reminders.count { it.isCompleted }
    val pendingCount = reminders.size - completedCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Tasks & Reminders", 
                        fontWeight = FontWeight.ExtraBold, 
                        fontSize = 20.sp 
                    ) 
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "$pendingCount PENDING",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                windowInsets = WindowInsets(top = 30.dp)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_reminder_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Reminder")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ACADEMIC PLANNER", fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text("Stay organized with precision. Track assignments and exam deadlines.", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }

            if (reminders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Task, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No reminders set", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.outline)
                            Text("Tap '+' to schedule an assignment or exam alert", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            } else {
                items(reminders, key = { reminder: Reminder -> reminder.id }) { reminder ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminder_item_${reminder.id}"),
                        shape = RoundedCornerShape(28.dp), // Premium radius
                        color = if (reminder.isCompleted) MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp) 
                                else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.toggleReminderCompleted(reminder) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                    contentDescription = "Toggle Complete",
                                    tint = if (reminder.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = reminder.title,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (reminder.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = when(reminder.priority) {
                                            "HIGH" -> Color(0xFFEF4444).copy(alpha = 0.1f)
                                            "MEDIUM" -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        }
                                    ) {
                                        Text(
                                            text = reminder.priority,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = when(reminder.priority) {
                                                "HIGH" -> Color(0xFFDC2626)
                                                "MEDIUM" -> Color(0xFFD97706)
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    }
                                }

                                if (reminder.description.isNotBlank()) {
                                    Text(
                                        text = reminder.description, 
                                        fontSize = 12.sp, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Event, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(4.dp))
                                        Text("${reminder.dueDate} • ${reminder.dueTime}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), 
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = reminder.category.uppercase(), 
                                            fontSize = 9.sp, 
                                            fontWeight = FontWeight.Black, 
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = { viewModel.deleteReminder(reminder.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Academic Reminder", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Title (e.g. Physics Assignment 3)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Description / Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = categoryInput == cat,
                                onClick = { categoryInput = cat },
                                label = { Text(cat, fontSize = 11.sp, maxLines = 1) }
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text("Priority:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(priorities) { prio ->
                            FilterChip(
                                selected = priorityInput == prio,
                                onClick = { priorityInput = prio },
                                label = { Text(prio, fontSize = 11.sp, maxLines = 1) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dueDateInput,
                            onValueChange = { dueDateInput = it },
                            label = { Text("Due Date (yyyy-MM-dd)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dueTimeInput,
                            onValueChange = { dueTimeInput = it },
                            label = { Text("Time (HH:mm)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (titleInput.isNotBlank()) {
                            viewModel.addReminder(
                                Reminder(
                                    title = titleInput,
                                    description = descInput,
                                    category = categoryInput,
                                    dueDate = dueDateInput,
                                    dueTime = dueTimeInput,
                                    priority = priorityInput
                                )
                            )
                            titleInput = ""
                            descInput = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save Reminder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}
