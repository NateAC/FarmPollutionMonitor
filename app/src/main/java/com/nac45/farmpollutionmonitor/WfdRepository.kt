package com.nac45.farmpollutionmonitor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

// Ceredigion bounding box in WGS84 (lng_min, lat_min, lng_max, lat_max)
// Filters out the rest of Wales so we only load relevant water bodies, saves phone resources.
private const val CEREDIGION_BBOX = "-4.7,51.9,-3.6,52.7"

// Only fetch fields needed for the map layer (lazy loading)
// Full details are fetched separately when the user taps a specific waterbody
private const val LAKE_PROPERTIES = "WB_NAME,WBID,OverallStatus,EcoStatus,ChemStatus,DO,Total_P,SqKms,Region"
private const val RIVER_PROPERTIES = "WB_NAME,WBID,OverallStatus,EcoStatus,ChemStatus,Region"

private const val TAG = "WfdRepository"

/**
 * Fetches WFD Lake waterbody polygons from the DataMapWales WFS endpoint.
 * Returns raw GeoJSON string ready for Mapbox GeoJsonSource.
 * Uses Ceredigion bounding box to save resources.
 */
suspend fun fetchWfdLakes(): String? = withContext(Dispatchers.IO) {
    try {
        val url = buildWfsUrl(
            typeName = "inspire-nrw:NRW_WFD_LAKES_C2",
            properties = LAKE_PROPERTIES
        )
        Log.d(TAG, "Fetching WFD lakes from: $url")
        val result = URL(url).readText()
        Log.d(TAG, "WFD lakes fetched successfully")
        result
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching WFD lakes: ${e.message}")
        null
    }
}

/**
 * Fetches WFD River waterbody polylines from the DataMapWales WFS endpoint.
 * Returns raw GeoJSON string ready for Mapbox GeoJsonSource.
 */
suspend fun fetchWfdRivers(): String? = withContext(Dispatchers.IO) {
    try {
        val url = buildWfsUrl(
            typeName = "inspire-nrw:NRW_WFD_RIVERWATERBODIES_C1",
            properties = RIVER_PROPERTIES
        )
        Log.d(TAG, "Fetching WFD rivers from: $url")
        val result = URL(url).readText()
        Log.d(TAG, "WFD rivers fetched successfully")
        result
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching WFD rivers: ${e.message}")
        null
    }
}

/**
 * Builds the DataMapWales WFS GetFeature URL with:
 * - Ceredigion bounding box filter (for performance)
 * - EPSG:4326 coordinate system (lat/lng that Mapbox understands)
 * - Only the property fields we need (lazy loading principle)
 */
private fun buildWfsUrl(typeName: String, properties: String): String {
    return "https://datamap.gov.wales/geoserver/ows?" +
            "service=WFS" +
            "&version=1.0.0" +
            "&request=GetFeature" +
            "&typeName=$typeName" +
            "&outputFormat=json" +
            "&srsName=EPSG:4326" +
            "&bbox=$CEREDIGION_BBOX,EPSG:4326" +
            "&propertyName=$properties"
}

/**
 * Maps WFD OverallStatus string from the dataset to our app's colour system.
 * Values from the CSV: "High", "Good", "Moderate", "Poor", "Bad"
 */
fun wfdStatusToColor(status: String?): String {
    return when (status?.trim()) {
        "High"     -> "#2196F3" // Blue   — best possible status
        "Good"     -> "#4CAF50" // Green
        "Moderate" -> "#FF9800" // Orange
        "Poor"     -> "#F44336" // Red
        "Bad"      -> "#9C27B0" // Purple — worst status
        else       -> "#9E9E9E" // Grey   — unknown/no data
    }
}