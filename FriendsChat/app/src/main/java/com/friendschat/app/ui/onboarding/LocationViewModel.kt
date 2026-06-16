package com.friendschat.app.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendschat.app.data.LocationRepository
import kotlinx.coroutines.launch

/** Loads countries once and cities on demand for the cascading location step. */
class LocationViewModel : ViewModel() {

    var countries by mutableStateOf<List<String>>(emptyList())
        private set
    var cities by mutableStateOf<List<String>>(emptyList())
        private set
    var loadingCountries by mutableStateOf(false)
        private set
    var loadingCities by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init { loadCountries() }

    fun loadCountries() {
        if (loadingCountries || countries.isNotEmpty()) return
        loadingCountries = true; error = null
        viewModelScope.launch {
            runCatching { LocationRepository.countries() }
                .onSuccess { countries = it }
                .onFailure { error = it.message ?: "Couldn't load countries" }
            loadingCountries = false
        }
    }

    /** Fetch cities for the chosen country; clears the previous list immediately. */
    fun loadCities(country: String) {
        cities = emptyList()
        if (country.isBlank()) return
        loadingCities = true; error = null
        viewModelScope.launch {
            runCatching { LocationRepository.cities(country) }
                .onSuccess { cities = it }
                .onFailure { error = it.message ?: "Couldn't load cities" }
            loadingCities = false
        }
    }
}
