package com.example.todo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo.R
import com.example.todo.data.Task
import com.example.todo.util.NotificationUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DryTodoApp(viewModel: TaskViewModel) {
    var showTaskDialog by remember { mutableStateOf<Task?>(null) }
    var isAddingTask by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf("HOME") }
    var focusTask by remember { mutableStateOf<Task?>(null) }
    var isFocusModeActive by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (isFocusModeActive) {
        FocusModeScreen(
            initialTask = focusTask,
            allTasks = viewModel.allTasks.collectAsState().value,
            onBack = { 
                isFocusModeActive = false
                focusTask = null
            },
            onComplete = { task ->
                viewModel.toggleTaskCompletion(task, context)
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                if (currentScreen != "TASKS" && currentScreen != "AGENDA" && currentScreen != "STATS") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Surface(
                            color = Color.White,
                            shadowElevation = 15.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(85.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BottomNavItem("Home", Icons.Default.Home, currentScreen == "HOME") { currentScreen = "HOME" }
                                BottomNavItem("Agenda", Icons.Default.CalendarMonth, currentScreen == "AGENDA") { currentScreen = "AGENDA" }

                                Spacer(modifier = Modifier.width(64.dp))

                                BottomNavItem("Focar", Icons.Default.CenterFocusStrong, currentScreen == "FOCUS") { 
                                    focusTask = null
                                    isFocusModeActive = true
                                }
                                BottomNavItem("Tarefas", Icons.Default.List, currentScreen == "TASKS") { currentScreen = "TASKS" }
                            }
                        }

                        FloatingActionButton(
                            onClick = { isAddingTask = true },
                            containerColor = Color(0xFF6200EE),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier
                                .offset(y = (-30).dp)
                                .size(64.dp),
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Adicionar", modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(if (currentScreen == "TASKS" || currentScreen == "AGENDA" || currentScreen == "STATS") PaddingValues(0.dp) else innerPadding)) {
                when (currentScreen) {
                    "HOME" -> DashboardScreen(
                        viewModel = viewModel,
                        onTaskClick = { showTaskDialog = it },
                        onFocusTask = { 
                            focusTask = it
                            isFocusModeActive = true
                        },
                        onTaskComplete = { viewModel.toggleTaskCompletion(it, context) },
                        onAddSuggestion = { title, desc, due, isRec, pattern ->
                            viewModel.addTaskWithNotification(context, title, desc, due, isRec, pattern)
                        },
                        onSeeAllClick = { currentScreen = "TASKS" }
                    )
                    "TASKS" -> TasksScreen(
                        viewModel = viewModel,
                        onTaskClick = { showTaskDialog = it },
                        onFocusTask = { 
                            focusTask = it
                            isFocusModeActive = true
                        },
                        onToggleTask = { viewModel.toggleTaskCompletion(it, context) },
                        onDeleteTask = { viewModel.deleteTask(it, context) },
                        onBack = { currentScreen = "HOME" },
                        onNavigateToAgenda = { currentScreen = "AGENDA" },
                        onNavigateToStats = { currentScreen = "STATS" }
                    )
                    "AGENDA" -> AgendaScreen(
                        viewModel = viewModel,
                        onTaskClick = { showTaskDialog = it },
                        onFocusTask = { 
                            focusTask = it
                            isFocusModeActive = true
                        },
                        onTaskComplete = { viewModel.toggleTaskCompletion(it, context) },
                        onBack = { currentScreen = "HOME" },
                        onNavigateToTasks = { currentScreen = "TASKS" },
                        onNavigateToStats = { currentScreen = "STATS" }
                    )
                    "STATS" -> StatsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = "HOME" }
                    )
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Ecrã em desenvolvimento")
                                if (currentScreen != "HOME") {
                                    Button(onClick = { currentScreen = "HOME" }) {
                                        Text("Voltar ao Início")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isAddingTask || showTaskDialog != null) {
                TaskEditDialog(
                    task = showTaskDialog,
                    onDismiss = {
                        isAddingTask = false
                        showTaskDialog = null
                    },
                    onConfirm = { title, desc, due, isRec, pattern, priority, category ->
                        if (showTaskDialog != null) {
                            viewModel.updateTask(showTaskDialog!!.copy(
                                title = title,
                                description = desc,
                                dueDate = due,
                                isRecurring = isRec,
                                recurrencePattern = pattern,
                                priority = priority,
                                category = category
                            ), context)
                        } else {
                            viewModel.addTaskWithNotification(context, title, desc, due, isRec, pattern, priority, category)
                        }
                        isAddingTask = false
                        showTaskDialog = null
                    },
                    onStartFocus = {
                        focusTask = it
                        isFocusModeActive = true
                        showTaskDialog = null
                        isAddingTask = false
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickableNoRipple { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF6200EE) else Color(0xFFBDBDBD),
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF6200EE) else Color(0xFFBDBDBD)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditDialog(
    task: Task? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Boolean, String?, String, String) -> Unit,
    onStartFocus: (Task) -> Unit = {}
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var desc by remember { mutableStateOf(task?.description ?: "") }
    var dueDate by remember { mutableStateOf(task?.dueDate ?: System.currentTimeMillis()) }
    var isRecurring by remember { mutableStateOf(task?.isRecurring ?: false) }
    var pattern by remember { mutableStateOf(task?.recurrencePattern ?: "DIÁRIO") }
    var priority by remember { mutableStateOf(task?.priority ?: "NORMAL") }
    var category by remember { mutableStateOf(task?.category ?: "Pessoal") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().apply { timeInMillis = dueDate }.get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().apply { timeInMillis = dueDate }.get(Calendar.MINUTE)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (task == null) "Nova Tarefa" else "Editar Tarefa", fontWeight = FontWeight.Bold)
                if (task != null) {
                    IconButton(onClick = { onStartFocus(task) }) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Focar", tint = Color(0xFF6200EE))
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Text("Data e Hora", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dueDate)), fontSize = 12.sp)
                        }
                    }
                    OutlinedCard(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(dueDate)), fontSize = 12.sp)
                        }
                    }
                }

                Text("Prioridade", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("BAIXA", "NORMAL", "ALTA", "URGENTE").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p, fontSize = 10.sp) }
                        )
                    }
                }

                Text("Categoria", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val categories = listOf("Pessoal", "Trabalho", "Saúde", "Outros")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c, fontSize = 10.sp) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isRecurring, onCheckedChange = { isRecurring = it })
                    Text("Tarefa Recorrente")
                }
                
                if (isRecurring) {
                    val patterns = listOf("DIÁRIO", "SEMANAL", "MENSAL")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        patterns.forEach { p ->
                            FilterChip(
                                selected = pattern == p,
                                onClick = { pattern = p },
                                label = { Text(p, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = datePickerState.selectedDateMillis ?: dueDate
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)

                    onConfirm(title, desc, cal.timeInMillis, isRecurring, if(isRecurring) pattern else null, priority, category) 
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (task == null) "Adicionar" else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    dueDate = datePickerState.selectedDateMillis ?: dueDate
                    showDatePicker = false 
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    val cal = Calendar.getInstance().apply { timeInMillis = dueDate }
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    dueDate = cal.timeInMillis
                    showTimePicker = false 
                }) { Text("OK") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}
