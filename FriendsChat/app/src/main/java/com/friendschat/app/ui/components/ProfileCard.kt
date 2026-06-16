package com.friendschat.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.friendschat.app.data.ChatUser
import com.friendschat.app.data.Prompt
import com.friendschat.app.data.RelationshipStatus

/**
 * Renders a dating profile the Hinge way: a tall hero photo with the name/age,
 * then prompts and remaining photos interleaved, with optional per-item like
 * buttons. The caller is responsible for making it scrollable.
 *
 * @param onLikeItem when non-null, a heart appears on each photo/prompt; tapping
 *        it reports a short label describing what was liked.
 */
@Composable
fun ProfileCard(
    user: ChatUser,
    modifier: Modifier = Modifier,
    starred: Boolean = false,
    onLikeItem: ((label: String) -> Unit)? = null
) {
    val photos = user.gallery
    val prompts = user.answeredPrompts

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Hero photo with name overlay
        HeroPhoto(
            user = user,
            photoUrl = photos.firstOrNull(),
            starred = starred,
            onLike = onLikeItem?.let { { it("their photo") } }
        )

        if (user.bio.isNotBlank()) {
            InfoCard {
                Text(user.bio, style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Interleave prompts with the remaining photos.
        val rest = photos.drop(1)
        val maxLen = maxOf(prompts.size, rest.size)
        for (i in 0 until maxLen) {
            prompts.getOrNull(i)?.let { p ->
                PromptCard(p, onLike = onLikeItem?.let { cb -> { cb("\"${p.question}\"") } })
            }
            rest.getOrNull(i)?.let { url ->
                GalleryPhoto(url, onLike = onLikeItem?.let { cb -> { cb("their photo") } })
            }
        }
    }
}

@Composable
private fun HeroPhoto(user: ChatUser, photoUrl: String?, starred: Boolean, onLike: (() -> Unit)?) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = user.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Bottom scrim for legible text
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to Color(0xCC000000)
                    )
                )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            val title = buildString {
                append(user.name.substringBefore(' '))
                if (user.age in 18..120) append(", ${user.age}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (starred) {
                    Spacer(Modifier.size(6.dp))
                    Icon(Icons.Filled.Star, contentDescription = "Verified by the community", tint = Color(0xFFFFC83D), modifier = Modifier.size(22.dp))
                }
                if (user.isNew) {
                    Spacer(Modifier.size(8.dp))
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondary) {
                        Text("New here", color = MaterialTheme.colorScheme.onSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }
            val sub = listOfNotNull(
                RelationshipStatus.label(user.relationshipStatus).takeIf { it.isNotBlank() },
                user.jobTitle.takeIf { it.isNotBlank() },
                user.location.takeIf { it.isNotBlank() }
            )
            if (sub.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(sub.joinToString("  ·  "), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (onLike != null) LikeHeart(Modifier.align(Alignment.BottomEnd).padding(14.dp), onLike)
    }
}

@Composable
private fun GalleryPhoto(url: String, onLike: (() -> Unit)?) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (onLike != null) LikeHeart(Modifier.align(Alignment.BottomEnd).padding(14.dp), onLike)
    }
}

@Composable
private fun PromptCard(prompt: Prompt, onLike: (() -> Unit)?) {
    InfoCard {
        Box(Modifier.fillMaxWidth()) {
            Column {
                Text(
                    prompt.question,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    prompt.answer,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = MaterialTheme.typography.headlineSmall.fontSize.times(1.25f)
                )
            }
            if (onLike != null) LikeHeart(Modifier.align(Alignment.BottomEnd), onLike)
        }
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun LikeHeart(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = modifier.size(44.dp),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Like this",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/** Small labelled chip row used on profile previews (job, location). */
@Composable
fun ProfileFactRow(jobTitle: String, location: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (jobTitle.isNotBlank()) FactChip(Icons.Filled.Work, jobTitle)
        if (location.isNotBlank()) FactChip(Icons.Filled.LocationOn, location)
    }
}

@Composable
private fun FactChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}
