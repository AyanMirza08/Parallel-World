package com.parallel.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.parallel.app.domain.ParallelWorld
import com.parallel.app.domain.WorldAnomaly
import com.parallel.app.domain.WorldCharacter
import com.parallel.app.domain.WorldLocation
import com.parallel.app.data.WorldRepository
import com.parallel.app.ui.theme.ParallelPalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private enum class AppScreen { HOME, CREATE, WORLD, OVERVIEW, TIMELINE, LOCATIONS, LOCATION_DETAIL, CHARACTERS, CHARACTER_DETAIL, ANOMALIES, ANOMALY_DETAIL }

@Composable
fun ParallelApp() {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var storageError by remember { mutableStateOf<String?>(null) }
    var selectedWorld by remember { mutableStateOf<ParallelWorld?>(null) }
    var selectedLocation by remember { mutableStateOf<WorldLocation?>(null) }
    var selectedCharacter by remember { mutableStateOf<WorldCharacter?>(null) }
    var selectedAnomaly by remember { mutableStateOf<WorldAnomaly?>(null) }
    var worldBackScreen by remember { mutableStateOf(AppScreen.HOME) }
    var pendingDelete by remember { mutableStateOf<ParallelWorld?>(null) }
    val scope = rememberCoroutineScope()
    val savedWorldsFlow = remember(context) { WorldRepository.observeWorlds(context) }
    var worlds by remember { mutableStateOf<List<ParallelWorld>>(emptyList()) }
    var worldsLoading by remember { mutableStateOf(true) }
    var worldsLoadFailed by remember { mutableStateOf(false) }
    var loadAttempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(context, savedWorldsFlow, loadAttempt) {
        worldsLoading = true
        worldsLoadFailed = false
        try {
            WorldRepository.importLegacyWorlds(context)
            savedWorldsFlow.collect { saved ->
                worlds = saved
                worldsLoading = false
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            worldsLoading = false
            worldsLoadFailed = true
        }
    }

    BackHandler(enabled = screen != AppScreen.HOME) {
        screen = when (screen) {
            AppScreen.LOCATION_DETAIL -> AppScreen.LOCATIONS
            AppScreen.CHARACTER_DETAIL -> AppScreen.CHARACTERS
            AppScreen.ANOMALY_DETAIL -> AppScreen.ANOMALIES
            AppScreen.OVERVIEW, AppScreen.TIMELINE, AppScreen.LOCATIONS,
            AppScreen.CHARACTERS, AppScreen.ANOMALIES -> AppScreen.WORLD
            AppScreen.WORLD -> worldBackScreen
            else -> AppScreen.HOME
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Crossfade(targetState = screen, animationSpec = tween(180), label = "Parallel screen transition") { visibleScreen ->
        when (visibleScreen) {
            AppScreen.HOME -> HomeScreen(
                worlds = worlds,
                isLoading = worldsLoading,
                loadFailed = worldsLoadFailed,
                onCreateWorld = { screen = AppScreen.CREATE },
                onSettings = { showSettingsDialog = true },
                onRetryLoad = { loadAttempt += 1 },
                onDeleteWorld = { world -> pendingDelete = world },
                onOpenWorld = { world ->
                    selectedWorld = world
                    selectedLocation = null
                    selectedCharacter = null
                    selectedAnomaly = null
                    worldBackScreen = AppScreen.HOME
                    screen = AppScreen.WORLD
                }
            )
            AppScreen.CREATE -> CreateWorldScreen(
                onBack = { screen = AppScreen.HOME },
                onGenerate = { world ->
                    WorldRepository.save(context, world)
                    selectedWorld = world
                    selectedLocation = null
                    selectedCharacter = null
                    selectedAnomaly = null
                    worldBackScreen = AppScreen.CREATE
                    screen = AppScreen.WORLD
                }
            )
            AppScreen.WORLD -> selectedWorld?.let { world ->
                WorldScreen(
                    world = world,
                    onBack = { screen = worldBackScreen },
                    onOpenSection = { section -> screen = section.toAppScreen() }
                )
            }
            AppScreen.OVERVIEW -> selectedWorld?.let { WorldSectionScreen(it, WorldSection.OVERVIEW) { screen = AppScreen.WORLD } }
            AppScreen.TIMELINE -> selectedWorld?.let { WorldSectionScreen(it, WorldSection.TIMELINE) { screen = AppScreen.WORLD } }
            AppScreen.LOCATIONS -> selectedWorld?.let { world ->
                WorldSectionScreen(
                    world = world,
                    section = WorldSection.LOCATIONS,
                    onBack = { screen = AppScreen.WORLD },
                    onLocationClick = { location ->
                        selectedLocation = location
                        screen = AppScreen.LOCATION_DETAIL
                    }
                )
            }
            AppScreen.LOCATION_DETAIL -> {
                val world = selectedWorld
                val location = selectedLocation
                if (world != null && location != null) {
                    WorldLocationDetailScreen(world, location) { screen = AppScreen.LOCATIONS }
                }
            }
            AppScreen.CHARACTERS -> selectedWorld?.let { world ->
                WorldSectionScreen(
                    world = world,
                    section = WorldSection.CHARACTERS,
                    onBack = { screen = AppScreen.WORLD },
                    onCharacterClick = { character ->
                        selectedCharacter = character
                        screen = AppScreen.CHARACTER_DETAIL
                    }
                )
            }
            AppScreen.CHARACTER_DETAIL -> {
                val world = selectedWorld
                val character = selectedCharacter
                if (world != null && character != null) {
                    WorldCharacterDetailScreen(world, character) { screen = AppScreen.CHARACTERS }
                }
            }
            AppScreen.ANOMALIES -> selectedWorld?.let { world ->
                WorldSectionScreen(
                    world = world,
                    section = WorldSection.ANOMALIES,
                    onBack = { screen = AppScreen.WORLD },
                    onAnomalyClick = { anomaly ->
                        selectedAnomaly = anomaly
                        screen = AppScreen.ANOMALY_DETAIL
                    }
                )
            }
            AppScreen.ANOMALY_DETAIL -> {
                val world = selectedWorld
                val anomaly = selectedAnomaly
                if (world != null && anomaly != null) {
                    WorldAnomalyDetailScreen(world, anomaly) { screen = AppScreen.ANOMALIES }
                }
            }
        }
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = ParallelPalette.SurfaceRaised,
            title = { Text("Settings", color = MaterialTheme.colorScheme.onBackground) },
            text = { Text("Parallel · Early access", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = { TextButton(onClick = { showSettingsDialog = false }) { Text("Done") } }
        )
    }

    pendingDelete?.let { world ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            shape = RoundedCornerShape(12.dp),
            containerColor = ParallelPalette.SurfaceRaised,
            title = { Text("Delete this world?", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Text(
                    "${world.name} will be removed from this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            WorldRepository.delete(context, world.id)
                            pendingDelete = null
                        } catch (_: Exception) {
                            pendingDelete = null
                            storageError = "Couldn't delete this world. Check device storage and try again."
                        }
                    }
                }) { Text("Delete", color = ParallelPalette.Error) }
            }
        )
    }

    storageError?.let { message ->
        AlertDialog(
            onDismissRequest = { storageError = null },
            shape = RoundedCornerShape(12.dp),
            containerColor = ParallelPalette.SurfaceRaised,
            title = { Text("Storage unavailable", color = ParallelPalette.TextPrimary) },
            text = { Text(message, color = ParallelPalette.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { storageError = null }) { Text("OK") }
            }
        )
    }
}

private fun WorldSection.toAppScreen(): AppScreen = when (this) {
    WorldSection.OVERVIEW -> AppScreen.OVERVIEW
    WorldSection.TIMELINE -> AppScreen.TIMELINE
    WorldSection.LOCATIONS -> AppScreen.LOCATIONS
    WorldSection.CHARACTERS -> AppScreen.CHARACTERS
    WorldSection.ANOMALIES -> AppScreen.ANOMALIES
}

@Composable
private fun HomeScreen(
    worlds: List<ParallelWorld>,
    isLoading: Boolean,
    loadFailed: Boolean,
    onCreateWorld: () -> Unit,
    onSettings: () -> Unit,
    onRetryLoad: () -> Unit,
    onDeleteWorld: (ParallelWorld) -> Unit,
    onOpenWorld: (ParallelWorld) -> Unit
) {
    AdaptivePage {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PARALLEL",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(54.dp))
        Text(
            text = "What if your world\nhad taken another path?",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(14.dp))
        Text("Reimagine the places you know.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(30.dp))

        Button(
            onClick = onCreateWorld,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ParallelPalette.TextPrimary, contentColor = Color(0xFF11131A))
        ) {
            Text("CREATE A WORLD", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(48.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("MY WORLDS", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f).height(1.dp).background(ParallelPalette.Outline))
            if (worlds.isNotEmpty()) {
                Spacer(Modifier.width(10.dp))
                Text(worlds.size.toString().padStart(2, '0'), color = ParallelPalette.TextMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(18.dp))

        when {
            isLoading -> WorldLoadingState()
            loadFailed -> WorldLoadErrorState(onRetryLoad)
            worlds.isEmpty() -> AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 12 }
            ) { EmptyWorldsCard() }
            else -> worlds.forEach { world ->
                WorldDraftCard(
                    world = world,
                    onClick = { onOpenWorld(world) },
                    onDelete = { onDeleteWorld(world) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun WorldLoadingState() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp).semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(17.dp),
            strokeWidth = 2.dp,
            color = ParallelPalette.Accent
        )
        Spacer(Modifier.width(12.dp))
        Text("Restoring saved worlds…", color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun WorldLoadErrorState(onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ParallelPalette.Surface),
        border = BorderStroke(1.dp, ParallelPalette.Outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Worlds couldn't load", color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(3.dp))
                Text("Parallel couldn't read its local library.", color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun EmptyWorldsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ParallelPalette.Surface),
        border = BorderStroke(1.dp, ParallelPalette.Outline)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(46.dp).background(ParallelPalette.AccentSurface, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Explore, contentDescription = null, tint = ParallelPalette.Accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("No worlds yet", color = ParallelPalette.TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(7.dp))
            Text(
                "Your alternate stories will live here.\nStart with a place you know.",
                color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WorldDraftCard(world: ParallelWorld, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ParallelPalette.Surface),
        border = BorderStroke(1.dp, ParallelPalette.Outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("ALTERNATE WORLD", color = ParallelPalette.Accent, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    world.name,
                    modifier = Modifier.semantics { heading() },
                    color = ParallelPalette.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Text(world.country + "  ·  " + world.currentYear, color = ParallelPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete ${world.name}",
                    tint = ParallelPalette.TextMuted
                )
            }
        }
    }
}
