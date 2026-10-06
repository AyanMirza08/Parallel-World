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
            val initialDatabase = Room.databaseBuilder(context, WorldDatabase::class.java, databaseName)
                .build()
            try {
                initialDatabase.savedWorldDao().upsert(world)
            } finally {
                initialDatabase.close()
            }

            val reopenedDatabase = Room.databaseBuilder(context, WorldDatabase::class.java, databaseName)
                .build()
            try {
                assertEquals(listOf(world), reopenedDatabase.savedWorldDao().observeAll().first())
                reopenedDatabase.savedWorldDao().deleteById(world.id)
                assertEquals(emptyList<SavedWorldEntity>(), reopenedDatabase.savedWorldDao().observeAll().first())
            } finally {
                reopenedDatabase.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }
}
