package com.nac45.farmpollutionmonitor

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
data class MonitoringSiteDB(
    val id: Int,
    val name: String,
    val region: String,
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class WaterQualityReading( // Current dummy data files, will eventually change this to the WFD dataset but its a big job and want to show a working app first!
    val id: Int,
    val site_id: Int,
    val phosphorus: Double,
    val nitrates: Double,
    val status: String
)

@Serializable
data class DailySummary( // Real-world data from Edore's study a few years ago, does not monitor phosphorus or nitrates
    val site_id: Int,
    val date: String,
    val avg_temperature: Double,
    val avg_ph: Double,
    val avg_do_mgl: Double,
    val avg_do_sat: Double,
    val avg_ec: Double,
    val avg_depth: Double,
    val reading_count: Int
)
// NewMonitoringSite is separate from MonitoringSiteDB because MonitoringSiteDB has an id field. Supabase sends it over causing crashes.
@Serializable
data class NewMonitoringSite(
    val name: String,
    val region: String,
    val latitude: Double,
    val longitude: Double
)

// Inserts a new monitoring site into Supabase.
// Region is hardcoded to Ceredigion for now since thats the project scope, but could be a dropdown in a future version to support other regions!
suspend fun insertMonitoringSite(
    name: String,
    region: String,
    latitude: Double,
    longitude: Double
) {
    supabase.from("monitoring_sites").insert(
        NewMonitoringSite(
            name = name,
            region = region,
            latitude = latitude,
            longitude = longitude
        )
    )
}

suspend fun fetchMonitoringSites(): List<MonitoringSiteDB> {
    return supabase.from("monitoring_sites").select().decodeList()
}

suspend fun fetchReadings(): List<WaterQualityReading> {
    return supabase.from("water_quality_readings").select().decodeList()
}

suspend fun fetchLatestDailySummaries(): List<DailySummary> {
    return supabase.from("daily_summaries").select().decodeList()
}