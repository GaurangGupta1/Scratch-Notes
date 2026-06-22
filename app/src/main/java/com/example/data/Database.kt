package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val contextSummary: String? = null,
    val generatedPrompt: String? = null,
    val literalMeanings: String? = null, // Store key word meanings as JSON or text
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "handwriting_profiles")
data class HandwritingProfile(
    @PrimaryKey val id: Int = 1,
    val isCalibrated: Boolean = false,
    val calibrationReport: String? = null,
    val sentence1Strokes: String? = null, // Stroke metadata
    val sentence2Strokes: String? = null,
    val sentence3Strokes: String? = null,
    val charSpacing: Float = 0f,
    val wordSpacing: Float = 0f,
    val writingSpeed: Float = 0f, // pixels per millisecond
    val averagePressure: Float = 1.0f,
    val writingSlant: String? = "Normal"
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val isDarkMode: Boolean = true,
    val username: String = "Author"
)

// --- DAOs ---

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): Note?

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    fun searchNotes(query: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM handwriting_profiles WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<HandwritingProfile?>

    @Query("SELECT * FROM handwriting_profiles WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): HandwritingProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: HandwritingProfile)

    @Query("DELETE FROM handwriting_profiles WHERE id = 1")
    suspend fun clearProfile()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: UserSettings)
}

// --- Database ---

@Database(
    entities = [Note::class, HandwritingProfile::class, UserSettings::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun profileDao(): ProfileDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_scratch_notes_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
