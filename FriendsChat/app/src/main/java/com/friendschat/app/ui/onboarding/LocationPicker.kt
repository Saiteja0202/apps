package com.friendschat.app.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val MAX_VISIBLE = 100

/**
 * Reusable cascading location selector used by both onboarding and Edit Profile.
 * Country list and the selected country's cities come live from
 * [LocationViewModel]; picking a country reloads the city list in real time.
 */
@Composable
fun LocationPicker(
    locVm: LocationViewModel,
    country: String,
    city: String,
    onCountry: (String) -> Unit,
    onCity: (String) -> Unit
) {
    Column {
        SearchableDropdown(
            label = "Country",
            selected = country,
            options = locVm.countries,
            loading = locVm.loadingCountries,
            enabled = true,
            onPick = onCountry
        )
        Spacer(Modifier.height(12.dp))
        SearchableDropdown(
            label = when {
                country.isBlank() -> "City (pick a country first)"
                locVm.cities.isNotEmpty() -> "City (${locVm.cities.size} available — type to search)"
                else -> "City"
            },
            selected = city,
            options = locVm.cities,
            loading = locVm.loadingCities,
            enabled = country.isNotBlank(),
            onPick = onCity
        )
        locVm.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}

/**
 * Type-to-filter dropdown that stays smooth with thousands of options by showing
 * only the first [MAX_VISIBLE] matches — every entry is still reachable by typing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDropdown(
    label: String,
    selected: String,
    options: List<String>,
    loading: Boolean,
    enabled: Boolean,
    onPick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(selected) { mutableStateOf(selected) }
    val filtered = remember(query, options) {
        if (query.isBlank()) options.take(MAX_VISIBLE)
        else options.filter { it.contains(query, ignoreCase = true) }.take(MAX_VISIBLE)
    }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; expanded = true },
            label = { Text(label) },
            singleLine = true,
            enabled = enabled,
            trailingIcon = {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        if (filtered.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
                filtered.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onPick(option); query = option; expanded = false }
                    )
                }
            }
        }
    }
}
