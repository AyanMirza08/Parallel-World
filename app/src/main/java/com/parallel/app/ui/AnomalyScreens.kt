package com.parallel.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
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
import com.parallel.app.domain.WorldAnomaly
import com.parallel.app.ui.theme.ParallelPalette

@Composable
fun AnomaliesSection(world: ParallelWorld, onAnomalyClick: (WorldAnomaly) -> Unit) {
    Text(
        "${world.strangeAnomalies.size} unresolved observations in this world",
        color = ParallelPalette.TextSecondary,
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(7.dp))
    Text(
        "Unusual patterns recorded by residents and public archives.",
        color = ParallelPalette.TextMuted,
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(17.dp))

    if (world.strangeAnomalies.isEmpty()) {
        Text("No anomaly records have surfaced in this world yet.", color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodyMedium)
        return
    }

    world.strangeAnomalies.forEachIndexed { index, anomaly ->
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(durationMillis = 180, delayMillis = index * 45)) +
                expandVertically(animationSpec = tween(durationMillis = 180, delayMillis = index * 45))
        ) {
            Card(
                onClick = { onAnomalyClick(anomaly) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(11.dp),
                colors = CardDefaults.cardColors(containerColor = ParallelPalette.Surface),
                border = BorderStroke(1.dp, ParallelPalette.Outline)
            ) {
                Column(Modifier.fillMaxWidth().padding(17.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(ParallelPalette.Accent, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(anomaly.classification.uppercase(), color = ParallelPalette.Accent, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(anomaly.name, color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(anomaly.description, color = ParallelPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(14.dp))
                    AnomalyStatus(anomaly.currentStatus)
                }
            }
        }
        if (index != world.strangeAnomalies.lastIndex) Spacer(Modifier.height(11.dp))
    }
}

@Composable
fun WorldAnomalyDetailScreen(world: ParallelWorld, anomaly: WorldAnomaly, onBack: () -> Unit) {
    AdaptivePage {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back to anomalies", tint = ParallelPalette.TextPrimary)
            }
            Text("PARALLEL", color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(22.dp))
        Text("ANOMALY RECORD", color = ParallelPalette.Accent, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(7.dp))
        Text(world.name, color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(22.dp))
        Text(anomaly.classification.uppercase(), color = ParallelPalette.Accent, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            anomaly.name,
            color = ParallelPalette.TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(16.dp))
        AnomalyDetailBlock("DESCRIPTION", anomaly.description)
        Spacer(Modifier.height(13.dp))
        AnomalyDetailBlock("DISCOVERY", anomaly.discovery)
        Spacer(Modifier.height(13.dp))
        KnownEffects(anomaly.knownEffects)
        Spacer(Modifier.height(13.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(ParallelPalette.Surface, RoundedCornerShape(11.dp))
                .padding(17.dp)
        ) {
            Text("CURRENT STATUS", color = ParallelPalette.Accent, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(9.dp))
            AnomalyStatus(anomaly.currentStatus)
        }
        Spacer(Modifier.height(27.dp))
    }
}

@Composable
private fun AnomalyDetailBlock(title: String, content: String) {
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
private fun KnownEffects(effects: List<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(ParallelPalette.Surface, RoundedCornerShape(11.dp))
            .padding(17.dp)
    ) {
        Text("KNOWN EFFECTS", color = ParallelPalette.Accent, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(10.dp))
        if (effects.isEmpty()) {
            Text("No effects have been confirmed in the available records.", color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
        } else {
            effects.forEachIndexed { index, effect ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Text((index + 1).toString().padStart(2, '0'), color = ParallelPalette.Accent, style = MaterialTheme.typography.labelSmall)
                    Text(effect, modifier = Modifier.weight(1f), color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.bodySmall)
                }
                if (index != effects.lastIndex) Spacer(Modifier.height(9.dp))
            }
        }
    }
}

@Composable
private fun AnomalyStatus(status: String) {
    Row(
        modifier = Modifier
            .background(ParallelPalette.AccentSurface, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).background(ParallelPalette.Accent, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(status, color = ParallelPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}
