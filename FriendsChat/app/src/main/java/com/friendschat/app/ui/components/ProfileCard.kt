package com.friendschat.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.friendschat.app.data.ChatUser
import com.friendschat.app.data.Prompt
import com.friendschat.app.data.RelationshipStatus

/**
 * Renders a dating profile as an editorial "page": a framed hero portrait with a
 * serif name plate, a row of sticker tags, a pull-quote bio, and prompt entries set
 * like magazine call-outs, interleaved with the remaining photos. Per-item like
 * buttons are preserved. The caller is responsible for making it scrollable.
 *
 * @param onLikeItem when non-null, a heart appears on each photo/prompt; tapping
 *        it reports a short label describing what was liked.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ProfileCard(
    user: ChatUser,
    modifier: Modifier = Modifier,
    starred: Boolean = false,
    onLikeItem: ((label: String) -> Unit)? = null
) {
    val photos = user.gallery
    val prompts = user.answeredPrompts

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Framed hero portrait with name plate
        HeroPhoto(
            user = user,
            photoUrl = photos.firstOrNull(),
            starred = starred,
            onLike = onLikeItem?.let { { it("their photo") } }
        )

        // Sticker tag row — relationship intent, work, place.
        val tags = buildList {
            RelationshipStatus.label(user.relationshipStatus).takeIf { it.isNotBlank() }
                ?.let { add(Triple<ImageVector?, String, Int>(Icons.Filled.FavoriteBorder, it, 0)) }
            user.jobTitle.takeIf { it.isNotBlank() }
                ?.let { add(Triple<ImageVector?, String, Int>(Icons.Filled.Work, it, 1)) }
            user.location.takeIf { it.isNotBlank() }
                ?.let { add(Triple<ImageVector?, String, Int>(Icons.Filled.LocationOn, it, 2)) }
        }
        if (tags.isNotEmpty()) {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { (icon, label, kind) -> StickerTag(icon, label, kind) }
            }
        }

        if (user.bio.isNotBlank()) {
            QuoteCard(user.bio)
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
            .clip(RoundedCornerShape(28.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp))
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
        // Warm bottom scrim for legible text
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.5f to Color.Transparent,
                        1f to Color(0xE6201712)
                    )
                )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            if (user.isNew) {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiary) {
                    Text(
                        "JUST ARRIVED",
                        color = MaterialTheme.colorScheme.onTertiary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            val title = buildString {
                append(user.name.substringBefore(' '))
                if (user.age in 18..120) append(", ${user.age}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                if (starred) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Verified by the community",
                        tint = Color(0xFFE6C46E),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
        if (onLike != null) LikeHeart(Modifier.align(Alignment.BottomEnd).padding(16.dp), onLike)
    }
}

@Composable
private fun GalleryPhoto(url: String, onLike: (() -> Unit)?) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
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

/** Pull-quote styled bio: a big serif opening mark with the text set as a quotation. */
@Composable
private fun QuoteCard(bio: String) {
    PaperCard {
        Row {
            Text(
                "“",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.height(28.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                bio,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 26.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun PromptCard(prompt: Prompt, onLike: (() -> Unit)?) {
    PaperCard {
        Box(Modifier.fillMaxWidth()) {
            Row {
                // Slim terracotta margin rule, like a marked-up page.
                Box(
                    Modifier
                        .width(3.dp)
                        .height(if (onLike != null) 64.dp else 56.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        prompt.question.uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        prompt.answer,
                        style = MaterialTheme.typography.headlineSmall,
                        lineHeight = MaterialTheme.typography.headlineSmall.fontSize.times(1.3f)
                    )
                }
            }
            if (onLike != null) LikeHeart(Modifier.align(Alignment.BottomEnd), onLike)
        }
    }
}

/** Flat paper card: soft surface with a hairline rule, no glossy elevation. */
@Composable
private fun PaperCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun LikeHeart(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        shadowElevation = 2.dp,
        modifier = modifier.size(46.dp),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Like this",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Sticker-style tag. `kind` picks a warm fill: 0 clay, 1 sage, 2 sand. */
@Composable
private fun StickerTag(icon: ImageVector?, label: String, kind: Int) {
    val (bg, fg) = when (kind) {
        0 -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(15.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = fg, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Small labelled chip row used on profile previews (job, location). */
@Composable
fun ProfileFactRow(jobTitle: String, location: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (jobTitle.isNotBlank()) StickerTag(Icons.Filled.Work, jobTitle, 1)
        if (location.isNotBlank()) StickerTag(Icons.Filled.LocationOn, location, 2)
    }
}
