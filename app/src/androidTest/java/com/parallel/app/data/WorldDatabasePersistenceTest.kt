package com.parallel.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class WorldDatabasePersistenceTest {
    @Test
    fun savedWorldSurvivesDatabaseReopenAndCanBeDeleted() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "parallel-persistence-${UUID.randomUUID()}.db"
        val world = SavedWorldEntity(
            id = "test-world",
            name = "Tokyo: The Inland Exchange",
            location = "Tokyo",
            country = "Japan",
            currentYear = 2026,
            worldJson = "{}",
            savedAt = 1L
        )

        try {
            Room.databaseBuilder(context, WorldDatabase::class.java, databaseName)
                .build().use { database ->
                    database.savedWorldDao().upsert(world)
                }

            Room.databaseBuilder(context, WorldDatabase::class.java, databaseName)
                .build().use { database ->
                    assertEquals(listOf(world), database.savedWorldDao().observeAll().first())
                    database.savedWorldDao().deleteById(world.id)
                    assertEquals(emptyList<SavedWorldEntity>(), database.savedWorldDao().observeAll().first())
                }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }
}
