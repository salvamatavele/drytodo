package com.example.todo.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo.data.Task
import com.example.todo.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(
    initialTask: Task?,
    allTasks: List<Task>,
    onBack: () -> Unit,
    onComplete: (Task) -> Unit
) {
    var currentTask by remember { mutableStateOf(initialTask) }
    var timeRemaining by remember { mutableLongStateOf(25 * 60 * 1000L) }
    var isRunning by remember { mutableStateOf(false) }
    var totalTime by remember { mutableLongStateOf(25 * 60 * 1000L) }
    var showTaskPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val progress by animateFloatAsState(
        targetValue = if (totalTime > 0) timeRemaining.toFloat() / totalTime else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "TimerProgress"
    )

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (timeRemaining > 0) {
                delay(1000L)
                timeRemaining -= 1000L
            }
            isRunning = false
            // Notify user
            notifyFinished(context)
        }
    }

    val minutes = floor(timeRemaining / 60000.0).toInt()
    val seconds = floor((timeRemaining % 60000) / 1000.0).toInt()
    val timeText = "%02d:%02d".format(minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A237E), Color(0xFF3F51B5))
                )
            )
    ) {
        // Top Bar Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Sair", tint = Color.White)
            }
            
            TextButton(onClick = { showTaskPicker = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mudar Tarefa", color = Color.White)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Task Info
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                onClick = { showTaskPicker = true }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "A FOCAR EM",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = currentTask?.title ?: "Sessão Livre",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Timer Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(280.dp)
                    .clickable { if (!isRunning) showTimePicker = true }
            ) {
                Canvas(modifier = Modifier.size(300.dp)) {
                    drawCircle(color = Color.White.copy(alpha = 0.05f), radius = size.minDimension / 2)
                }

                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White.copy(alpha = 0.1f),
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )

                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    strokeWidth = 8.dp,
                    trackColor = Color.Transparent,
                    strokeCap = StrokeCap.Round
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = timeText, color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Light)
                    if (!isRunning) {
                        Text("Tocar para ajustar", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                IconButton(
                    onClick = {
                        isRunning = false
                        timeRemaining = totalTime
                    },
                    modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                }

                LargeFloatingActionButton(
                    onClick = { isRunning = !isRunning },
                    containerColor = Color.White,
                    contentColor = Color(0xFF1A237E),
                    shape = CircleShape,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = "Ajustar", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (currentTask != null) {
                Button(
                    onClick = { onComplete(currentTask!!) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Concluir Tarefa", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Task Picker Sheet
        if (showTaskPicker) {
            ModalBottomSheet(
                onDismissRequest = { showTaskPicker = false },
                containerColor = Color.White
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Selecionar Tarefa", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                    
                    ListItem(
                        headlineContent = { Text("Sessão Livre (Sem tarefa)") },
                        leadingContent = { Icon(Icons.Default.Timer, contentDescription = null) },
                        modifier = Modifier.clickable {
                            currentTask = null
                            showTaskPicker = false
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    val pendingTasks = allTasks.filter { !it.isCompleted }
                    if (pendingTasks.isEmpty()) {
                        Text("Não há tarefas pendentes", color = Color.Gray, modifier = Modifier.padding(16.dp))
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(pendingTasks) { task ->
                                ListItem(
                                    headlineContent = { Text(task.title) },
                                    supportingContent = { Text(task.category) },
                                    modifier = Modifier.clickable {
                                        currentTask = task
                                        showTaskPicker = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Manual Time Picker Dialog
        if (showTimePicker) {
            var inputMinutes by remember { mutableStateOf((totalTime / 60000).toString()) }
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("Definir Tempo (minutos)") },
                text = {
                    OutlinedTextField(
                        value = inputMinutes,
                        onValueChange = { if (it.all { char -> char.isDigit() }) inputMinutes = it },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val mins = inputMinutes.toLongOrNull() ?: 25
                        totalTime = mins * 60 * 1000L
                        timeRemaining = totalTime
                        showTimePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

private fun notifyFinished(context: Context) {
    // Beep
    try {
        val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
    } catch (e: Exception) { e.printStackTrace() }

    // Long Vibrate
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (vibrator.hasVibrator()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1500)
        }
    }
}
