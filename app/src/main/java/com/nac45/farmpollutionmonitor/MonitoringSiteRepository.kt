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
data class DailySummary( // Real-world data from Edore's study a few years ago, does not monitor phosphorus or nitrates but
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

suspend fun fetchMonitoringSites(): List<MonitoringSiteDB> {
    return supabase.from("monitoring_sites").select().decodeList()
}

suspend fun fetchReadings(): List<WaterQualityReading> {
    return supabase.from("water_quality_readings").select().decodeList()
}

suspend fun fetchLatestDailySummaries(): List<DailySummary> {
    return supabase.from("daily_summaries").select().decodeList()
}