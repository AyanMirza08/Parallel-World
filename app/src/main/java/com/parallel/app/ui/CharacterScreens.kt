package com.parallel.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.parallel.app.domain.ParallelWorld
import com.parallel.app.domain.WorldCharacter
import com.parallel.app.ui.theme.ParallelPalette

@Composable
fun CharactersSection(world: ParallelWorld, onCharacterClick: (WorldCharacter) -> Unit) {
    Text(
        "${world.importantCharacters.size} people whose lives are part of this timeline",
        color = ParallelPalette.TextSecondary,
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(15.dp))

    if (world.importantCharacters.isEmpty()) {
        Text("No character records have been found in this world yet.", color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodyMedium)
        return
    }

    world.importantCharacters.forEachIndexed { index, character ->
        Card(
            onClick = { onCharacterClick(character) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(11.dp),
            colors = CardDefaults.cardColors(containerColor = ParallelPalette.Surface),
            border = BorderStroke(1.dp, ParallelPalette.Outline)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CharacterMark(character, large = false)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(character.name, color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(character.role, color = ParallelPalette.Accent, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(character.age?.let { "AGE $it" } ?: "AGE ?", color = ParallelPalette.TextMuted, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(14.dp))
                Text(character.background, color = ParallelPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(11.dp))
                Text(character.timelineRelationship, color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(13.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("OPEN PROFILE", color = ParallelPalette.Accent, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(7.dp))
                    Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = ParallelPalette.Accent, modifier = Modifier.size(14.dp))
                }
            }
        }
        if (index != world.importantCharacters.lastIndex) Spacer(Modifier.height(11.dp))
    }
}

@Composable
fun WorldCharacterDetailScreen(world: ParallelWorld, character: WorldCharacter, onBack: () -> Unit) {
    AdaptivePage {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back to characters", tint = ParallelPalette.TextPrimary)
            }
            Text("PARALLEL", color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(22.dp))
        Text("CHARACTER FILE", color = ParallelPalette.Accent, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(7.dp))
        Text(world.name, color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(21.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CharacterMark(character, large = true)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    character.name,
                    color = ParallelPalette.TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(5.dp))
                Text(character.role, color = ParallelPalette.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(character.age?.let { "AGE $it" } ?: "AGE NOT RECORDED", color = ParallelPalette.Accent, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(24.dp))

        CharacterDetailBlock("BACKGROUND", character.background)
        Spacer(Modifier.height(13.dp))
        CharacterDetailBlock("PERSONALITY", character.personality)
        Spacer(Modifier.height(13.dp))
        CharacterDetailBlock("RELATIONSHIP TO THIS TIMELINE", character.timelineRelationship)
        Spacer(Modifier.height(13.dp))
        CharacterActions(character.majorActions)
        Spacer(Modifier.height(27.dp))
    }
}

@Composable
private fun CharacterMark(character: WorldCharacter, large: Boolean) {
    val dimension = if (large) 64.dp else 44.dp
    val initials = character.name.split(Regex("\\s+"))
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")

    Box(
        modifier = Modifier.size(dimension).background(ParallelPalette.AccentSurface, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initials,
            color = ParallelPalette.TextPrimary,
            fontSize = if (large) 21.sp else 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.7.sp
        )
    }
}

@Composable
private fun CharacterDetailBlock(title: String, content: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(ParallelPalette.Surface, RoundedCornerShape(11.dp))
            .padding(17.dp)
    ) {
        Text(title, color = ParallelPalette.Accent, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(7.dp))
        Text(content, color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CharacterActions(actions: List<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(ParallelPalette.Surface, RoundedCornerShape(11.dp))
            .padding(17.dp)
    ) {
        Text("MAJOR ACTIONS", color = ParallelPalette.Accent, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(10.dp))
        if (actions.isEmpty()) {
            Text("No specific actions were recorded for this character.", color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
        } else {
            actions.forEachIndexed { index, action ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Text((index + 1).toString().padStart(2, '0'), color = ParallelPalette.Accent, style = MaterialTheme.typography.labelSmall)
                    Text(action, modifier = Modifier.weight(1f), color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.bodySmall)
                }
                if (index != actions.lastIndex) Spacer(Modifier.height(9.dp))
            }
        }
    }
}
