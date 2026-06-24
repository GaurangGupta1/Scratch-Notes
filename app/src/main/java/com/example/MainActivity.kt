package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val userSettings by viewModel.userSettings.collectAsState()
            val isDark = userSettings?.isDarkMode ?: true

            MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isDark) Color(0xFF121212) else Color(0xFFF4F4F6)
                ) {
                    AppScreenRouter(viewModel = viewModel, isDark = isDark)
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppScreenRouter(viewModel: MainViewModel, isDark: Boolean) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn(animationSpec = spring()) with fadeOut(animationSpec = spring())
        },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            "calibration_intro" -> CalibrationIntroScreen(viewModel, isDark)
            "calibration_sentences" -> CalibrationSentencesScreen(viewModel, isDark)
            "calibration_complete" -> CalibrationCompleteScreen(viewModel, isDark)
            "main_editor" -> MainEditorScreen(viewModel, isDark)
            "notes_list" -> NotesListScreen(viewModel, isDark)
            "settings" -> SettingsScreen(viewModel, isDark)
            else -> CalibrationIntroScreen(viewModel, isDark)
        }
    }
}

// ==========================================
// ONBOARDING / CALIBRATION INTRO
// ==========================================
@Composable
fun CalibrationIntroScreen(viewModel: MainViewModel, isDark: Boolean) {
    val paperColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val textColor = if (isDark) Color(0xFFE5E5E5) else Color(0xFF1C1C1C)
    val accentColor = if (isDark) Color(0xFFF37B43) else Color(0xFFD45E27)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = paperColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header decoration
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(accentColor.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Handwriting Onboarding Icon",
                        tint = accentColor,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "AI Scratch Notes",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Normal,
                    fontFamily = FontFamily.Serif,
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "AI Scratch Notes learns your handwriting style to better understand your notes.",
                    fontSize = 16.sp,
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = "Please write the following sentences exactly as you would normally write rough notes for yourself. Do not try to improve your handwriting.",
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { viewModel.startCalibration() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("begin_calibration_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text(
                        text = "Begin Calibration",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

// ==========================================
// CALIBRATION SENTENCES PANEL (STEP 1-3)
// ==========================================
@Composable
fun CalibrationSentencesScreen(viewModel: MainViewModel, isDark: Boolean) {
    val currentIndex by viewModel.currentSentenceIndex.collectAsState()
    val strokes by viewModel.calibrationStrokes.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingSentence.collectAsState()
    val errorMsg by viewModel.calibrationError.collectAsState()

    val currentSentence = viewModel.calibrationSentences[currentIndex]

    val paperColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val textColor = if (isDark) Color(0xFFE5E5E5) else Color(0xFF1C1C1C)
    val accentColor = if (isDark) Color(0xFFF37B43) else Color(0xFFD45E27)

    var width by remember { mutableStateOf(0) }
    var height by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF121212) else Color(0xFFF4F4F6))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Handwriting Calibration",
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "Step ${currentIndex + 1} of 3",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (currentIndex + 1) / 3f,
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.2f)
                )
            }
        },
        containerColor = if (isDark) Color(0xFF121115) else Color(0xFFFDF8F6)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Target Sentence Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = "WRITE THIS SENTENCE:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = paperColor),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = currentSentence,
                        fontSize = 18.sp,
                        fontStyle = FontStyle.Normal,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(paperColor, shape = RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        if (isDark) Color(0xFF2D2D2D) else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
            ) {
                // Background guidelines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val yStep = 45.dp.toPx()
                    val linesCount = (size.height / yStep).toInt()
                    val lineColor = accentColor.copy(alpha = 0.15f)
                    for (i in 1..linesCount) {
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, i * yStep),
                            end = Offset(size.width, i * yStep),
                            strokeWidth = 1f
                        )
                    }
                }

                // Smooth gesture canvas
                val tempStroke = remember { mutableStateOf<List<Offset>>(emptyList()) }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size ->
                            width = size.width
                            height = size.height
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    tempStroke.value = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    tempStroke.value = tempStroke.value + change.position
                                },
                                onDragEnd = {
                                    if (tempStroke.value.isNotEmpty()) {
                                        viewModel.calibrationStrokes.value =
                                            viewModel.calibrationStrokes.value + listOf(tempStroke.value)
                                        tempStroke.value = emptyList()
                                    }
                                }
                            )
                        }
                ) {
                    val inkColor = accentColor

                    // Draw historical strokes
                    for (stroke in strokes) {
                        if (stroke.size < 2) continue
                        val path = Path().apply {
                            moveTo(stroke.first().x, stroke.first().y)
                            for (i in 1 until stroke.size) {
                                lineTo(stroke[i].x, stroke[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = inkColor,
                            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    // Draw current stroke
                    if (tempStroke.value.size >= 2) {
                        val path = Path().apply {
                            moveTo(tempStroke.value.first().x, tempStroke.value.first().y)
                            for (i in 1 until tempStroke.value.size) {
                                lineTo(tempStroke.value[i].x, tempStroke.value[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = inkColor,
                            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                if (strokes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Write here using your stylus or finger...",
                            fontSize = 14.sp,
                            color = textColor.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Error display
            if (errorMsg != null) {
                Text(
                    text = errorMsg!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Commands Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.clearCalibrationStrokes() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, textColor.copy(alpha = 0.2f))
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Clear")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Canvas", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.submitCalibrationSentence(width, height) },
                    enabled = !isAnalyzing,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("submit_handwriting_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = if (isDark) Color.Black else Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Submit Step",
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.Black else Color.White
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// ANIMATED TICK MARK
// ==========================================
@Composable
fun AnimatedTickMark(modifier: Modifier = Modifier) {
    var animationPlayed by remember { mutableStateOf(false) }
    val progress = animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "TickProgress"
    )
    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Canvas(modifier = modifier.size(100.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2 - 4.dp.toPx()

        // Background circle with success green color
        drawCircle(
            color = Color(0xFF4CAF50),
            radius = radius,
            center = center,
            style = Fill
        )

        // Draw checkmark lines
        val path = Path()
        val startX = size.width * 0.30f
        val startY = size.height * 0.50f
        
        val midX = size.width * 0.45f
        val midY = size.height * 0.65f
        
        val endX = size.width * 0.70f
        val endY = size.height * 0.35f

        path.moveTo(startX, startY)
        
        val progressVal = progress.value
        if (progressVal > 0f) {
            if (progressVal <= 0.4f) {
                val p = progressVal / 0.4f
                val curX = startX + (midX - startX) * p
                val curY = startY + (midY - startY) * p
                path.lineTo(curX, curY)
            } else {
                path.lineTo(midX, midY)
                val p = (progressVal - 0.4f) / 0.6f
                val curX = midX + (endX - midX) * p
                val curY = midY + (endY - midY) * p
                path.lineTo(curX, curY)
            }
            
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(
                    width = 8.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

// ==========================================
// CALIBRATION COMPLETE SCREEN
// ==========================================
@Composable
fun CalibrationCompleteScreen(viewModel: MainViewModel, isDark: Boolean) {
    val paperColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val textColor = if (isDark) Color(0xFFE5E5E5) else Color(0xFF1C1C1C)
    val accentColor = if (isDark) Color(0xFFF37B43) else Color(0xFFD45E27)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = paperColor),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Centered Tick Mark animation
                AnimatedTickMark()

                Spacer(modifier = Modifier.height(24.dp))

                // Display the success message below the tick mark
                Text(
                    text = "Your personalized handwriting calibration metrics profile is successfully configured.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { viewModel.navigateTo("main_editor") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("start_notes_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text(
                        "Start Writing Notes",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ==========================================
// MAIN NOTE EDITOR
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainEditorScreen(viewModel: MainViewModel, isDark: Boolean) {
    val noteId by viewModel.currentNoteId.collectAsState()
    val title by viewModel.noteTitle.collectAsState()
    val body by viewModel.noteBody.collectAsState()
    val contextSummary by viewModel.noteContextSummary.collectAsState()
    val literalMeanings by viewModel.noteLiteralMeanings.collectAsState()
    val promptResult by viewModel.noteGeneratedPrompt.collectAsState()

    val isDrawing by viewModel.isEditorDrawing.collectAsState()
    val strokes by viewModel.editorStrokes.collectAsState()

    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val isDefiningWords by viewModel.isDefiningWords.collectAsState()
    val isGeneratingContext by viewModel.isGeneratingContext.collectAsState()
    val isGeneratingPrompt by viewModel.isGeneratingPrompt.collectAsState()

    val paperColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val textColor = if (isDark) Color(0xFFE5E5E5) else Color(0xFF1C1C1C)
    val accentColor = if (isDark) Color(0xFFF37B43) else Color(0xFFD45E27)

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var canvasWidth by remember { mutableStateOf(0) }
    var canvasHeight by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI Scratch Notes",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textColor
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo("notes_list") }) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Past Notes", tint = textColor)
                    }
                    IconButton(onClick = { viewModel.startNewNote() }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Note", tint = textColor)
                    }
                    IconButton(onClick = { viewModel.navigateTo("settings") }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) Color(0xFF121212) else Color(0xFFF4F4F6)
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF121212) else Color(0xFFF4F4F6))
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                // Large Generate CTA Button
                Button(
                    onClick = {
                        viewModel.saveCurrentNote {
                            viewModel.generateAIPrompt()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("generate_prompt_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    enabled = body.isNotBlank() && !isGeneratingPrompt
                ) {
                    if (isGeneratingPrompt) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = if (isDark) Color.Black else Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Optimizing AI Prompt...",
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.Black else Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (isDark) Color.Black else Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate Prompt",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.Black else Color.White
                        )
                    }
                }
            }
        },
        containerColor = if (isDark) Color(0xFF121212) else Color(0xFFF4F4F6)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Note Title Field
            TextField(
                value = title,
                onValueChange = {
                    viewModel.noteTitle.value = it
                },
                placeholder = {
                    Text(
                        "Note Title (e.g. Solar Panels)",
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        color = textColor.copy(alpha = 0.4f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedIndicatorColor = accentColor,
                    unfocusedIndicatorColor = textColor.copy(alpha = 0.1f)
                ),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Body Area / Handwriting or Typing Choice
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode description
                Text(
                    text = if (isDrawing) "HANDWRITING CANVAS" else "TYPED KEYBOARD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )

                // Toggle Button
                TextButton(
                    onClick = { viewModel.toggleEditorDrawing() },
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                ) {
                    Icon(
                        imageVector = if (isDrawing) Icons.Default.Info else Icons.Default.Edit,
                        contentDescription = "Toggle Input Method",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isDrawing) "Type Note" else "Draw Handwriting",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isDrawing) {
                // Drawing Canvas container
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(paperColor, shape = RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (isDark) Color(0xFF2D2D2D) else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        // Drawing Guidelines
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val yStep = 45.dp.toPx()
                            val lineIndices = (size.height / yStep).toInt()
                            val lineColor = accentColor.copy(alpha = 0.15f)
                            for (i in 1..lineIndices) {
                                drawLine(
                                    color = lineColor,
                                    start = Offset(0f, i * yStep),
                                    end = Offset(size.width, i * yStep),
                                    strokeWidth = 1f
                                )
                            }
                        }

                        // Ink canvas
                        val activeStroke = remember { mutableStateOf<List<Offset>>(emptyList()) }

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { size ->
                                    canvasWidth = size.width
                                    canvasHeight = size.height
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            activeStroke.value = listOf(offset)
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            activeStroke.value = activeStroke.value + change.position
                                        },
                                        onDragEnd = {
                                            if (activeStroke.value.isNotEmpty()) {
                                                viewModel.editorStrokes.value =
                                                    viewModel.editorStrokes.value + listOf(activeStroke.value)
                                                activeStroke.value = emptyList()
                                            }
                                        }
                                    )
                                }
                        ) {
                            val strokeColor = accentColor

                            for (stroke in strokes) {
                                if (stroke.size < 2) continue
                                val path = Path().apply {
                                    moveTo(stroke.first().x, stroke.first().y)
                                    for (i in 1 until stroke.size) {
                                        lineTo(stroke[i].x, stroke[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = strokeColor,
                                    style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }

                            if (activeStroke.value.size >= 2) {
                                val path = Path().apply {
                                    moveTo(activeStroke.value.first().x, activeStroke.value.first().y)
                                    for (i in 1 until activeStroke.value.size) {
                                        lineTo(activeStroke.value[i].x, activeStroke.value[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = strokeColor,
                                    style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }

                        if (strokes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Draw rough handwritten items here...",
                                    color = textColor.copy(alpha = 0.35f),
                                    fontSize = 13.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.clearEditorStrokes() },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            border = BorderStroke(1.dp, textColor.copy(alpha = 0.15f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor.copy(alpha = 0.7f))
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Canvas", fontSize = 13.sp)
                        }

                        Button(
                            onClick = { viewModel.transcribeStrokesToText(canvasWidth, canvasHeight) },
                            enabled = strokes.isNotEmpty() && !isTranscribing,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                                .testTag("transcribe_handwriting_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            if (isTranscribing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = if (isDark) Color.Black else Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Transcribe", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Transcribe Text", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.Black else Color.White)
                            }
                        }
                    }
                }
            } else {
                // Keyboard / Text Typing
                OutlinedTextField(
                    value = body,
                    onValueChange = {
                        viewModel.noteBody.value = it
                    },
                    placeholder = {
                        Text(
                            "Think naturally, write naturally. Enter scrap notes, incomplete bullet points, ideas or thoughts...",
                            fontSize = 15.sp,
                            color = textColor.copy(alpha = 0.4f),
                            fontStyle = FontStyle.Italic
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("note_content_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = paperColor,
                        unfocusedContainerColor = paperColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = accentColor.copy(alpha = 0.5f),
                        unfocusedBorderColor = textColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-Engines Quick Triggers Container (Literal meaning + Context engine)
            if (body.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Meaning Engine Button
                    OutlinedButton(
                        onClick = { viewModel.extractLiteralMeanings() },
                        enabled = !isDefiningWords,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        if (isDefiningWords) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = accentColor, strokeWidth = 1.5.dp)
                        } else {
                            Icon(imageVector = Icons.Default.List, contentDescription = "Meaning", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dictionary Lookup", fontSize = 11.sp)
                        }
                    }

                    // Context Engine Button
                    OutlinedButton(
                        onClick = { viewModel.generateContextSummary() },
                        enabled = !isGeneratingContext,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        if (isGeneratingContext) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = accentColor, strokeWidth = 1.5.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Info, contentDescription = "Context", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Analyze Context", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Literal Meanings Tray
            if (literalMeanings.isNotBlank() && body.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = paperColor),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, textColor.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Dictionary",
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "LITERAL MEANING ENGINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.5f),
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = literalMeanings,
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Context engine Result Tray
            if (contextSummary.isNotBlank() && body.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = paperColor),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, textColor.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Context Icon",
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "CONTEXT ENGINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.5f),
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = contextSummary,
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.8f),
                            lineHeight = 18.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }

            // Generated Prompt Result Box
            if (promptResult.isNotBlank() && body.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF242424) else Color(0xFFFCF5F1)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI-READY OPTIMIZED PROMPT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    letterSpacing = 1.sp
                                )
                            }

                            // Copy Button
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(promptResult))
                                    Toast.makeText(context, "Copied Prompt!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Copy Prompt",
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        SelectionContainer {
                            Text(
                                text = promptResult,
                                fontSize = 14.sp,
                                color = textColor,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "💡 Copy this prompt and paste it directly into your favorite AI model.",
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.5f),
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }

            // Save status helper button
            if (body.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            viewModel.saveCurrentNote {
                                Toast.makeText(context, "Note Saved locally!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = textColor.copy(alpha = 0.05f), contentColor = textColor),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Save Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ==========================================
// PAST NOTES LIST
// ==========================================
@Composable
fun NotesListScreen(viewModel: MainViewModel, isDark: Boolean) {
    val notes by viewModel.allNotes.collectAsState()
    val query by viewModel.searchQuery.collectAsState()

    val textColor = if (isDark) Color(0xFFE5E5E5) else Color(0xFF1C1C1C)
    val accentColor = if (isDark) Color(0xFFF37B43) else Color(0xFFD45E27)
    val paperColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)

    val formatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF121212) else Color(0xFFF4F4F6))
                    .padding(horizontal = 16.dp)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateTo("main_editor") }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Notebook Index",
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                // Search Bar
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search title or content...", fontSize = 14.sp) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search icon") },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("search_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = paperColor,
                        unfocusedContainerColor = paperColor,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.1f)
                    ),
                    singleLine = true
                )
            }
        },
        containerColor = if (isDark) Color(0xFF121212) else Color(0xFFF4F4F6)
    ) { innerPadding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "No Notes",
                        modifier = Modifier.size(60.dp),
                        tint = textColor.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (query.isEmpty()) "Your notebook is empty." else "No records match search.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (query.isEmpty()) {
                        Button(
                            onClick = { viewModel.startNewNote() },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("Write Your First Note", color = if (isDark) Color.Black else Color.White)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.loadNote(note) }
                            .testTag("note_item_${note.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = paperColor),
                        border = BorderStroke(1.dp, textColor.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = note.title.ifBlank { "Untitled Note" },
                                    fontSize = 17.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = { viewModel.deleteNote(note) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = note.content,
                                fontSize = 13.sp,
                                color = textColor.copy(alpha = 0.6f),
                                maxLines = 2,
                                lineHeight = 18.sp
                            )

                            if (!note.contextSummary.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Context: ${note.contextSummary}",
                                    fontSize = 12.sp,
                                    color = accentColor,
                                    maxLines = 1,
                                    fontStyle = FontStyle.Italic
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = formatter.format(Date(note.timestamp)),
                                fontSize = 11.sp,
                                color = textColor.copy(alpha = 0.35f)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ==========================================
// SETTINGS PAGE
// ==========================================
@Composable
fun SettingsScreen(viewModel: MainViewModel, isDark: Boolean) {
    val settings by viewModel.userSettings.collectAsState()
    val profile by viewModel.handwritingProfileState.collectAsState()

    val textColor = if (isDark) Color(0xFFE5E5E5) else Color(0xFF1C1C1C)
    val accentColor = if (isDark) Color(0xFFF37B43) else Color(0xFFD45E27)
    val paperColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)

    val context = LocalContext.current

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF121212) else Color(0xFFF4F4F6))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo("main_editor") }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Application Settings",
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        },
        containerColor = if (isDark) Color(0xFF121212) else Color(0xFFF4F4F6)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // General settings header
            Text(
                "PREFERENCES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Dark Mode switch item
            Card(
                colors = CardDefaults.cardColors(containerColor = paperColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Aesthetic", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text("Use comfortable eye-saving theme", fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
                    }
                    Switch(
                        checked = settings?.isDarkMode ?: true,
                        onCheckedChange = { viewModel.setDarkMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "HANDWRITING PROFILE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Calibration info box
            Card(
                colors = CardDefaults.cardColors(containerColor = paperColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (profile?.isCalibrated == true) "Calibration Profile: ACTIVE" else "Calibration Profile: NOT ACTIVE",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (profile?.isCalibrated == true) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your handwriting profile continuously calibrates parameters (char spacing: ${profile?.charSpacing ?: "default"}, slant: ${profile?.writingSlant ?: "default"}) to perfect visual transcription calculations.",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.triggerRecalibration() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Recalibrate", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Re-Run Handwriting Calibration", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "DATA & EXPORT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = paperColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Exporting raw JSON backup to downloads...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, textColor.copy(alpha = 0.15f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                    ) {
                        Text("Export Notes Backup")
                    }

                    Button(
                        onClick = { viewModel.deleteAccount() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear All Data & Delete History", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "AI Scratch Notes • Version 0.9 (MVP Build)",
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
