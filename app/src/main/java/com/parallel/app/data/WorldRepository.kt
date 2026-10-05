package com.parallel.app.data

import android.content.Context
import com.parallel.app.domain.ParallelWorld
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex

/** The UI talks to this repository instead of reading or writing the database directly. */
object WorldRepository {
    private const val LEGACY_IMPORT_COMPLETE = "room_migration_complete"
    private val legacyImportMutex = Mutex()

    fun observeWorlds(context: Context): Flow<List<ParallelWorld>> =
        WorldDatabase.get(context).savedWorldDao().observeAll().mapNotNull { rows ->
            rows.mapNotNull { WorldStore.decode(it.worldJson) }
        }

    suspend fun save(context: Context, world: ParallelWorld) {
        importLegacyWorlds(context)
        WorldDatabase.get(context).savedWorldDao().upsert(world.toEntity())
    }

    suspend fun delete(context: Context, worldId: String) {
        importLegacyWorlds(context)
        WorldDatabase.get(context).savedWorldDao().deleteById(worldId)
    }

    /** Imports existing prototype saves once so upgrading does not discard a user's worlds. */
    suspend fun importLegacyWorlds(context: Context) {
        legacyImportMutex.lock()
        try {
            val appContext = context.applicationContext
            val preferences = appContext.getSharedPreferences("parallel_worlds", Context.MODE_PRIVATE)
            if (preferences.getBoolean(LEGACY_IMPORT_COMPLETE, false)) return

            val legacyWorlds = WorldStore.load(appContext)
            if (legacyWorlds.isNotEmpty()) {
                WorldDatabase.get(appContext).savedWorldDao().upsertAll(legacyWorlds.map { it.toEntity() })
            }
            preferences.edit().putBoolean(LEGACY_IMPORT_COMPLETE, true).apply()
        } finally {
            legacyImportMutex.unlock()
        }
    }

    private fun ParallelWorld.toEntity() = SavedWorldEntity(
        id = id,
        name = name,
        location = location,
        country = country,
        currentYear = currentYear,
        worldJson = WorldStore.encode(this),
        savedAt = System.currentTimeMillis()
    )
}
