package com.parallel.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalWorldGeneratorTest {
    @Test
    fun sameInputsProduceTheSameCompleteWorld() {
        val first = LocalWorldGenerator.generate(
            location = "Tokyo",
            country = "Japan",
            year = "2026",
            premise = "Tokyo never became a major railway hub"
        )
        val second = LocalWorldGenerator.generate(
            location = "Tokyo",
            country = "Japan",
            year = "2026",
            premise = "Tokyo never became a major railway hub"
        )

        assertEquals(first, second)
        assertEquals(2026, first.currentYear)
        assertTrue(first.divergenceYear < first.currentYear)
    }

    @Test
    fun rejectedRailHubPremiseProducesDistributedMobility() {
        val world = LocalWorldGenerator.generate(
            location = "Tokyo",
            country = "Japan",
            year = "2026",
            premise = "Tokyo never became one of the world's major railway hubs"
        )

        assertTrue(world.alternateTimelineSummary.contains("never became a single dominant rail hub"))
        assertTrue(world.government.system.contains("district"))
        assertTrue(world.economy.sectors.contains("regional logistics"))
        assertFalse(world.alternateTimelineSummary.contains("principal interchange"))
        assertFalse(world.alternateTimelineSummary.contains("dense network of lines"))
        assertEquals(3, world.importantLocations.size)
        assertEquals(3, world.importantCharacters.size)
        assertTrue(world.strangeAnomalies.isNotEmpty())
    }

    @Test
    fun differentPremisesChangeTheGeneratedSetting() {
        val noRailHub = LocalWorldGenerator.generate(
            location = "Tokyo",
            country = "Japan",
            year = "2026",
            premise = "Tokyo never became a major railway hub"
        )
        val noInternet = LocalWorldGenerator.generate(
            location = "Tokyo",
            country = "Japan",
            year = "2026",
            premise = "Tokyo never adopted the internet"
        )

        assertNotEquals(noRailHub.name, noInternet.name)
        assertNotEquals(noRailHub.technology.level, noInternet.technology.level)
        assertNotEquals(noRailHub.alternateTimelineSummary, noInternet.alternateTimelineSummary)
    }

    @Test
    fun timelineEventsAreOrderedAndDoNotExceedTheCurrentYear() {
        val world = LocalWorldGenerator.generate(
            location = "Reykjavik",
            country = "Iceland",
            year = "2026",
            premise = "The city built a climate adaptation compact"
        )
        val years = world.majorHistoricalEvents.map { it.year }

        assertTrue(years.isNotEmpty())
        assertEquals(years.sorted(), years)
        assertTrue(years.all { it in world.divergenceYear..world.currentYear })
    }

    @Test
    fun timelineDetailsAreGeneratedForDifferentWorlds() {
        val scenarios = listOf(
            Triple("Tokyo", "Japan", "Tokyo never became a major railway hub"),
            Triple("Reykjavik", "Iceland", "The city built a climate adaptation compact"),
            Triple("Delhi", "India", "Local assemblies never gave up authority"),
            Triple("Porto", "Portugal", "A public network became the city's digital utility")
        )

        val worlds = scenarios.map { (location, country, premise) ->
            LocalWorldGenerator.generate(location, country, "2026", premise)
        }

        worlds.forEach { world ->
            val events = world.majorHistoricalEvents
            assertTrue(events.isNotEmpty())
            assertEquals(events.map { it.year }.sorted(), events.map { it.year })
            assertTrue(events.all { it.year in world.divergenceYear..world.currentYear })
            events.forEach { event ->
                assertTrue(event.title.isNotBlank())
                assertTrue(event.description.isNotBlank())
                assertTrue(event.historicalSignificance.isNotBlank())
            }
        }

        assertEquals(scenarios.map { it.first }.toSet(), worlds.map { it.location }.toSet())
        assertTrue(worlds.map { it.majorHistoricalEvents.first().historicalSignificance }.toSet().size > 1)
    }

    @Test
    fun locationsExplainTheirRoleInDifferentAlternateTimelines() {
        val distributedCity = LocalWorldGenerator.generate(
            location = "Tokyo",
            country = "Japan",
            year = "2026",
            premise = "Tokyo never became a major railway hub"
        )
        val railHubCity = LocalWorldGenerator.generate(
            location = "Kyoto",
            country = "Japan",
            year = "2026",
            premise = "The region built a major railway hub"
        )

        listOf(distributedCity, railHubCity).forEach { world ->
            assertTrue(world.importantLocations.isNotEmpty())
            world.importantLocations.forEach { location ->
                assertTrue(location.name.isNotBlank())
                assertTrue(location.description.isNotBlank())
                assertTrue(location.whyItMatters.isNotBlank())
            }
        }
        assertNotEquals(
            distributedCity.importantLocations.first().whyItMatters,
            railHubCity.importantLocations.first().whyItMatters
        )
    }

    @Test
    fun charactersHaveGroundedProfilesForDifferentWorldThemes() {
        val worlds = listOf(
            LocalWorldGenerator.generate("Tokyo", "Japan", "2026", "Tokyo never became a major railway hub"),
            LocalWorldGenerator.generate("Reykjavik", "Iceland", "2026", "The city built a climate adaptation compact"),
            LocalWorldGenerator.generate("Delhi", "India", "2026", "Local assemblies never gave up authority"),
            LocalWorldGenerator.generate("Porto", "Portugal", "2026", "A public network became the city's digital utility")
        )

        worlds.forEach { world ->
            assertEquals(3, world.importantCharacters.size)
            world.importantCharacters.forEach { character ->
                assertTrue(character.name.isNotBlank())
                assertTrue(character.role.isNotBlank())
                assertTrue(character.age == null || character.age in 1..125)
                assertTrue(character.background.isNotBlank())
                assertTrue(character.personality.isNotBlank())
                assertTrue(character.timelineRelationship.contains(world.location))
                assertTrue(character.majorActions.isNotEmpty())
                assertTrue(character.majorActions.all(String::isNotBlank))
            }
        }

        assertTrue(worlds.map { it.importantCharacters.first().role }.toSet().size > 1)
        assertTrue(worlds.map { it.importantCharacters.first().timelineRelationship }.toSet().size > 1)
    }

    @Test
    fun anomaliesAreStructuredAndFitTheirWorldThemes() {
        val worlds = listOf(
            LocalWorldGenerator.generate("Tokyo", "Japan", "2026", "Tokyo never became a major railway hub"),
            LocalWorldGenerator.generate("Reykjavik", "Iceland", "2026", "The city built a climate adaptation compact"),
            LocalWorldGenerator.generate("Delhi", "India", "2026", "Local assemblies never gave up authority"),
            LocalWorldGenerator.generate("Porto", "Portugal", "2026", "A public network became the city's digital utility")
        )

        worlds.forEach { world ->
            assertTrue(world.strangeAnomalies.isNotEmpty())
            world.strangeAnomalies.forEach { anomaly ->
                assertTrue(anomaly.name.isNotBlank())
                assertTrue(anomaly.classification.isNotBlank())
                assertTrue(anomaly.description.isNotBlank())
                assertTrue(anomaly.discovery.isNotBlank())
                assertTrue(anomaly.knownEffects.isNotEmpty())
                assertTrue(anomaly.knownEffects.all(String::isNotBlank))
                assertTrue(anomaly.currentStatus.isNotBlank())
            }
        }

        assertEquals("The Unlisted Stop", worlds.first().strangeAnomalies.first().name)
        assertTrue(worlds.map { it.strangeAnomalies.first().classification }.toSet().size > 1)
    }
}
