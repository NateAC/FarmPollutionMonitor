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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.mapbox.common.MapboxOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.viewannotation.geometry
import com.nac45.farmpollutionmonitor.ui.theme.FarmPollutionMonitorTheme
 import com.mapbox.maps.extension.compose.MapEffect
 import com.mapbox.maps.extension.style.layers.addLayer
 import com.mapbox.maps.extension.style.layers.generated.fillLayer
 import com.mapbox.maps.extension.style.layers.generated.lineLayer
 import com.mapbox.maps.extension.style.sources.addSource
 import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
 import com.mapbox.maps.plugin.gestures.addOnMapClickListener
 import com.mapbox.geojson.Feature
 import com.mapbox.geojson.FeatureCollection
 import com.mapbox.maps.QueriedRenderedFeature
 import androidx.compose.material3.BottomSheetDefaults
 import androidx.compose.material3.ExperimentalMaterial3Api
 import androidx.compose.material3.ModalBottomSheet
 import androidx.compose.material3.rememberModalBottomSheetState
 import androidx.compose.ui.text.font.FontWeight

// Source and layer IDs, kept as constants so they're consistent between adding them and querying them on tap
private const val LAKES_SOURCE_ID  = "wfd-lakes-source"
private const val RIVERS_SOURCE_ID = "wfd-rivers-source"
private const val LAKES_FILL_LAYER = "wfd-lakes-fill"
private const val LAKES_LINE_LAYER = "wfd-lakes-outline"
private const val RIVERS_LINE_LAYER = "wfd-rivers-line"

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

