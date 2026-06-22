package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiService
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = NoteRepository(db)

    // Current app screens: "calibration_intro", "calibration_sentences", "calibration_complete", "main_editor", "notes_list", "settings"
    private val _currentScreen = MutableStateFlow("calibration_intro")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Notes lists & Searches
    val searchQuery = MutableStateFlow("")
    val allNotes: StateFlow<List<Note>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allNotes
            } else {
                repository.searchNotes(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App State for Settings
    val userSettings = repository.userSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings()
    )

    // Handwriting Profile state
    val handwritingProfileState = repository.handwritingProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Calibration steps & variables
    val calibrationSentences = listOf(
        "The quick brown fox jumps over the lazy dog",
        "The lazy brown dog quickly jumps over the brown fox",
        "The brown jumps quickly over the lazy dog and fox"
    )
    val currentSentenceIndex = MutableStateFlow(0)
    
    // Stroking state for current Calibration sentence
    val calibrationStrokes = MutableStateFlow<List<List<Offset>>>(emptyList())
    val isAnalyzingSentence = MutableStateFlow(false)
    val calibrationError = MutableStateFlow<String?>(null)

    // Individual sentence reports
    private val sentenceReports = mutableListOf<String>()
    val finalSlant = MutableStateFlow("Straight")
    val finalSpacing = MutableStateFlow("Normal")
    val finalNeatness = MutableStateFlow(7)

    // Note Creator variables
    val currentNoteId = MutableStateFlow<Long?>(null)
    val noteTitle = MutableStateFlow("")
    val noteBody = MutableStateFlow("")
    val noteContextSummary = MutableStateFlow("")
    val noteLiteralMeanings = MutableStateFlow("")
    val noteGeneratedPrompt = MutableStateFlow("")

    val isEditorDrawing = MutableStateFlow(false) // Toggle between canvas and typing
    val editorStrokes = MutableStateFlow<List<List<Offset>>>(emptyList())

    // Progress indications
    val isTranscribing = MutableStateFlow(false)
    val isGeneratingContext = MutableStateFlow(false)
    val isDefiningWords = MutableStateFlow(false)
    val isGeneratingPrompt = MutableStateFlow(false)

    init {
        // Evaluate if user is already calibrated
        viewModelScope.launch {
            val profile = repository.getProfileSync()
            if (profile != null && profile.isCalibrated) {
                _currentScreen.value = "main_editor"
            } else {
                _currentScreen.value = "calibration_intro"
            }

            // Ensure settings exists
            val existingSettings = repository.getSettingsSync()
            if (existingSettings == null) {
                repository.saveSettings(UserSettings())
            }
        }
    }

    // Navigation helper
    fun navigateTo(screen: String) {
        _currentScreen.value = screen
        if (screen == "main_editor" && currentNoteId.value == null) {
            clearEditor()
        }
    }

    // --- Handwriting Calibration flow ---

    fun startCalibration() {
        currentSentenceIndex.value = 0
        calibrationStrokes.value = emptyList()
        sentenceReports.clear()
        calibrationError.value = null
        _currentScreen.value = "calibration_sentences"
    }

    fun clearCalibrationStrokes() {
        calibrationStrokes.value = emptyList()
    }

    fun submitCalibrationSentence(width: Int, height: Int) {
        val strokes = calibrationStrokes.value
        if (strokes.isEmpty()) {
            calibrationError.value = "Please write the sentence on the canvas first."
            return
        }

        isAnalyzingSentence.value = true
        calibrationError.value = null

        val sentenceText = calibrationSentences[currentSentenceIndex.value]
        val bitmap = createBitmapFromStrokes(strokes, width, height)

        viewModelScope.launch {
            try {
                val result = GeminiService.analyzeCalibrationSentence(sentenceText, bitmap)
                sentenceReports.add("Sentence ${currentSentenceIndex.value + 1}: ${result.report}")
                
                // Aggregate slant and spacing
                if (currentSentenceIndex.value == 2) {
                    finalSlant.value = result.slant
                    finalSpacing.value = result.spacing
                    finalNeatness.value = result.neatness
                }

                if (currentSentenceIndex.value < 2) {
                    currentSentenceIndex.value += 1
                    calibrationStrokes.value = emptyList()
                } else {
                    // All 3 sentences completed
                    saveCalibratedProfile()
                    _currentScreen.value = "calibration_complete"
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Calibration analysis failed", e)
                calibrationError.value = "Communication failed. Retrying offline profiling parameters."
                
                // Default gracefully so user isn't stuck
                sentenceReports.add("Analyzing handwritten metrics...")
                if (currentSentenceIndex.value < 2) {
                    currentSentenceIndex.value += 1
                    calibrationStrokes.value = emptyList()
                } else {
                    saveCalibratedProfile()
                    _currentScreen.value = "calibration_complete"
                }
            } finally {
                isAnalyzingSentence.value = false
            }
        }
    }

    private suspend fun saveCalibratedProfile() {
        val compositeReport = sentenceReports.joinToString("\n\n")
        val profile = HandwritingProfile(
            id = 1,
            isCalibrated = true,
            calibrationReport = compositeReport,
            writingSlant = finalSlant.value,
            charSpacing = if (finalSpacing.value == "Tight") 0.12f else 0.20f,
            wordSpacing = if (finalSpacing.value == "Tight") 0.20f else 0.35f,
            writingSpeed = 1.4f,
            averagePressure = 1.0f
        )
        repository.saveProfile(profile)
    }

    // --- Notes Management & Engines ---

    fun loadNote(note: Note) {
        currentNoteId.value = note.id
        noteTitle.value = note.title
        noteBody.value = note.content
        noteContextSummary.value = note.contextSummary ?: ""
        noteLiteralMeanings.value = note.literalMeanings ?: ""
        noteGeneratedPrompt.value = note.generatedPrompt ?: ""
        
        editorStrokes.value = emptyList()
        isEditorDrawing.value = false
        _currentScreen.value = "main_editor"
    }

    fun startNewNote() {
        clearEditor()
        _currentScreen.value = "main_editor"
    }

    fun clearEditor() {
        currentNoteId.value = null
        noteTitle.value = ""
        noteBody.value = ""
        noteContextSummary.value = ""
        noteLiteralMeanings.value = ""
        noteGeneratedPrompt.value = ""
        editorStrokes.value = emptyList()
        isEditorDrawing.value = false
    }

    fun toggleEditorDrawing() {
        isEditorDrawing.value = !isEditorDrawing.value
    }

    fun clearEditorStrokes() {
        editorStrokes.value = emptyList()
    }

    /**
     * Pipeline 1: Transcribe handwritten drawing canvas into text
     */
    fun transcribeStrokesToText(width: Int, height: Int) {
        val strokes = editorStrokes.value
        if (strokes.isEmpty()) return

        isTranscribing.value = true
        val bitmap = createBitmapFromStrokes(strokes, width, height)

        viewModelScope.launch {
            try {
                val profile = repository.getProfileSync()
                val transcription = GeminiService.transcribeHandwriting(bitmap, profile?.calibrationReport)
                if (transcription.isNotBlank() && !transcription.startsWith("Error")) {
                    noteBody.value = if (noteBody.value.isBlank()) transcription else "${noteBody.value}\n$transcription"
                    isEditorDrawing.value = false // back to text editor to see results
                    editorStrokes.value = emptyList() // clear
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Ocr failed", e)
            } finally {
                isTranscribing.value = false
            }
        }
    }

    /**
     * Pipeline 2: Literal Meaning Engine (DICTIONARY definitions)
     */
    fun extractLiteralMeanings() {
        if (noteBody.value.isBlank()) return
        isDefiningWords.value = true

        viewModelScope.launch {
            try {
                val definitions = GeminiService.getLiteralMeanings(noteTitle.value, noteBody.value)
                noteLiteralMeanings.value = definitions
                saveCurrentNote()
            } catch (e: Exception) {
                Log.e("ViewModel", "Meaning engine failed", e)
            } finally {
                isDefiningWords.value = false
            }
        }
    }

    /**
     * Pipeline 3: Context Engine
     */
    fun generateContextSummary() {
        if (noteBody.value.isBlank()) return
        isGeneratingContext.value = true

        viewModelScope.launch {
            try {
                val summary = GeminiService.getContextSummary(noteTitle.value, noteBody.value)
                noteContextSummary.value = summary
                saveCurrentNote()
            } catch (e: Exception) {
                Log.e("ViewModel", "Context engine failed", e)
            } finally {
                isGeneratingContext.value = false
            }
        }
    }

    /**
     * Pipeline 4: Prompt Generation Engine (Generates optimized AI Propmts)
     */
    fun generateAIPrompt() {
        if (noteBody.value.isBlank()) return
        
        viewModelScope.launch {
            // Ensure we have a context summary first
            if (noteContextSummary.value.isBlank()) {
                isGeneratingContext.value = true
                try {
                    val summary = GeminiService.getContextSummary(noteTitle.value, noteBody.value)
                    noteContextSummary.value = summary
                } catch (e: Exception) {
                    noteContextSummary.value = "A customized note regarding workspace concepts."
                } finally {
                    isGeneratingContext.value = false
                }
            }

            // Also trigger literal meanings in background if empty (adds depth to notes)
            if (noteLiteralMeanings.value.isBlank()) {
                launch {
                    try {
                        val definitions = GeminiService.getLiteralMeanings(noteTitle.value, noteBody.value)
                        noteLiteralMeanings.value = definitions
                        saveCurrentNote()
                    } catch (e: Exception) { /* ignore safe background */ }
                }
            }

            isGeneratingPrompt.value = true
            try {
                val promptResult = GeminiService.generatePrompt(
                    noteTitle.value,
                    noteBody.value,
                    noteContextSummary.value
                )
                noteGeneratedPrompt.value = promptResult
                saveCurrentNote()
            } catch (e: Exception) {
                Log.e("ViewModel", "Prompt engine failed", e)
                noteGeneratedPrompt.value = "Failed to generate optimized prompt automatically. Please check your networks."
            } finally {
                isGeneratingPrompt.value = false
            }
        }
    }

    fun saveCurrentNote(onSaved: () -> Unit = {}) {
        val titleText = noteTitle.value.ifBlank { "Untitled Note" }
        val bodyText = noteBody.value

        viewModelScope.launch {
            val note = Note(
                id = currentNoteId.value ?: 0L,
                title = titleText,
                content = bodyText,
                contextSummary = noteContextSummary.value.ifBlank { null },
                generatedPrompt = noteGeneratedPrompt.value.ifBlank { null },
                literalMeanings = noteLiteralMeanings.value.ifBlank { null }
            )
            val newId = repository.insertOrUpdateNote(note)
            currentNoteId.value = newId
            onSaved()
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNoteById(note.id)
            if (currentNoteId.value == note.id) {
                clearEditor()
            }
        }
    }

    // --- Settings / Calibration Remap ---

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            val settings = (userSettings.value ?: UserSettings()).copy(isDarkMode = enabled)
            repository.saveSettings(settings)
        }
    }

    fun triggerRecalibration() {
        viewModelScope.launch {
            repository.clearProfile()
            navigateTo("calibration_intro")
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            repository.clearProfile()
            // clear notes
            val notes = allNotes.value
            for (note in notes) {
                repository.deleteNoteById(note.id)
            }
            clearEditor()
            navigateTo("calibration_intro")
        }
    }

    // --- Offline Bitmap Stroke Exporter Utility ---

    private fun createBitmapFromStrokes(strokes: List<List<Offset>>, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(
            width.coerceAtLeast(100),
            height.coerceAtLeast(100),
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        val paintBg = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

        val paintStroke = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 6f
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            isAntiAlias = true
        }

        for (stroke in strokes) {
            if (stroke.size < 2) continue
            val path = android.graphics.Path().apply {
                moveTo(stroke.first().x, stroke.first().y)
                for (i in 1 until stroke.size) {
                    lineTo(stroke[i].x, stroke[i].y)
                }
            }
            canvas.drawPath(path, paintStroke)
        }
        return bitmap
    }
}
