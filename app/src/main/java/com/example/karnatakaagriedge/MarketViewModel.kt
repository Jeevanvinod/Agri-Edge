package com.example.karnatakaagriedge

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MarketViewModel : ViewModel() {
    // This holds the list of prices we get from the government API
    val marketData = mutableStateOf<List<MandiRecord>>(emptyList())

    // This tells the UI if we are currently downloading data
    val isLoading = mutableStateOf(false)

    // This catches any errors (like no internet)
    val errorMessage = mutableStateOf<String?>(null)

    fun fetchKarnatakaPrices(apiKey: String) {
        viewModelScope.launch {
            if (marketData.value.isNotEmpty()) return@launch // Don't fetch if we already have data

            isLoading.value = true
            try {
                // We call the Retrofit instance we created in MarketData.kt
                val response = RetrofitClient.instance.getMarketPrices(
                    apiKey = apiKey,
                    state = "Karnataka"
                )
                marketData.value = response.records
            } catch (e: Exception) {
                errorMessage.value = "Failed to load prices: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }
}