/**
 * Map screen showing all monitoring sites as markers loaded
 * from the Supabase server. Tapping the floating button previously allowed manual
 * coordinate entry, I will repurpose later for adding new sites directly to the database.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(mapViewportState: MapViewportState, paddingValues: PaddingValues) {
    val mapViewModel: MapViewModel = viewModel()
    val wfdViewModel: WfdViewModel = viewModel()

    var showDialog by remember { mutableStateOf(false) }
    var latInput by remember { mutableStateOf("") }
    var lngInput by remember { mutableStateOf("") }

    // Bottom sheet state, shown when user taps a WFD waterbody
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    Box(modifier = Modifier.padding(paddingValues)) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = { MapStyle(style = Style.MAPBOX_STREETS) }
        ) {
            // Monitoring site markers (from Supabase)
            mapViewModel.sites.forEach { site ->
                val point = Point.fromLngLat(site.longitude, site.latitude)
                ViewAnnotation(
                    options = viewAnnotationOptions {
                        geometry(point)
                        allowOverlap(true)
                    }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = site.name,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Icon(
                            Icons.Default.Water,
                            contentDescription = site.name,
                            tint = Color.Red,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // WFD GeoJSON layers via MapEffect
            MapEffect(
                key1 = wfdViewModel.lakesGeoJson,
                key2 = wfdViewModel.riversGeoJson
            ) { mapView ->
                mapView.mapboxMap.addOnStyleLoadedListener {
                    val style = mapView.mapboxMap.style ?: return@addOnStyleLoadedListener

                    // ── Lakes: filled polygons ──
                    wfdViewModel.lakesGeoJson?.let { geojson ->
                        // Remove old source/layers if refreshing
                        if (style.styleSourceExists(LAKES_SOURCE_ID)) {
                            style.removeStyleLayer(LAKES_FILL_LAYER)
                            style.removeStyleLayer(LAKES_LINE_LAYER)
                            style.removeStyleSource(LAKES_SOURCE_ID)
                        }

                        style.addSource(
                            geoJsonSource(LAKES_SOURCE_ID) { data(geojson) }
                        )

                        // Semi-transparent fill coloured by OverallStatus
                        style.addLayer(
                            fillLayer(LAKES_FILL_LAYER, LAKES_SOURCE_ID) {
                                fillColor(
                                    com.mapbox.maps.extension.style.expressions.dsl.generated.match {
                                        get { literal("OverallStatus") }
                                        literal("High");     color(android.graphics.Color.parseColor("#2196F3"))
                                        literal("Good");     color(android.graphics.Color.parseColor("#4CAF50"))
                                        literal("Moderate"); color(android.graphics.Color.parseColor("#FF9800"))
                                        literal("Poor");     color(android.graphics.Color.parseColor("#F44336"))
                                        literal("Bad");      color(android.graphics.Color.parseColor("#9C27B0"))
                                        color(android.graphics.Color.parseColor("#9E9E9E"))
                                    }
                                )
                                fillOpacity(0.4)
                            }
                        )

                        // Solid outline so the lake shape is clear even at low opacity
                        style.addLayer(
                            lineLayer(LAKES_LINE_LAYER, LAKES_SOURCE_ID) {
                                lineColor("#000000")
                                lineOpacity(0.3)
                                lineWidth(1.0)
                            }
                        )
                    }

                    // Rivers: coloured polylines
                    wfdViewModel.riversGeoJson?.let { geojson ->
                        if (style.styleSourceExists(RIVERS_SOURCE_ID)) {
                            style.removeStyleLayer(RIVERS_LINE_LAYER)
                            style.removeStyleSource(RIVERS_SOURCE_ID)
                        }

                        style.addSource(
                            geoJsonSource(RIVERS_SOURCE_ID) { data(geojson) }
                        )

                        style.addLayer(
                            lineLayer(RIVERS_LINE_LAYER, RIVERS_SOURCE_ID) {
                                lineColor(
                                    com.mapbox.maps.extension.style.expressions.dsl.generated.match {
                                        get { literal("OverallStatus") }
                                        literal("High");     color(android.graphics.Color.parseColor("#2196F3"))
                                        literal("Good");     color(android.graphics.Color.parseColor("#4CAF50"))
                                        literal("Moderate"); color(android.graphics.Color.parseColor("#FF9800"))
                                        literal("Poor");     color(android.graphics.Color.parseColor("#F44336"))
                                        literal("Bad");      color(android.graphics.Color.parseColor("#9C27B0"))
                                        color(android.graphics.Color.parseColor("#9E9E9E"))
                                    }
                                )
                                lineWidth(3.0)
                                lineOpacity(0.8)
                            }
                        )
                    }
                }

                // Tap listener for WFD features
                // Queries rendered features at the tap point and checks if any belong to the WFD layers. If so, show the bottom sheet
                mapView.mapboxMap.addOnMapClickListener { point ->
                    mapView.mapboxMap.queryRenderedFeatures(
                        com.mapbox.maps.RenderedQueryGeometry(
                            com.mapbox.maps.ScreenCoordinate(
                                mapView.mapboxMap.pixelForCoordinate(point).x,
                                mapView.mapboxMap.pixelForCoordinate(point).y
                            )
                        ),
                        com.mapbox.maps.RenderedQueryOptions(
                            listOf(LAKES_FILL_LAYER, RIVERS_LINE_LAYER), null
                        )
                    ) { result ->
                        val features = result.value ?: return@queryRenderedFeatures
                        if (features.isNotEmpty()) {
                            val feature = features.first().queriedFeature.feature
                            val props = feature.properties() ?: return@queryRenderedFeatures

                            // Determine if this is a lake or river by which layer it came from
                            val layerId = features.first().layers.firstOrNull()
                            val type = if (layerId == LAKES_FILL_LAYER) WaterbodyType.LAKE else WaterbodyType.RIVER

                            wfdViewModel.onWaterbodyTapped(
                                WaterbodyProperties(
                                    name = props.get("WB_NAME")?.asString?.trim() ?: "Unknown",
                                    wbid = props.get("WBID")?.asString?.trim() ?: "",
                                    overallStatus = props.get("OverallStatus")?.asString?.trim(),
                                    ecoStatus = props.get("EcoStatus")?.asString?.trim(),
                                    chemStatus = props.get("ChemStatus")?.asString?.trim(),
                                    dissolvedOxygen = props.get("DO")?.asString?.trim(),
                                    totalPhosphorus = props.get("Total_P")?.asString?.trim(),
                                    sqKms = props.get("SqKms")?.asString?.trim(),
                                    region = props.get("Region")?.asString?.trim(),
                                    type = type
                                )
                            )
                        }
                    }
                    false // Return false so Mapbox still handles the event normally
                }
            }
        }

        // Loading indicator for WFD data
        if (wfdViewModel.isLoadingLakes || wfdViewModel.isLoadingRivers) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text("Loading WFD data...", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Error Snackbar
        wfdViewModel.errorMessage?.let { error ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Floating Action Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Refresh WFD data button
            SmallFloatingActionButton(onClick = { wfdViewModel.refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh WFD layers")
            }
            // Existing add site button
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Monitoring Site")
            }
        }

        // Site Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Add Monitoring Site") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latInput,
                            onValueChange = { latInput = it },
                            label = { Text("Latitude") },
                            placeholder = { Text("e.g. 52.4153") }
                        )
                        OutlinedTextField(
                            value = lngInput,
                            onValueChange = { lngInput = it },
                            label = { Text("Longitude") },
                            placeholder = { Text("e.g. -4.0829") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val lat = latInput.toDoubleOrNull()
                        val lng = lngInput.toDoubleOrNull()
                        if (lat != null && lng != null) {
                            showDialog = false
                            latInput = ""
                            lngInput = ""
                        }
                    }) { Text("Add") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDialog = false }) { Text("Cancel") }
                }
            )
        }
    }

    // WFD Waterbody detail bottom sheet
    // Sits outside the Box so it renders over the whole screen properly
    wfdViewModel.selectedWaterbody?.let { waterbody ->
        ModalBottomSheet(
            onDismissRequest = { wfdViewModel.onBottomSheetDismissed() },
            sheetState = bottomSheetState
        ) {
            WaterbodyBottomSheet(waterbody = waterbody)
        }
    }
}

/**
 * Bottom sheet content shown when a WFD lake or river is tapped.
 * Shows the key WFD classification fields from the dataset.
 */
