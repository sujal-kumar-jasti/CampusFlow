package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.util.AutoSilentManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.CalculatorViewModel
import com.example.ui.ScannerViewModel
import com.example.ui.screens.*
import com.example.ui.theme.CampusCompanionTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val calculatorViewModel: CalculatorViewModel by viewModels()
    private val scannerViewModel: ScannerViewModel by viewModels()
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationSettingsLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("CampusPulse", "[Location] GPS resolution successful. Fetching...")
            viewModel.fetchCurrentLocationAndDetectCampus(this)
        } else {
            Log.w("CampusPulse", "[Location] GPS resolution cancelled or failed.")
        }
    }

    private val periodicHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val periodicCheckRunnable = object : Runnable {
        override fun run() {
            triggerPeriodicLocationAndSilentCheck(uiOnly = false)
            periodicHandler.postDelayed(this, 600000L) // Every 10 minutes
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkAndEnableGps()
        observeActiveClassState()
        
        com.example.util.NotificationHelper.createNotificationChannels(this)
        
        // IMMEDIATE FETCH ON STARTUP
        viewModel.fetchCurrentLocationAndDetectCampus(this)
        
        periodicHandler.post(periodicCheckRunnable)
        com.example.util.BackgroundScheduleManager.triggerImmediateCheck(this)

        val destinationExtra = intent.getStringExtra("destination")
        val autoConfirmExtra = intent.getBooleanExtra("auto_confirm", false)
        if (autoConfirmExtra) {
            handleAutoConfirm()
        }

        setContent {
            CampusCompanionTheme {
                CampusCompanionApp(
                    viewModel = viewModel,
                    calculatorViewModel = calculatorViewModel,
                    scannerViewModel = scannerViewModel,
                    initialDestination = destinationExtra
                )
            }
        }
    }

    private fun handleAutoConfirm() {
        lifecycleScope.launch {
            viewModel.activeClassState.first { it.isInitialized }.currentClass?.let {
                viewModel.confirmAttendanceManually(it.id)
            }
        }
    }

    private fun observeActiveClassState() {
        var lastState: AutoSilentManager.ActiveClassState? = null
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.systemActiveClassState.collect { state ->
                    if (!state.isInitialized) return@collect
                    
                    // Only apply if the core state that affects sound has changed
                    // Moving lastState check here ensures it persists across lifecycle pauses
                    if (lastState?.isClassActive != state.isClassActive || 
                        lastState?.currentClass?.id != state.currentClass?.id) {
                        
                        Log.d("CampusPulse", "[Sound] State changed. active=${state.isClassActive}")
                        com.example.util.AutoSilentManager.applySoundModeForClass(this@MainActivity, state)
                        lastState = state
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        periodicHandler.removeCallbacks(periodicCheckRunnable)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshActiveClassState()
        // One-shot fetch on resume instead of continuous tracking to save battery
        viewModel.fetchCurrentLocationAndDetectCampus(this)
        com.example.util.BackgroundScheduleManager.triggerImmediateCheck(this)
    }

    override fun onPause() {
        super.onPause()
    }

    private fun checkAndEnableGps() {
        val request = com.google.android.gms.location.LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).build()
        val builder = com.google.android.gms.location.LocationSettingsRequest.Builder().addLocationRequest(request)
        com.google.android.gms.location.LocationServices.getSettingsClient(this)
            .checkLocationSettings(builder.build())
            .addOnFailureListener { e ->
                if (e is com.google.android.gms.common.api.ResolvableApiException) {
                    try {
                        val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(e.resolution).build()
                        locationSettingsLauncher.launch(intentSenderRequest)
                    } catch (sendEx: android.content.IntentSender.SendIntentException) {
                        sendEx.printStackTrace()
                    }
                }
            }
    }

    private fun triggerPeriodicLocationAndSilentCheck(uiOnly: Boolean) {
        try {
            viewModel.refreshActiveClassState()
            
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // AGGRESSIVE FRESH FIX
                val cts = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            Log.d("CampusPulse", "[Location] Aggressive Fix: Lat=${location.latitude}, Lon=${location.longitude}")
                            viewModel.updateGpsLocation(location.latitude, location.longitude, uiOnly = uiOnly)
                        } else {
                            // Instant fallback to last known if current fix is null
                            fusedLocationClient.lastLocation.addOnSuccessListener { last ->
                                last?.let {
                                    Log.d("CampusPulse", "[Location] Last Known Fallback: Lat=${it.latitude}, Lon=${it.longitude}")
                                    viewModel.updateGpsLocation(it.latitude, it.longitude, uiOnly = uiOnly)
                                }
                            }
                        }
                    }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}

sealed class NavItem(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Dashboard : NavItem("dashboard", "Home", Icons.Default.Home, Icons.Outlined.Home)
    object Attendance : NavItem("attendance", "Attend", Icons.Default.PinDrop, Icons.Outlined.PinDrop)
    object Timetable : NavItem("timetable", "Timetable", Icons.Default.EditCalendar, Icons.Outlined.EditCalendar)
    object Calculator : NavItem("calculator", "Calculator", Icons.Default.Calculate, Icons.Outlined.Calculate)
    object Scanner : NavItem("scanner", "Scanner", Icons.Default.DocumentScanner, Icons.Outlined.DocumentScanner)
}

@Composable
fun ModernPillNavigationBar(
    currentRoute: String,
    onItemSelected: (String) -> Unit
) {
    val navItems = listOf(
        NavItem.Dashboard,
        NavItem.Attendance,
        NavItem.Timetable,
        NavItem.Calculator,
        NavItem.Scanner
    )

    val isDark = isSystemInDarkTheme()
    val navColor = if (isDark) {
                Color(0xFFEDE6FA).copy(alpha = 0.20f)
    } else {

                Color(0xFFEDE6FA).copy(alpha = 0.40f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
            .height(72.dp),
        shape = RoundedCornerShape(36.dp),
        color = if (isDark) Color.Black else Color.White,
        shadowElevation = if (isDark) 0.dp else 12.dp,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(navColor)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = currentRoute == item.route
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .clickable { onItemSelected(item.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title,
                            modifier = Modifier.size(24.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary 
                                   else if (isDark) Color.White.copy(alpha = 0.6f)
                                   else Color(0xFF64748B)
                        )
                        Text(
                            text = item.title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary 
                                   else if (isDark) Color.White.copy(alpha = 0.6f)
                                   else Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CampusCompanionApp(
    viewModel: MainViewModel,
    calculatorViewModel: CalculatorViewModel,
    scannerViewModel: ScannerViewModel,
    initialDestination: String? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavItem.Dashboard.route

    LaunchedEffect(initialDestination) {
        if (!initialDestination.isNullOrBlank()) {
            navController.navigate(initialDestination) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController, 
                startDestination = NavItem.Dashboard.route,
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
            ) {
                fun navigateToTab(route: String) { navController.navigate(route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }

                composable(NavItem.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToAttendance = { navigateToTab(NavItem.Attendance.route) },
                        onNavigateToReminders = { navigateToTab("reminders_full") },
                        onNavigateToTimetable = { navigateToTab(NavItem.Timetable.route) },
                        onNavigateToScanner = { navigateToTab(NavItem.Scanner.route) },
                        onNavigateToAcademicCalendar = { date -> 
                            val route = if (date != null) "academic_calendar?date=$date" else "academic_calendar"
                            navController.navigate(route)
                        }
                    )
                }
                composable(NavItem.Attendance.route) { 
                    AttendanceScreen(
                        viewModel,
                        onNavigateToCalendar = { navController.navigate("attendance_calendar") }
                    ) 
                }
                composable("attendance_calendar") { AttendanceCalendarScreen(viewModel, onNavigateBack = { navController.popBackStack() }) }
                composable(NavItem.Timetable.route) { TimetableScreen(viewModel) }
                composable(NavItem.Calculator.route) { CalculatorScreen(calculatorViewModel) }
                composable(NavItem.Scanner.route) { ScannerScreen(scannerViewModel, onNavigateToEdit = { navController.navigate("scan_edit") }) }
                composable("scan_edit") { ScanEditScreen(scannerViewModel, onNavigateBack = { navController.popBackStack() }) }
                composable("reminders_full") { RemindersScreen(viewModel) }
                composable("academic_calendar?date={date}") { backStackEntry ->
                    val date = backStackEntry.arguments?.getString("date")
                    AcademicCalendarScreen(
                        initialDate = date,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                val mainRoutes = listOf(
                    NavItem.Dashboard.route,
                    NavItem.Attendance.route,
                    NavItem.Timetable.route,
                    NavItem.Calculator.route,
                    NavItem.Scanner.route
                )
                
                if (currentRoute in mainRoutes) {
                    ModernPillNavigationBar(
                        currentRoute = currentRoute,
                        onItemSelected = { route ->
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
