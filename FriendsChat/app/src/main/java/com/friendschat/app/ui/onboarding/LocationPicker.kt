package com.friendschat.app.ui.onboarding

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val MAX_SUGGESTIONS = 6

/**
 * Reusable cascading location selector used by both onboarding and Edit Profile.
 * Country list and the selected country's cities come live from
 * [LocationViewModel]; the city list reloads automatically whenever the country
 * text matches a known country.
 *
 * Both fields are ordinary editable text fields — you can type to filter, tap a
 * suggestion, clear with the ✕, or delete freely.
 */
@Composable
fun LocationPicker(
    locVm: LocationViewModel,
    country: String,
    city: String,
    onCountry: (String) -> Unit,
    onCity: (String) -> Unit
) {
    // Load the matching city list whenever a valid country is set — whether the
    // user typed it in full or picked it from the suggestions.
    LaunchedEffect(country, locVm.countries) {
        if (country.isNotBlank() && locVm.countries.any { it.equals(country, ignoreCase = true) }) {
            locVm.loadCities(country)
        }
    }

    Column {
        SearchableField(
            label = "Country",
            selected = country,
            options = locVm.countries,
            loading = locVm.loadingCountries,
            enabled = true,
            onValueChange = onCountry
        )
        Spacer(Modifier.height(12.dp))
        SearchableField(
            label = when {
                country.isBlank() -> "City (pick a country first)"
                locVm.cities.isNotEmpty() -> "City (${locVm.cities.size} available — type to search)"
                else -> "City"
            },
            selected = city,
            options = locVm.cities,
            loading = locVm.loadingCities,
            enabled = country.isNotBlank(),
            onValueChange = onCity
        )
        locVm.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}

/**
 * An always-editable text field with type-to-filter suggestions shown inline.
 *
 * Crucially it does NOT use ExposedDropdownMenuBox: that opens a focusable popup
 * which steals focus from the field, leaving you unable to keep typing or delete.
 * Here [selected] is the displayed text, every edit flows straight back through
 * [onValueChange], the ✕ clears it, and suggestions are tapped via [pointerInput]
 * (which doesn't grab focus) so editing always works.
 */
@Composable
fun SearchableField(
    label: String,
    selected: String,
    options: List<String>,
    loading: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }

    val suggestions = remember(selected, options) {
        val base = if (selected.isBlank()) options
        else options.filter { it.contains(selected, ignoreCase = true) }
        base.take(MAX_SUGGESTIONS)
    }
    // Don't bother showing the list once the text already is an exact option.
    val showSuggestions = focused && enabled && suggestions.isNotEmpty() &&
        !(suggestions.size == 1 && suggestions.first().equals(selected, ignoreCase = true))

    Column {
        OutlinedTextField(
            value = selected,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            enabled = enabled,
            trailingIcon = {
                when {
                    loading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    selected.isNotEmpty() && enabled -> IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
        )
        if (showSuggestions) {
            Surface(
                tonalElevation = 3.dp,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column {
                    suggestions.forEach { option ->
                        Text(
                            option,
                            modifier = Modifier
                                .fillMaxWidth()
                                // pointerInput taps don't request focus, so the
                                // field stays focused and the tap isn't cancelled.
                                .pointerInput(option) {
                                    detectTapGestures {
                                        onValueChange(option)
                                        focusManager.clearFocus()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
