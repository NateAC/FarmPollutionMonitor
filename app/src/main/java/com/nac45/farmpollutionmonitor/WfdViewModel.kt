package com.nac45.farmpollutionmonitor

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Holds WFD GeoJSON data for lakes and rivers.
 * Fetched once on creation and cached, so don't need to refetch each time.
 *
 * Also tracks which waterbody the user has tapped so the
 * bottom sheet knows what details to show.
 */
class WfdViewModel : ViewModel() {

    // Raw GeoJSON strings — passed directly into Mapbox GeoJsonSource
    var lakesGeoJson by mutableStateOf<String?>(null)
        private set

    var riversGeoJson by mutableStateOf<String?>(null)
        private set

    // Loading and error states
    var isLoadingLakes by mutableStateOf(false)
        private set

    var isLoadingRivers by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // The waterbody the user tapped activates the bottom sheet
    // Properties pulled from the GeoJSON feature
    var selectedWaterbody by mutableStateOf<WaterbodyProperties?>(null)
        private set

    init {
        loadWfdData()
    }

    private fun loadWfdData() {
        // Fetch lakes and rivers in parallel
        viewModelScope.launch {
            isLoadingLakes = true
            try {
                lakesGeoJson = fetchWfdLakes()
                Log.d("WfdViewModel", "Lakes GeoJSON loaded: ${lakesGeoJson?.length} chars")
            } catch (e: Exception) {
                errorMessage = "Failed to load lake data: ${e.message}"
                Log.e("WfdViewModel", "Lakes error: ${e.message}")
            } finally {
                isLoadingLakes = false
            }
        }

        viewModelScope.launch {
            isLoadingRivers = true
            try {
                riversGeoJson = fetchWfdRivers()
                Log.d("WfdViewModel", "Rivers GeoJSON loaded: ${riversGeoJson?.length} chars")
            } catch (e: Exception) {
                errorMessage = "Failed to load river data: ${e.message}"
                Log.e("WfdViewModel", "Rivers error: ${e.message}")
            } finally {
                isLoadingRivers = false
            }
        }
    }

    fun onWaterbodyTapped(properties: WaterbodyProperties) {
        selectedWaterbody = properties
    }

    fun onBottomSheetDismissed() {
        selectedWaterbody = null
    }

    // Allows the user to manually refresh the WFD data
    fun refresh() {
        lakesGeoJson = null
        riversGeoJson = null
        errorMessage = null
        loadWfdData()
    }
}

/**
 * Properties pulled from a tapped GeoJSON feature.
 * Populated from the WFD dataset fields.
 */
data class WaterbodyProperties(
    val name: String,
    val wbid: String,
    val overallStatus: String?,
    val ecoStatus: String?,
    val chemStatus: String?,
    val dissolvedOxygen: String?,  // Lakes only
    val totalPhosphorus: String?,  // Lakes only
    val sqKms: String?,            // Lakes only
    val region: String?,
    val type: WaterbodyType
)

enum class WaterbodyType { LAKE, RIVER }
