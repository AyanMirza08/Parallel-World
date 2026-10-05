package com.parallel.app.domain

/** Canonical world record. Generation and persistence use the same API-facing data shape. */
data class ParallelWorld(
    val id: String,
    val name: String,
    val location: String,
    val country: String,
    val alternateTimelineSummary: String,
    val divergencePoint: String,
    val divergenceYear: Int,
    val currentYear: Int,
    val population: Long,
    val government: WorldGovernment,
    val economy: WorldEconomy,
    val culture: WorldCulture,
    val technology: WorldTechnology,
    val majorHistoricalEvents: List<WorldEvent>,
    val importantLocations: List<WorldLocation>,
    val importantCharacters: List<WorldCharacter>,
    val strangeAnomalies: List<WorldAnomaly>
)

data class WorldGovernment(val system: String, val description: String)

data class WorldEconomy(val description: String, val majorSectors: List<String>)

data class WorldCulture(val description: String, val definingTraits: List<String>)

data class WorldTechnology(val level: String, val description: String)

data class WorldEvent(
    val year: Int,
    val title: String,
    val description: String,
    val historicalSignificance: String
)

data class WorldLocation(
    val name: String,
    val description: String,
    val whyItMatters: String,
    val coordinates: WorldCoordinates? = null
)

/** Geographic data stays independent of any particular map provider or SDK. */
data class WorldCoordinates(val latitude: Double, val longitude: Double)

data class WorldCharacter(
    val name: String,
    val role: String,
    val age: Int?,
    val background: String,
    val personality: String,
    val timelineRelationship: String,
    val majorActions: List<String>
)

data class WorldAnomaly(
    val name: String,
    val classification: String,
    val description: String,
    val discovery: String,
    val knownEffects: List<String>,
    val currentStatus: String
)
