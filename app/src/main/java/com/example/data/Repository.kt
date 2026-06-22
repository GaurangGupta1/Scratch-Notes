package com.example.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val db: AppDatabase) {
    val noteDao = db.noteDao()
    val profileDao = db.profileDao()
    val settingsDao = db.settingsDao()

    // Notes
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    suspend fun insertOrUpdateNote(note: Note): Long {
        return if (note.id == 0L) {
            noteDao.insertNote(note)
        } else {
            noteDao.updateNote(note)
            note.id
        }
    }

    suspend fun deleteNoteById(id: Long) {
        noteDao.deleteNoteById(id)
    }

    // Handwriting Profile
    val handwritingProfile: Flow<HandwritingProfile?> = profileDao.getProfile()

    suspend fun getProfileSync(): HandwritingProfile? = profileDao.getProfileSync()

    suspend fun saveProfile(profile: HandwritingProfile) {
        profileDao.saveProfile(profile)
    }

    suspend fun clearProfile() {
        profileDao.clearProfile()
    }

    // Settings
    val userSettings: Flow<UserSettings?> = settingsDao.getSettings()

    suspend fun getSettingsSync(): UserSettings? = settingsDao.getSettingsSync()

    suspend fun saveSettings(settings: UserSettings) {
        settingsDao.saveSettings(settings)
    }
}
