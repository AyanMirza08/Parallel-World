package com.parallel.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.parallel.app.domain.ParallelWorld
import com.parallel.app.domain.WorldAnomaly
import com.parallel.app.domain.WorldCharacter
import com.parallel.app.domain.WorldLocation
import com.parallel.app.ui.theme.ParallelPalette

enum class WorldSection(val icon: String, val title: String) {
    OVERVIEW("🌍", "OVERVIEW"),
    TIMELINE("📜", "TIMELINE"),
    LOCATIONS("📍", "LOCATIONS"),
    CHARACTERS("👤", "CHARACTERS"),
    ANOMALIES("⚡", "ANOMALIES")
}

@Composable
fun WorldScreen(
    world: ParallelWorld,
    onBack: () -> Unit,
    onOpenSection: (WorldSection) -> Unit
) {
    AdaptivePage {
        WorldTopBar(onBack = onBack, backDescription = "Back")
        Spacer(Modifier.height(28.dp))
        Text("YOUR PARALLEL WORLD", color = ParallelPalette.Accent, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(10.dp))
        Text(
            world.name,
            modifier = Modifier.semantics { heading() },
            color = ParallelPalette.TextPrimary,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(7.dp))
        Text(world.location + ", " + world.country + "  ·  " + world.currentYear, color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(17.dp))
        Text(world.alternateTimelineSummary, color = ParallelPalette.TextSecondary, style = MaterialTheme.typography.bodyLarge)

        Spacer(Modifier.height(30.dp))
        Text(
            "CHOOSE A THREAD TO EXPLORE",
            color = ParallelPalette.TextMuted,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(14.dp))
        WorldSection.entries.forEach { section ->
            WorldSectionCard(
                section = section,
                subtitle = sectionSubtitle(world, section),
                onClick = { onOpenSection(section) }
            )
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
fun WorldSectionScreen(
    world: ParallelWorld,
    section: WorldSection,
    onBack: () -> Unit,
    onLocationClick: (WorldLocation) -> Unit = {},
    onCharacterClick: (WorldCharacter) -> Unit = {},
    onAnomalyClick: (WorldAnomaly) -> Unit = {}
) {
    AdaptivePage {
        WorldTopBar(onBack = onBack, backDescription = "Back to exploration")
        Spacer(Modifier.height(22.dp))
        Text(section.icon, modifier = Modifier.clearAndSetSemantics { }, fontSize = 23.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            section.title,
            color = ParallelPalette.TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(6.dp))
        Text(world.name, color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(23.dp))

        when (section) {
            WorldSection.OVERVIEW -> OverviewSection(world)
            WorldSection.TIMELINE -> TimelineSection(world)
            WorldSection.LOCATIONS -> LocationsSection(world, onLocationClick)
            WorldSection.CHARACTERS -> CharactersSection(world, onCharacterClick)
            WorldSection.ANOMALIES -> AnomaliesSection(world, onAnomalyClick)
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun WorldTopBar(onBack: () -> Unit, backDescription: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = backDescription, tint = ParallelPalette.TextPrimary)
        }
        Text("PARALLEL", color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun WorldSectionCard(section: WorldSection, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = ParallelPalette.Surface),
        border = BorderStroke(1.dp, ParallelPalette.Outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(ParallelPalette.AccentSurface, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(section.icon, modifier = Modifier.clearAndSetSemantics { }, fontSize = 19.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(section.title, color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(5.dp))
                Text(subtitle, color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = ParallelPalette.TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun OverviewSection(world: ParallelWorld) {
    Text(world.alternateTimelineSummary, color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(20.dp))
    InfoCard("POINT OF DIVERGENCE · " + world.divergenceYear, world.divergencePoint)
    Spacer(Modifier.height(11.dp))
    InfoCard("CURRENT YEAR", world.currentYear.toString())
    Spacer(Modifier.height(11.dp))
    InfoCard("POPULATION", String.format(java.util.Locale.US, "%.1f million", world.population / 1_000_000.0))
    Spacer(Modifier.height(11.dp))
    InfoCard("GOVERNMENT / SYSTEM", world.government.system, world.government.description)
    Spacer(Modifier.height(11.dp))
    InfoCard("ECONOMY", world.economy.description, world.economy.majorSectors.joinToString("  ·  "))
    Spacer(Modifier.height(11.dp))
    InfoCard("CULTURE", world.culture.description, world.culture.definingTraits.joinToString("  ·  "))
    Spacer(Modifier.height(11.dp))
    InfoCard("TECHNOLOGY · " + world.technology.level, world.technology.description)
}

@Composable
private fun TimelineSection(world: ParallelWorld) {
    Text("The events that shaped this branch of history.", color = ParallelPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(19.dp))

    val events = world.majorHistoricalEvents.sortedBy { it.year }
    if (events.isEmpty()) {
        Text("No historical events have been recorded for this world yet.", color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodyMedium)
        return
    }

    events.forEachIndexed { index, event ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val centerX = 12.dp.toPx()
                    val centerY = 14.dp.toPx()
                    if (index != events.lastIndex) {
                        drawLine(
                            color = ParallelPalette.Outline,
                            start = Offset(centerX, centerY),
                            end = Offset(centerX, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                    drawCircle(color = ParallelPalette.Accent, radius = 5.dp.toPx(), center = Offset(centerX, centerY))
                    drawCircle(color = ParallelPalette.Surface, radius = 2.dp.toPx(), center = Offset(centerX, centerY))
                },
            verticalAlignment = Alignment.Top
        ) {
            Spacer(Modifier.width(29.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(11.dp),
                colors = CardDefaults.cardColors(containerColor = ParallelPalette.Surface),
                border = BorderStroke(1.dp, ParallelPalette.Outline)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("YEAR  ${event.year}", color = ParallelPalette.Accent, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(7.dp))
                    Text(event.title, color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(event.description, color = ParallelPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(13.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(ParallelPalette.Outline))
                    Spacer(Modifier.height(12.dp))
                    Text("HISTORICAL SIGNIFICANCE", color = ParallelPalette.Accent, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(5.dp))
                    Text(event.historicalSignificance, color = ParallelPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (index != events.lastIndex) {
            Box(
                Modifier
                    .width(29.dp)
                    .height(14.dp)
                    .drawBehind {
                        val centerX = 12.dp.toPx()
                        drawLine(
                            color = ParallelPalette.Outline,
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, description: String? = null, detail: String? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = ParallelPalette.Surface),
        border = BorderStroke(1.dp, ParallelPalette.Outline)
    ) {
        Column(Modifier.fillMaxWidth().padding(17.dp)) {
            Text(title, color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.titleMedium)
            description?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = ParallelPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            detail?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = ParallelPalette.Accent, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun sectionSubtitle(world: ParallelWorld, section: WorldSection): String = when (section) {
    WorldSection.OVERVIEW -> "Society, divergence, and how this world works"
    WorldSection.TIMELINE -> world.majorHistoricalEvents.size.toString() + " events across the alternate history"
    WorldSection.LOCATIONS -> world.importantLocations.size.toString() + " places worth discovering"
    WorldSection.CHARACTERS -> world.importantCharacters.size.toString() + " people connected to this timeline"
    WorldSection.ANOMALIES -> world.strangeAnomalies.size.toString() + " unexplained records to examine"
}
