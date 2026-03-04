package com.nac45.farmpollutionmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import com.mapbox.common.MapboxOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.nac45.farmpollutionmonitor.ui.theme.FarmPollutionMonitorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN

        setContent {
            FarmPollutionMonitorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selectedItem by remember { mutableStateOf(0) }
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(-4.0, 52.0)) // Centered on Wales
            zoom(8.0)
            pitch(0.0)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedItem == 0,
                    onClick = { selectedItem = 0 },
                    icon = {
                        Icon(
                            if (selectedItem == 0) Icons.Filled.Map else Icons.Outlined.Map,
                            contentDescription = "Map"
                        )
                    },
                    label = { Text("Map") }
                )
                NavigationBarItem(
                    selected = selectedItem == 1,
                    onClick = { selectedItem = 1 },
                    icon = {
                        Icon(
                            if (selectedItem == 1) Icons.Filled.List else Icons.Outlined.List,
                            contentDescription = "Data"
                        )
                    },
                    label = { Text("Data") }
                )
                NavigationBarItem(
                    selected = selectedItem == 2,
                    onClick = { selectedItem = 2 },
                    icon = {
                        Icon(
                            if (selectedItem == 2) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Profile") }
                )
            }
        }
    ) { paddingValues ->
        when (selectedItem) {
            0 -> MapScreen(mapViewportState, paddingValues)
            1 -> DataScreen(paddingValues)
            2 -> ProfileScreen(paddingValues)
        }
    }
}

@Composable
fun MapScreen(mapViewportState: MapViewportState, paddingValues: PaddingValues) {
    Box(modifier = Modifier.padding(paddingValues)) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = { MapStyle(style = Style.MAPBOX_STREETS) }
        ) {
            // Water markers maybe go here
        }

        // Floating action button to add test marker
        FloatingActionButton(
            onClick = { /* TODO: Add marker */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Marker")
        }
    }
}

/**
 * Data Screen, shows all of my 'lake markers' though they are all fake dummy ones right now.
 */
@Composable
fun DataScreen(paddingValues: PaddingValues) {
    // Sample data for water monitoring sites
    val monitoringSites = listOf(
        MonitoringSite("River Teifi", "Phosphorus: 0.12 mg/L", "Nitrates: 4.5 mg/L", "Status: Good", Icons.Default.Water), // Eventually change status attributes to Enum,
        MonitoringSite("Llyn Brianne", "Phosphorus: 0.08 mg/L", "Nitrates: 2.1 mg/L", "Status: Excellent", Icons.Default.Water), // Just getting the initial idea down!
        MonitoringSite("Afon Rheidol", "Phosphorus: 0.25 mg/L", "Nitrates: 8.7 mg/L", "Status: Moderate", Icons.Default.Warning),
        MonitoringSite("River Ystwyth", "Phosphorus: 0.18 mg/L", "Nitrates: 6.2 mg/L", "Status: Good", Icons.Default.Water),
        MonitoringSite("Llyn Syfydrin", "Phosphorus: 0.05 mg/L", "Nitrates: 1.8 mg/L", "Status: Excellent", Icons.Default.Water),
        MonitoringSite("Afon Clarach", "Phosphorus: 0.32 mg/L", "Nitrates: 12.4 mg/L", "Status: Poor", Icons.Default.Error)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(top = 0.dp)  // Added this line to remove extra top padding
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Water Monitoring Sites",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Ceredigion, Wales",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Summary Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryCard("Total Sites", "${monitoringSites.size}", Icons.Default.Place, modifier = Modifier.weight(1f))
            SummaryCard("Active Sensors", "6", Icons.Default.Sensors, modifier = Modifier.weight(1f))
            SummaryCard("Last Update", "2 min ago", Icons.Default.Schedule, modifier = Modifier.weight(1f))
        }

        // List of sites - Takes remaining space and scrolls (This is my lazy column bits and they are stuck at the bottom of the screen,now solved bc .fillMaxWidth() *I believe* was addding padding
        // for ALL of my sites therefore they got pushed to the bottom of the screen.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp)  // Moved padding here
        ) {
            items(monitoringSites) { site ->
                MonitoringSiteCard(site)
            }
        }
    }
}

/**
 * Profile Screen with user info
 */
@Composable
fun ProfileScreen(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Farmer Nate", // Change these to an xml string file/ link with actual data in future!
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "managerOfBestFarm@farmcoop.cymru",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Account Settings Cards
        SettingsCard(
            icon = Icons.Default.Person,
            title = "Personal Information",
            subtitle = "Name, email, phone"
        )

        SettingsCard(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            subtitle = "Alert preferences"
        )

        SettingsCard(
            icon = Icons.Default.LocationOn,
            title = "Farm Locations",
            subtitle = "Manage monitored sites"
        )

        SettingsCard(
            icon = Icons.Default.Security,
            title = "Privacy & Security",
            subtitle = "App permissions, data sharing"
        )

        SettingsCard(
            icon = Icons.Default.Help,
            title = "Help & Support",
            subtitle = "FAQs, contact support"
        )

        Spacer(modifier = Modifier.weight(1f))

        // Logout Button
        Button(
            onClick = { /* TODO: Logout */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out")
        }
    }
}

// HELPER COMPOSABLES

data class MonitoringSite(
    val name: String,
    val phosphorusLevel: String,
    val nitrateLevel: String,
    val status: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun MonitoringSiteCard(site: MonitoringSite) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    site.icon,
                    contentDescription = null,
                    tint = when { // Change later on so its not written out 3 times
                        site.status.contains("Excellent") -> Color.Green
                        site.status.contains("Good") -> Color.Blue
                        site.status.contains("Moderate") -> Color(0xFFFFA500) // Orange
                        else -> Color.Red
                    }
                )

                Column {
                    Text(
                        text = site.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = site.phosphorusLevel,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = site.nitrateLevel,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = when {
                    site.status.contains("Excellent") -> Color.Green.copy(alpha = 0.2f)
                    site.status.contains("Good") -> Color.Blue.copy(alpha = 0.2f)
                    site.status.contains("Moderate") -> Color(0xFFFFA500).copy(alpha = 0.2f)
                    else -> Color.Red.copy(alpha = 0.2f)
                }
            ) {
                Text(
                    text = site.status,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        site.status.contains("Excellent") -> Color.Green
                        site.status.contains("Good") -> Color.Blue
                        site.status.contains("Moderate") -> Color(0xFFFFA500)
                        else -> Color.Red
                    }
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null)
            Text(text = value, style = MaterialTheme.typography.titleLarge)
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = { /* TODO: Navigate to setting */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null)
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Navigate")
        }
    }
}