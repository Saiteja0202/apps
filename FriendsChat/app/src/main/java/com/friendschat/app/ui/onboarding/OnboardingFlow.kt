package com.friendschat.app.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.friendschat.app.ui.components.AnimeCarousel
import com.friendschat.app.data.Prompt
import com.friendschat.app.data.PromptLibrary
import com.friendschat.app.data.RelationshipStatus
import com.friendschat.app.ui.profile.ProfileViewModel

private const val MAX_PHOTOS = 6
private const val LAST_STEP = 8

private data class Choice(val key: String, val label: String, val emoji: String)

private val GENDER_CHOICES = listOf(
    Choice("woman", "Woman", "👩"),
    Choice("man", "Man", "👨"),
    Choice("nonbinary", "Nonbinary", "🧑")
)
private val INTEREST_CHOICES = listOf(
    Choice("woman", "Women", "💗"),
    Choice("man", "Men", "💙"),
    Choice("nonbinary", "Everyone", "💜")
)

/**
 * A friendly, animated, multi-page profile builder. One idea per page, with
 * sliding transitions, a live progress bar, and the drifting [AnimatedBackground]
 * behind it. Replaces the old single-form onboarding.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingFlow(
    onDone: () -> Unit,
    vm: ProfileViewModel = viewModel(),
    locVm: LocationViewModel = viewModel()
) {
    val me by vm.me.collectAsState()

    var step by rememberSaveable { mutableIntStateOf(0) }
    var seeded by remember { mutableStateOf(false) }

    var name by rememberSaveable { mutableStateOf("") }
    var ageText by rememberSaveable { mutableStateOf("") }
    var jobTitle by rememberSaveable { mutableStateOf("") }
    var relationship by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }   // city
    var gender by rememberSaveable { mutableStateOf("") }
    val interestedIn = remember { mutableStateListOf<String>() }
    var bio by rememberSaveable { mutableStateOf("") }
    val prompts = remember { mutableStateListOf(Prompt(), Prompt(), Prompt()) }

    if (!seeded && me != null) {
        name = me!!.name
        seeded = true
    }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { vm.addPhoto(it) } }
    val photos = me?.gallery ?: emptyList()

    val age = ageText.toIntOrNull() ?: 0
    val canProceed = when (step) {
        1 -> name.isNotBlank() && age >= 18
        2 -> country.isNotBlank() && location.isNotBlank()
        3 -> gender.isNotBlank()
        4 -> interestedIn.isNotEmpty()
        5 -> photos.isNotEmpty()
        else -> true
    }

    AnimatedBackground {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            // Progress
            val progress by animateFloatAsState(step / LAST_STEP.toFloat(), tween(400), label = "progress")
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Step ${step + 1} of ${LAST_STEP + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState >= initialState)
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                    else
                        (slideInHorizontally { -it } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "steps"
            ) { s ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(16.dp))
                    when (s) {
                        0 -> WelcomeStep()
                        1 -> AboutStep(name, { name = it }, ageText, { ageText = it.filter { c -> c.isDigit() }.take(3) }, jobTitle, { jobTitle = it }, relationship, { relationship = it })
                        2 -> LocationStep(
                            locVm = locVm,
                            country = country,
                            city = location,
                            onCountry = { country = it },
                            onCity = { location = it }
                        )
                        3 -> ChoiceStep(
                            icon = Icons.Rounded.Face,
                            title = "How do you identify?",
                            subtitle = "This is shown on your profile.",
                            choices = GENDER_CHOICES,
                            selected = setOf(gender),
                            onToggle = { gender = it }
                        )
                        4 -> ChoiceStep(
                            icon = Icons.Rounded.FavoriteBorder,
                            title = "Who would you like to meet?",
                            subtitle = "Pick all that apply.",
                            choices = INTEREST_CHOICES,
                            selected = interestedIn.toSet(),
                            onToggle = { if (interestedIn.contains(it)) interestedIn.remove(it) else interestedIn.add(it) }
                        )
                        5 -> PhotosStep(photos, vm.uploading, { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, { vm.removePhoto(it) })
                        6 -> BioStep(bio) { bio = it.take(300) }
                        7 -> PromptsStep(prompts) { i, p -> prompts[i] = p }
                        else -> FinishStep(name)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            vm.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }

            // Controls
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) {
                    TextButton(onClick = { vm.clearError(); step-- }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Back")
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        vm.clearError()
                        if (step < LAST_STEP) step++
                        else vm.save(
                            name = name, age = age, gender = gender,
                            interestedIn = interestedIn.toList(), bio = bio,
                            jobTitle = jobTitle, relationshipStatus = relationship,
                            country = country, location = location,
                            prompts = prompts.toList(), completeOnboarding = true,
                            photoCount = photos.size, onDone = onDone
                        )
                    },
                    enabled = canProceed && !vm.saving,
                    modifier = Modifier.height(52.dp)
                ) {
                    if (vm.saving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Text(
                            when (step) {
                                0 -> "Let's go"
                                LAST_STEP -> "Start meeting people"
                                else -> "Next"
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(if (step == LAST_STEP) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward, null)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- step content

@Composable
private fun StepHeader(icon: ImageVector, title: String, subtitle: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(60.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
        }
    }
    Spacer(Modifier.height(14.dp))
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    if (subtitle.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun WelcomeStep() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().height(240.dp)
    ) {
        Box {
            AnimeCarousel(Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0xAA000000))
                )
            )
        }
    }
    Spacer(Modifier.height(20.dp))
    StepHeader(Icons.Rounded.Favorite, "Welcome to GenZ", "Let's build a profile that feels like you. It only takes a minute.")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutStep(
    name: String, onName: (String) -> Unit,
    age: String, onAge: (String) -> Unit,
    job: String, onJob: (String) -> Unit,
    relationship: String, onRelationship: (String) -> Unit
) {
    StepHeader(Icons.Rounded.Person, "The basics", "What should people call you?")
    OutlinedTextField(name, onName, label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        age, onAge, label = { Text("Age (18+)") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(job, onJob, label = { Text("Job (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(16.dp))
    Text(
        "Relationship status",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        RelationshipStatus.options.forEach { (key, label) ->
            FilterChip(
                selected = relationship == key,
                onClick = { onRelationship(if (relationship == key) "" else key) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun LocationStep(
    locVm: LocationViewModel,
    country: String,
    city: String,
    onCountry: (String) -> Unit,
    onCity: (String) -> Unit
) {
    StepHeader(Icons.Rounded.LocationOn, "Where are you?", "You'll be matched with people in your city.")
    LocationPicker(locVm, country, city, onCountry, onCity)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceStep(
    icon: ImageVector,
    title: String,
    subtitle: String,
    choices: List<Choice>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    StepHeader(icon, title, subtitle)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        choices.forEach { c ->
            ChoiceCard(c.emoji, c.label, selected.contains(c.key)) { onToggle(c.key) }
        }
    }
}

@Composable
private fun ChoiceCard(emoji: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.06f else 1f, tween(220), label = "scale")
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 6.dp else 1.dp,
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.size(120.dp).scale(scale).clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(emoji, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(label, fontWeight = FontWeight.SemiBold, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotosStep(photos: List<String>, uploading: Boolean, onAdd: () -> Unit, onRemove: (String) -> Unit) {
    StepHeader(Icons.Rounded.PhotoCamera, "Add your photos", "At least one. Pick your favourites — first one is your main.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        photos.forEach { url ->
            Box(Modifier.size(104.dp)) {
                AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)))
                Surface(
                    shape = CircleShape, color = Color(0xCC000000),
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clickable { onRemove(url) }
                ) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "Remove", tint = Color.White, modifier = Modifier.size(16.dp)) }
                }
            }
        }
        if (photos.size < MAX_PHOTOS) {
            Surface(
                shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(104.dp).clickable(enabled = !uploading) { onAdd() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (uploading) CircularProgressIndicator(modifier = Modifier.size(26.dp))
                    else Icon(Icons.Rounded.Add, "Add photo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                }
            }
        }
    }
}

@Composable
private fun BioStep(bio: String, onBio: (String) -> Unit) {
    StepHeader(Icons.Rounded.Edit, "Say something about you", "A line or two. Keep it real, keep it light.")
    OutlinedTextField(bio, onBio, label = { Text("Your bio (optional)") }, modifier = Modifier.fillMaxWidth().height(140.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptsStep(prompts: List<Prompt>, onChange: (Int, Prompt) -> Unit) {
    StepHeader(Icons.AutoMirrored.Rounded.Chat, "Pick a few prompts", "Give people something to message you about.")
    prompts.forEachIndexed { i, p ->
        PromptSlot(p) { onChange(i, it) }
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptSlot(prompt: Prompt, onChange: (Prompt) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = prompt.question.ifBlank { "Choose a prompt" },
                    onValueChange = {}, readOnly = true,
                    label = { Text("Prompt") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    PromptLibrary.questions.forEach { q ->
                        DropdownMenuItem(text = { Text(q) }, onClick = { onChange(prompt.copy(question = q)); expanded = false })
                    }
                }
            }
            if (prompt.question.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(prompt.answer, { onChange(prompt.copy(answer = it.take(200))) }, label = { Text("Your answer") }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun FinishStep(name: String) {
    StepHeader(Icons.Rounded.CheckCircle, "You're all set${if (name.isNotBlank()) ", ${name.substringBefore(' ')}" else ""}!", "Tap below to start meeting people on GenZ.")
}
