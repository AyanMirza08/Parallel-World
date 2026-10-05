package com.parallel.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntil
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parallel.app.MainActivity
import com.parallel.app.domain.LocalWorldGenerator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorldNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun everyWorldSectionOpensAndReturnsToExplorationHub() {
        createWorld()

        val destinations = listOf(
            "OVERVIEW" to "CURRENT YEAR",
            "TIMELINE" to "The events that shaped this branch of history.",
            "LOCATIONS" to "3 places in this timeline",
            "CHARACTERS" to "3 people whose lives are part of this timeline",
            "ANOMALIES" to "2 unresolved observations in this world"
        )

        destinations.forEach { (cardTitle, pageContent) ->
            composeRule.onNodeWithText(cardTitle).performClick()
            composeRule.onNodeWithText("Back to exploration").assertIsDisplayed()
            composeRule.onNodeWithText(pageContent).assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Back to exploration").performClick()
            composeRule.onNodeWithText("CHOOSE A THREAD TO EXPLORE").assertIsDisplayed()
        }
    }

    @Test
    fun systemBackFromSectionReturnsToHubThenToCreateScreen() {
        createWorld()
        composeRule.onNodeWithText("LOCATIONS").performClick()
        composeRule.onNodeWithText("3 places in this timeline").assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText("CHOOSE A THREAD TO EXPLORE").assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText("Start with a place.").assertIsDisplayed()
    }

    @Test
    fun locationListOpensDetailsAndReturnsToTheList() {
        createWorld()
        composeRule.onNodeWithText("LOCATIONS").performClick()
        composeRule.onNodeWithText("3 places in this timeline").assertIsDisplayed()

        composeRule.onNodeWithText("Old Terminal Commons").performScrollTo().performClick()
        composeRule.onNodeWithText("LOCATION FILE").assertIsDisplayed()
        composeRule.onNodeWithText("ABOUT THIS PLACE").assertIsDisplayed()
        composeRule.onNodeWithText("WHY IT MATTERS").assertIsDisplayed()
        composeRule.onNodeWithText("IMAGE PLACEHOLDER  ·  OLD TERMINAL COMMONS").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back to locations").performClick()
        composeRule.onNodeWithText("3 places in this timeline").assertIsDisplayed()
        composeRule.onNodeWithText("Old Terminal Commons").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back to exploration").performClick()
        composeRule.onNodeWithText("CHOOSE A THREAD TO EXPLORE").assertIsDisplayed()
    }

    @Test
    fun characterListOpensDetailsAndReturnsToTheList() {
        createWorld()
        composeRule.onNodeWithText("CHARACTERS").performClick()
        composeRule.onNodeWithText("3 people whose lives are part of this timeline").assertIsDisplayed()

        composeRule.onNodeWithText("Transit planner").performScrollTo().performClick()
        composeRule.onNodeWithText("CHARACTER FILE").assertIsDisplayed()
        composeRule.onNodeWithText("BACKGROUND").assertIsDisplayed()
        composeRule.onNodeWithText("PERSONALITY").assertIsDisplayed()
        composeRule.onNodeWithText("RELATIONSHIP TO THIS TIMELINE").assertIsDisplayed()
        composeRule.onNodeWithText("MAJOR ACTIONS").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back to characters").performClick()
        composeRule.onNodeWithText("3 people whose lives are part of this timeline").assertIsDisplayed()
        composeRule.onNodeWithText("Transit planner").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back to exploration").performClick()
        composeRule.onNodeWithText("CHOOSE A THREAD TO EXPLORE").assertIsDisplayed()
    }

    @Test
    fun anomalyListOpensDetailsAndReturnsToTheList() {
        createWorld()
        composeRule.onNodeWithText("ANOMALIES").performClick()
        composeRule.onNodeWithText("2 unresolved observations in this world").assertIsDisplayed()

        composeRule.onNodeWithText("The Unlisted Stop").performScrollTo().performClick()
        composeRule.onNodeWithText("ANOMALY RECORD").assertIsDisplayed()
        composeRule.onNodeWithText("DESCRIPTION").assertIsDisplayed()
        composeRule.onNodeWithText("DISCOVERY").assertIsDisplayed()
        composeRule.onNodeWithText("KNOWN EFFECTS").assertIsDisplayed()
        composeRule.onNodeWithText("CURRENT STATUS").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back to anomalies").performClick()
        composeRule.onNodeWithText("2 unresolved observations in this world").assertIsDisplayed()
        composeRule.onNodeWithText("The Unlisted Stop").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back to exploration").performClick()
        composeRule.onNodeWithText("CHOOSE A THREAD TO EXPLORE").assertIsDisplayed()
    }

    @Test
    fun savedWorldSurvivesActivityRecreationCanBeOpenedAndDeleted() {
        val worldName = LocalWorldGenerator.generate(
            location = "Tokyo",
            country = "Japan",
            year = null,
            premise = "Tokyo never became a major railway hub."
        ).name

        createWorld()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("MY WORLDS").assertIsDisplayed()
        composeRule.onNodeWithText(worldName).assertIsDisplayed()

        // Recreate the activity to exercise a close/reopen of the UI while Room remains the source of truth.
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(worldName).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText(worldName).performClick()
        composeRule.onNodeWithText("CHOOSE A THREAD TO EXPLORE").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithContentDescription("Delete $worldName").performClick()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(worldName).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText(worldName).assertDoesNotExist()
    }

    private fun createWorld() {
        composeRule.onNodeWithText("CREATE A WORLD").performClick()
        composeRule.onNodeWithText("e.g. Tokyo").performTextInput("Tokyo")
        composeRule.onNodeWithText("e.g. Japan").performTextInput("Japan")
        composeRule.onNodeWithText("Tokyo never became one of the world's major railway hubs.").performTextInput(
            "Tokyo never became a major railway hub."
        )
        composeRule.onNodeWithText("GENERATE WORLD").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("CHOOSE A THREAD TO EXPLORE")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
