package com.nac45.farmpollutionmonitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

/**
 * ViewModel for the Map screen.
 * Fetches monitoring site coordinates from Supabase so they
 * can be displayed as markers on the map automatically —
 * no hardcoded coordinates in the code!
 */
class MapViewModel : ViewModel() {

    // List of sites to display as markers on the map
    var sites by mutableStateOf<List<MonitoringSiteDB>>(emptyList())
        private set

    init {
        loadSites()
    }

    private fun loadSites() {
        viewModelScope.launch {
            try {
                sites = fetchMonitoringSites()
                android.util.Log.d("MapViewModel", "Loaded ${sites.size} map markers from Supabase")
            } catch (e: Exception) {
                android.util.Log.e("MapViewModel", "Error loading markers: ${e.message}")
            }
        }
    }
}