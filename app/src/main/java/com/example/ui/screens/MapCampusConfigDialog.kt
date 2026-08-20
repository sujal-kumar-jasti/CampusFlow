package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.GpsLocationHelper
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.cos

@Composable
fun MapCampusConfigDialog(
    currentLoc: GpsLocationHelper.CampusLocation,
    userLat: Double,
    userLon: Double,
    onDismiss: () -> Unit,
    onSave: (name: String, lat: Double, lon: Double, radius: Double) -> Unit,
    onFetchLocation: () -> Unit = {}
) {
    val locale = LocalConfiguration.current.locales[0]
    var name by remember { mutableStateOf(currentLoc.name) }
    
    val centerMarkerState = remember { MarkerState(position = LatLng(currentLoc.latitude, currentLoc.longitude)) }
    
    // Constant for converting meters to lat/lng offset approximately (at equator)
    val METERS_PER_DEGREE = 111320.0

    var radiusMeters by remember { mutableDoubleStateOf(currentLoc.radiusMeters) }
    
    val initialRadiusPos = LatLng(
        currentLoc.latitude, 
        currentLoc.longitude + (radiusMeters / (METERS_PER_DEGREE * cos(Math.toRadians(currentLoc.latitude))))
    )
    val radiusMarkerState = remember { MarkerState(position = initialRadiusPos) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.builder()
            .target(centerMarkerState.position)
            .zoom(17f)
            .tilt(0f) // Stable 2D view
            .build()
    }

    // Function to keep radius marker at the same distance when center moves
    fun syncRadiusMarker() {
        val currentCenter = centerMarkerState.position
        val offset = radiusMeters / (METERS_PER_DEGREE * cos(Math.toRadians(currentCenter.latitude)))
        radiusMarkerState.position = LatLng(currentCenter.latitude, currentCenter.longitude + offset)
    }

    // When center moves (drag or programmatic), edge marker follows to keep radius constant
    LaunchedEffect(centerMarkerState.position) {
        if (!radiusMarkerState.isDragging) {
            syncRadiusMarker()
        }
    }

    // Update radius value when radius marker is moved
    LaunchedEffect(radiusMarkerState.position) {
        val dist = GpsLocationHelper.calculateDistanceMeters(
            centerMarkerState.position.latitude, centerMarkerState.position.longitude,
            radiusMarkerState.position.latitude, radiusMarkerState.position.longitude
        )
        // Update radius based on current distance between markers
        radiusMeters = dist.coerceAtLeast(10.0)
    }

    // Move everything when map is clicked
    val mapClickUpdate: (LatLng) -> Unit = { latLng ->
        centerMarkerState.position = latLng
        val offset = radiusMeters / (METERS_PER_DEGREE * cos(Math.toRadians(latLng.latitude)))
        radiusMarkerState.position = LatLng(latLng.latitude, latLng.longitude + offset)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp), // Less padding for more map space
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Campus Geofence",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Campus Name") },
                    modifier = Modifier.fillMaxWidth().testTag("config_campus_name"),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        onMapClick = mapClickUpdate,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false, 
                            myLocationButtonEnabled = false,
                            tiltGesturesEnabled = false
                        ),
                        properties = MapProperties(
                            isMyLocationEnabled = userLat != 0.0,
                            isBuildingEnabled = true
                        )
                    ) {
                        Marker(
                            state = centerMarkerState,
                            title = "Campus Center",
                            snippet = "Tap or drag to move",
                            draggable = true
                        )

                        Marker(
                            state = radiusMarkerState,
                            title = "Radius Edge",
                            snippet = "Drag to resize",
                            draggable = true
                        )

                        Circle(
                            center = centerMarkerState.position,
                            radius = radiusMeters,
                            fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            strokeColor = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2f
                        )
                    }

                    // Map Overlay HUD for Coordinates
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = String.format(locale, "%.6f, %.6f", centerMarkerState.position.latitude, centerMarkerState.position.longitude),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Radius: ${radiusMeters.toInt()}m",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            if (userLat != 0.0) {
                                val userPos = LatLng(userLat, userLon)
                                centerMarkerState.position = userPos
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(userPos, 17f)
                            } else {
                                // Try to fetch if zero
                                onFetchLocation()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(24.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            onSave(name, centerMarkerState.position.latitude, centerMarkerState.position.longitude, radiusMeters)
                        },
                        modifier = Modifier.weight(1.5f).testTag("save_campus_config_btn"),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Save Geofence", fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    }
                }
            }
        }
    }
}
