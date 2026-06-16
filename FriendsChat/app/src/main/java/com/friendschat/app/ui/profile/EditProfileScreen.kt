package com.friendschat.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.friendschat.app.data.Prompt
import com.friendschat.app.data.PromptLibrary
import com.friendschat.app.ui.onboarding.LocationPicker
import com.friendschat.app.ui.onboarding.LocationViewModel

private val GENDERS = listOf("woman" to "Woman", "man" to "Man", "nonbinary" to "Nonbinary")
private val GENDERS_PLURAL = listOf("woman" to "Women", "man" to "Men", "nonbinary" to "Nonbinary")
private const val MAX_PHOTOS = 6

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    onboarding: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit,
    vm: ProfileViewModel = viewModel(),
    locVm: LocationViewModel = viewModel()
) {
    val me by vm.me.collectAsState()

    var seeded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    val interestedIn = remember { mutableStateListOf<String>() }
    var bio by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    val prompts = remember { mutableStateListOf(Prompt(), Prompt(), Prompt()) }

    // Seed the form once the profile loads.
    if (!seeded && me != null) {
        val m = me!!
        name = m.name
        ageText = if (m.age in 18..120) m.age.toString() else ""
        gender = m.gender
        interestedIn.clear(); interestedIn.addAll(m.interestedIn)
        bio = m.bio
        jobTitle = m.jobTitle
        relationship = m.relationshipStatus
        country = m.country
        location = m.location
        m.answeredPrompts.take(3).forEachIndexed { i, p -> prompts[i] = p }
        seeded = true
    }

    // Once the saved profile is loaded, populate the city dropdown for its country.
    LaunchedEffect(seeded) { if (seeded && country.isNotBlank()) locVm.loadCities(country) }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { vm.addPhoto(it) } }

    val photos = me?.gallery ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (onboarding) "Build your profile" else "Edit profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (!onboarding) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (onboarding) {
                Text(
                    "Add a few photos and tell people who you are. You can change all of this later.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
            }

            SectionLabel("PHOTOS  (${photos.size}/$MAX_PHOTOS)")
            PhotoGrid(
                photos = photos,
                uploading = vm.uploading,
                canAdd = photos.size < MAX_PHOTOS,
                onAdd = {
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRemove = { vm.removePhoto(it) }
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("ABOUT YOU")
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Display name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = ageText,
                onValueChange = { v -> ageText = v.filter { it.isDigit() }.take(3) },
                label = { Text("Age") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = jobTitle, onValueChange = { jobTitle = it },
                label = { Text("Job (optional)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("RELATIONSHIP STATUS")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.friendschat.app.data.RelationshipStatus.options.forEach { (key, label) ->
                    FilterChip(
                        selected = relationship == key,
                        onClick = { relationship = if (relationship == key) "" else key },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("LOCATION")
            LocationPicker(
                locVm = locVm,
                country = country,
                city = location,
                onCountry = { country = it; location = ""; locVm.loadCities(it) },
                onCity = { location = it }
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("I AM A")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GENDERS.forEach { (key, label) ->
                    FilterChip(
                        selected = gender == key,
                        onClick = { gender = key },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("INTERESTED IN")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GENDERS_PLURAL.forEach { (key, label) ->
                    val selected = interestedIn.contains(key)
                    FilterChip(
                        selected = selected,
                        onClick = { if (selected) interestedIn.remove(key) else interestedIn.add(key) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("BIO")
            OutlinedTextField(
                value = bio, onValueChange = { bio = it.take(300) },
                label = { Text("A short intro") },
                modifier = Modifier.fillMaxWidth().height(110.dp)
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("PROMPTS  (make them count)")
            prompts.forEachIndexed { i, p ->
                PromptEditor(
                    prompt = p,
                    onChange = { prompts[i] = it }
                )
                Spacer(Modifier.height(12.dp))
            }

            vm.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    vm.save(
                        name = name,
                        age = ageText.toIntOrNull() ?: 0,
                        gender = gender,
                        interestedIn = interestedIn.toList(),
                        bio = bio,
                        jobTitle = jobTitle,
                        relationshipStatus = relationship,
                        country = country,
                        location = location,
                        prompts = prompts.toList(),
                        completeOnboarding = onboarding,
                        photoCount = photos.size,
                        onDone = onDone
                    )
                },
                enabled = !vm.saving,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (vm.saving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                else Text(if (onboarding) "Start meeting people" else "Save profile", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun PhotoGrid(
    photos: List<String>,
    uploading: Boolean,
    canAdd: Boolean,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit
) {
    FlowRowPhotos(photos, uploading, canAdd, onAdd, onRemove)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowPhotos(
    photos: List<String>,
    uploading: Boolean,
    canAdd: Boolean,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        photos.forEach { url ->
            Box(Modifier.size(104.dp)) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                )
                Surface(
                    shape = CircleShape,
                    color = Color(0xCC000000),
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clickable { onRemove(url) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        if (canAdd) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(104.dp).clickable(enabled = !uploading) { onAdd() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (uploading) CircularProgressIndicator(modifier = Modifier.size(26.dp))
                    else Icon(Icons.Filled.Add, "Add photo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptEditor(prompt: Prompt, onChange: (Prompt) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = prompt.question.ifBlank { "Choose a prompt" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Prompt") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    PromptLibrary.questions.forEach { q ->
                        DropdownMenuItem(
                            text = { Text(q) },
                            onClick = { onChange(prompt.copy(question = q)); expanded = false }
                        )
                    }
                }
            }
            if (prompt.question.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = prompt.answer,
                    onValueChange = { onChange(prompt.copy(answer = it.take(200))) },
                    label = { Text("Your answer") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