@Composable
fun WaterbodyBottomSheet(waterbody: WaterbodyProperties) {
    val statusColor = when (waterbody.overallStatus?.trim()) {
        "High"     -> Color(0xFF2196F3)
        "Good"     -> Color(0xFF4CAF50)
        "Moderate" -> Color(0xFFFF9800)
        "Poor"     -> Color(0xFFF44336)
        "Bad"      -> Color(0xFF9C27B0)
        else       -> Color(0xFF9E9E9E)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = waterbody.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (waterbody.type == WaterbodyType.LAKE) "WFD Lake Waterbody" else "WFD River Waterbody",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            // Overall status badge
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = waterbody.overallStatus ?: "Unknown",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        HorizontalDivider()

        // WFD classification breakdown
        Text(
            text = "WFD Classification",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WfdStatItem(label = "Overall", value = waterbody.overallStatus ?: "—")
            WfdStatItem(label = "Ecological", value = waterbody.ecoStatus ?: "—")
            WfdStatItem(label = "Chemical", value = waterbody.chemStatus ?: "—")
        }

        // Lake-specific fields
        if (waterbody.type == WaterbodyType.LAKE) {
            HorizontalDivider()
            Text(
                text = "Water Quality Indicators",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WfdStatItem(label = "Dissolved O₂", value = waterbody.dissolvedOxygen ?: "—")
                WfdStatItem(label = "Total Phosphorus", value = waterbody.totalPhosphorus ?: "—")
                WfdStatItem(label = "Area (km²)", value = waterbody.sqKms ?: "—")
            }
        }

        HorizontalDivider()

        // Metadata
        waterbody.region?.let {
            Text(
                text = "Region: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        Text(
            text = "Waterbody ID: ${waterbody.wbid}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = "Source: Natural Resources Wales — WFD Cycle 2",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun WfdStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Data Screen, shows all of my 'lake markers' though they are all fake dummy ones right now.
 */
@Composable
fun DataScreen(paddingValues: PaddingValues) {
    // viewModel() creates or retrieves the ViewModel for this screen
    val viewModel: DataViewModel = viewModel()
    // Track which card is expanded, null means none are expanded
    var expandedSiteId by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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

        // Show cool loading spinner while data is being fetched!
        if (viewModel.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryCard("Total Sites", "${viewModel.sites.size}", Icons.Default.Place, modifier = Modifier.weight(1f))
                SummaryCard("Active Sensors", "${viewModel.sites.size}", Icons.Default.Sensors, modifier = Modifier.weight(1f))
                SummaryCard("Data Points", "5281", Icons.Default.Schedule, modifier = Modifier.weight(1f))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp)
            ) {
                items(viewModel.sites) { site ->
                    // Get most recent daily summary for this site if it exists (Black Covert)
                    val latestSummary = viewModel.dailySummaries
                        .filter { it.site_id == site.id }
                        .maxByOrNull { it.date }

                    // Get water quality reading for dummy sites
                    val reading = viewModel.readings
                        .find { it.site_id == site.id }

                    SensorSiteCard(
                        site = site,
                        summary = latestSummary,
                        reading = reading,
                        isExpanded = expandedSiteId == site.id,
                        onCardClick = {
                            // Toggle expanded, tap it again to collapse
                            expandedSiteId = if (expandedSiteId == site.id) null else site.id
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun SensorSiteCard(
    site: MonitoringSiteDB,
    summary: DailySummary?,
    reading: WaterQualityReading?,
    isExpanded: Boolean,
    onCardClick: () -> Unit
) {
    // Use real sensor data for status if available, otherwise use dummy data
    val statusColor = when {
        summary != null && summary.avg_ph in 6.5..8.5 && summary.avg_do_mgl > 7.0 -> Color.Green
        summary != null && summary.avg_ph in 6.0..9.0 && summary.avg_do_mgl > 5.0 -> Color(0xFFFFA500)
        summary != null -> Color.Red
        reading?.status == "Excellent" -> Color.Green
        reading?.status == "Good" -> Color.Blue
        reading?.status == "Moderate" -> Color(0xFFFFA500)
        reading?.status == "Poor" -> Color.Red
        else -> Color.Gray
    }

    val statusText = when {
        summary != null && summary.avg_ph in 6.5..8.5 && summary.avg_do_mgl > 7.0 -> "Good"
        summary != null && summary.avg_ph in 6.0..9.0 && summary.avg_do_mgl > 5.0 -> "Moderate"
        summary != null -> "Poor"
        reading != null -> reading.status
        else -> "No Data"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onCardClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Water,
                        contentDescription = null,
                        tint = statusColor
                    )
                    Column {
                        Text(
                            text = site.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = site.region,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = statusColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Show real sensor data for Black Covert
            if (summary != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStatCard("pH", "${summary.avg_ph}")
                    MiniStatCard("Temp", "${summary.avg_temperature}°C")
                    MiniStatCard("DO", "${summary.avg_do_mgl} mg/L")
                }
                // Show dummy phosphorus/nitrates for Welsh sites
            } else if (reading != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStatCard("Phosphorus", "${reading.phosphorus} mg/L")
                    MiniStatCard("Nitrates", "${reading.nitrates} mg/L")
                    MiniStatCard("Status", reading.status)
                }
            }

            // Expanded section
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                if (summary != null) {
                    Text(
                        text = "Latest Daily Average: ${summary.date}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MiniStatCard("DO Sat", "${summary.avg_do_sat}%")
                        MiniStatCard("EC", "${summary.avg_ec} µS")
                        MiniStatCard("Depth", "${summary.avg_depth}m")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Based on ${summary.reading_count} sensor readings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lat: ${site.latitude}, Lng: ${site.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun MiniStatCard(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
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