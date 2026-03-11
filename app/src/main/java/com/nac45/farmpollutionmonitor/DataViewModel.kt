package com.nac45.farmpollutionmonitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

/**
 * ViewModel file that sits between the UI and the data to act as the messenger!
 * It fetches data from Supabase and holds it ready for the screen to display.
 */
class DataViewModel : ViewModel() {

    // These three states are what the UI watches and reacts to
    var sites by mutableStateOf<List<MonitoringSiteDB>>(emptyList())
        private set
    var readings by mutableStateOf<List<WaterQualityReading>>(emptyList())
        private set
    var dailySummaries by mutableStateOf<List<DailySummary>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set

    // This runs automatically when the ViewModel is created
    init {
        loadData()
    }

    private fun loadData() {
        // viewModelScope means if the user leaves the screen,
        // the network request cancels automatically, so no memory leaks.
        viewModelScope.launch {
            try {
                sites = fetchMonitoringSites()
                readings = fetchReadings()
                // Test to catch silent error
                android.util.Log.d("DataViewModel", "Sites loaded: ${sites.size}")
                android.util.Log.d("DataViewModel", "Readings loaded: ${readings.size}")
                android.util.Log.d("DataViewModel", "Summaries loaded: ${dailySummaries.size}")
            } catch (e: Exception) {
                // If something goes wrong, log data message
                android.util.Log.e("DataViewModel", "Error fetching data: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}