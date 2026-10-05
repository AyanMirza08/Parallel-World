package com.parallel.app.data

import android.content.Context
import com.parallel.app.domain.LocalWorldGenerator
import com.parallel.app.domain.ParallelWorld
import com.parallel.app.domain.WorldAnomaly
import com.parallel.app.domain.WorldCharacter
import com.parallel.app.domain.WorldCulture
import com.parallel.app.domain.WorldEconomy
import com.parallel.app.domain.WorldEvent
import com.parallel.app.domain.WorldGovernment
import com.parallel.app.domain.WorldLocation
import com.parallel.app.domain.WorldCoordinates
import com.parallel.app.domain.WorldTechnology
import org.json.JSONArray
import org.json.JSONObject

/** JSON codec for Room records and a reader for worlds saved by the earlier preferences prototype. */
object WorldStore {
    private const val PREFERENCES = "parallel_worlds"
    private const val WORLDS_KEY = "saved_worlds"
    private const val SCHEMA_VERSION = 6

    fun load(context: Context): List<ParallelWorld> {
        val raw = context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(WORLDS_KEY, null) ?: return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    runCatching { add(array.getJSONObject(index).toWorld()) }
                }
            }
        }.getOrDefault(emptyList())
    }

    /** Encodes a complete world for storage as one Room record. */
    fun encode(world: ParallelWorld): String = world.toJson().toString()

    /** Decodes a Room record, returning null if a record cannot be recovered. */
    fun decode(encoded: String): ParallelWorld? = runCatching {
        JSONObject(encoded).toWorld()
    }.getOrNull()

    private fun ParallelWorld.toJson() = JSONObject().apply {
        put("schemaVersion", SCHEMA_VERSION)
        put("id", id)
        put("name", name)
        put("location", location)
        put("country", country)
        put("alternateTimelineSummary", alternateTimelineSummary)
        put("divergencePoint", divergencePoint)
        put("divergenceYear", divergenceYear)
        put("currentYear", currentYear)
        put("population", population)
        put("government", JSONObject().put("system", government.system).put("description", government.description))
        put("economy", JSONObject().put("description", economy.description).put("majorSectors", economy.majorSectors.toJsonArray()))
        put("culture", JSONObject().put("description", culture.description).put("definingTraits", culture.definingTraits.toJsonArray()))
        put("technology", JSONObject().put("level", technology.level).put("description", technology.description))
        put("majorHistoricalEvents", JSONArray().apply {
            majorHistoricalEvents.forEach {
                put(JSONObject()
                    .put("year", it.year)
                    .put("title", it.title)
                    .put("description", it.description)
                    .put("historicalSignificance", it.historicalSignificance))
            }
        })
        put("importantLocations", JSONArray().apply {
            importantLocations.forEach { location ->
                put(JSONObject()
                    .put("name", location.name)
                    .put("description", location.description)
                    .put("whyItMatters", location.whyItMatters)
                    .put("coordinates", location.coordinates?.let { coordinates ->
                        JSONObject().put("latitude", coordinates.latitude).put("longitude", coordinates.longitude)
                    } ?: JSONObject.NULL))
            }
        })
        put("importantCharacters", JSONArray().apply {
            importantCharacters.forEach { character ->
                put(JSONObject()
                    .put("name", character.name)
                    .put("role", character.role)
                    .put("age", character.age ?: JSONObject.NULL)
                    .put("background", character.background)
                    .put("personality", character.personality)
                    .put("timelineRelationship", character.timelineRelationship)
                    .put("majorActions", character.majorActions.toJsonArray()))
            }
        })
        put("strangeAnomalies", JSONArray().apply {
            strangeAnomalies.forEach { anomaly ->
                put(JSONObject()
                    .put("name", anomaly.name)
                    .put("classification", anomaly.classification)
                    .put("description", anomaly.description)
                    .put("discovery", anomaly.discovery)
                    .put("knownEffects", anomaly.knownEffects.toJsonArray())
                    .put("currentStatus", anomaly.currentStatus))
            }
        })
    }

    private fun JSONObject.toWorld(): ParallelWorld {
        // Worlds saved by the earlier prototype are regenerated from their original inputs.
        if (!has("schemaVersion")) {
            val oldYear = optString("year").takeUnless { it.isBlank() || it == "null" }
            return LocalWorldGenerator.generate(
                location = optString("location"),
                country = optString("country"),
                year = oldYear,
                premise = optString("premise")
            )
        }

        val governmentJson = getJSONObject("government")
        val economyJson = getJSONObject("economy")
        val cultureJson = getJSONObject("culture")
        val technologyJson = getJSONObject("technology")
        return ParallelWorld(
            id = optString("id"),
            name = optString("name"),
            location = optString("location"),
            country = optString("country"),
            alternateTimelineSummary = optString("alternateTimelineSummary"),
            divergencePoint = optString("divergencePoint"),
            divergenceYear = optInt("divergenceYear"),
            currentYear = optInt("currentYear"),
            population = optLong("population"),
            government = WorldGovernment(governmentJson.optString("system"), governmentJson.optString("description")),
            economy = WorldEconomy(economyJson.optString("description"), economyJson.getJSONArray("majorSectors").strings()),
            culture = WorldCulture(cultureJson.optString("description"), cultureJson.getJSONArray("definingTraits").strings()),
            technology = WorldTechnology(technologyJson.optString("level"), technologyJson.optString("description")),
            majorHistoricalEvents = getJSONArray("majorHistoricalEvents").objects {
                WorldEvent(
                    year = optInt("year"),
                    title = optString("title"),
                    description = optString("description"),
                    historicalSignificance = optString(
                        "historicalSignificance",
                        "This event established a lasting direction for the city's institutions and daily life."
                    )
                )
            },
            importantLocations = getJSONArray("importantLocations").objects {
                val coordinatesJson = optJSONObject("coordinates")
                val coordinates = coordinatesJson?.let {
                    val latitude = it.optDouble("latitude", Double.NaN)
                    val longitude = it.optDouble("longitude", Double.NaN)
                    if (latitude in -90.0..90.0 && longitude in -180.0..180.0) {
                        WorldCoordinates(latitude, longitude)
                    } else {
                        null
                    }
                }
                WorldLocation(
                    name = optString("name"),
                    description = optString("description"),
                    whyItMatters = optString(
                        "whyItMatters",
                        "This place shows how the alternate timeline changed daily life in the city."
                    ),
                    coordinates = coordinates
                )
            },
            importantCharacters = getJSONArray("importantCharacters").objects {
                WorldCharacter(
                    name = optString("name"),
                    role = optString("role"),
                    age = if (isNull("age")) null else optInt("age").takeIf { it in 1..125 },
                    background = optString("background").ifBlank {
                        optString("description", "Background details were not recorded in this saved world.")
                    },
                    personality = optString("personality", "Personality details were not recorded in this saved world."),
                    timelineRelationship = optString(
                        "timelineRelationship",
                        "Their work continued through the turning point recorded in this world."
                    ),
                    majorActions = optJSONArray("majorActions")?.strings().orEmpty()
                )
            },
            strangeAnomalies = getJSONArray("strangeAnomalies").objects {
                WorldAnomaly(
                    name = optString("name"),
                    classification = optString("classification", "Unclassified"),
                    description = optString("description"),
                    discovery = optString("discovery", "The original discovery record was not preserved."),
                    knownEffects = optJSONArray("knownEffects")?.strings().orEmpty(),
                    currentStatus = optString("currentStatus", "Unresolved · under observation")
                )
            }
        )
    }

    private fun List<String>.toJsonArray() = JSONArray().also { array -> forEach { array.put(it) } }

    private fun JSONArray.strings(): List<String> = buildList {
        for (index in 0 until length()) add(optString(index))
    }

    private inline fun <T> JSONArray.objects(map: JSONObject.() -> T): List<T> = buildList {
        for (index in 0 until length()) add(getJSONObject(index).map())
    }
}
