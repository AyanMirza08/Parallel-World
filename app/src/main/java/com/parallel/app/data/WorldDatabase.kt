package com.parallel.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** One row per generated world; the JSON column preserves the full structured world model. */
@Entity(tableName = "saved_worlds")
data class SavedWorldEntity(
    @PrimaryKey val id: String,
    val name: String,
    val location: String,
    val country: String,
    val currentYear: Int,
    val worldJson: String,
    val savedAt: Long
)

@Dao
interface SavedWorldDao {
    @Query("SELECT * FROM saved_worlds ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<SavedWorldEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(world: SavedWorldEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(worlds: List<SavedWorldEntity>)

    @Query("DELETE FROM saved_worlds WHERE id = :worldId")
    suspend fun deleteById(worldId: String)
}

@Database(entities = [SavedWorldEntity::class], version = 1, exportSchema = false)
abstract class WorldDatabase : RoomDatabase() {
    abstract fun savedWorldDao(): SavedWorldDao

    companion object {
        @Volatile private var instance: WorldDatabase? = null

        fun get(context: Context): WorldDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                WorldDatabase::class.java,
                "parallel_worlds.db"
            ).build().also { instance = it }
        }
    }
}
