package com.parallel.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.parallel.app.domain.ParallelWorld
import com.parallel.app.domain.WorldLocation
import com.parallel.app.ui.theme.ParallelPalette
import java.util.Locale

@Composable
fun LocationsSection(world: ParallelWorld, onLocationClick: (WorldLocation) -> Unit) {
    Text("${world.importantLocations.size} places in this timeline", color = ParallelPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(15.dp))

    if (world.importantLocations.isEmpty()) {
        Text("No locations have been recorded for this world yet.", color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodyMedium)
        return
    }

    world.importantLocations.forEach { location ->
        Card(
            onClick = { onLocationClick(location) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(11.dp),
            colors = CardDefaults.cardColors(containerColor = ParallelPalette.Surface),
            border = BorderStroke(1.dp, ParallelPalette.Outline)
        ) {
            Column(Modifier.fillMaxWidth().padding(11.dp)) {
                LocationVisualPlaceholder(location.name, height = 112.dp)
                Column(Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 12.dp)) {
                    Text(location.name, color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(location.description, color = ParallelPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(13.dp))
                    Text("WHY IT MATTERS", color = ParallelPalette.Accent, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(5.dp))
                    Text(location.whyItMatters, color = ParallelPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("OPEN LOCATION", color = ParallelPalette.Accent, style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.width(7.dp))
                        Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = ParallelPalette.Accent, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun WorldLocationDetailScreen(
    world: ParallelWorld,
    location: WorldLocation,
    onBack: () -> Unit
) {
    AdaptivePage {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back to locations", tint = ParallelPalette.TextPrimary)
            }
            Text("PARALLEL", color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(22.dp))
        Text("LOCATION FILE", color = ParallelPalette.Accent, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(7.dp))
        Text(world.name, color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(17.dp))
        LocationVisualPlaceholder(location.name, height = 208.dp)
        Spacer(Modifier.height(22.dp))
        Text(
            location.name,
            color = ParallelPalette.TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(7.dp))
        Text("${world.location}, ${world.country}  ·  ${world.currentYear}", color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(23.dp))

        LocationDetailBlock("ABOUT THIS PLACE", location.description)
        Spacer(Modifier.height(17.dp))
        LocationDetailBlock("WHY IT MATTERS", location.whyItMatters)
        location.coordinates?.let { coordinates ->
            Spacer(Modifier.height(17.dp))
            LocationDetailBlock(
                "COORDINATES",
                String.format(Locale.US, "%.5f°, %.5f°", coordinates.latitude, coordinates.longitude)
            )
        }
        Spacer(Modifier.height(27.dp))
    }
}

@Composable
private fun LocationDetailBlock(label: String, content: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(ParallelPalette.Surface)
            .padding(17.dp)
    ) {
        Text(label, color = ParallelPalette.Accent, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(7.dp))
        Text(content, color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

/** A replaceable media viewport; map or image providers can render here when available. */
@Composable
private fun LocationVisualPlaceholder(name: String, height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF10131A))
            .drawBehind {
                val gridColor = Color(0xFF202633)
                for (column in 1..8) {
                    val x = size.width * column / 8f
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                }
                for (row in 1..5) {
                    val y = size.height * row / 5f
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                val center = Offset(size.width * 0.73f, size.height * 0.43f)
                drawCircle(Color(0xFF30384A), radius = size.height * 0.24f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                drawCircle(Color(0xFF46516B), radius = size.height * 0.12f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                drawLine(Color(0xFF46516B), Offset(center.x - 8.dp.toPx(), center.y), Offset(center.x + 8.dp.toPx(), center.y), strokeWidth = 1.dp.toPx())
                drawLine(Color(0xFF46516B), Offset(center.x, center.y - 8.dp.toPx()), Offset(center.x, center.y + 8.dp.toPx()), strokeWidth = 1.dp.toPx())
            },
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            "IMAGE PLACEHOLDER  ·  ${name.uppercase(Locale.getDefault())}",
            modifier = Modifier.padding(12.dp),
            color = ParallelPalette.TextPrimary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

