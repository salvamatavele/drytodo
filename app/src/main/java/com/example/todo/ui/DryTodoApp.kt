package com.example.todo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo.R
import com.example.todo.data.Task
import com.example.todo.util.DateUtils
import com.example.todo.util.NotificationUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DryTodoApp(viewModel: TaskViewModel) {
    val tasks by viewModel.allTasks.collectAsState()
    var showTaskDialog by remember { mutableStateOf<Task?>(null) }
    var isAddingTask by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("HOJE") }

    val context = LocalContext.current

    val filteredTasks = when (selectedFilter) {
        "PASSADO" -> tasks.filter { it.dueDate < DateUtils.startOfDay() && !it.isCompleted }
        "HOJE" -> tasks.filter { DateUtils.isSameDay(it.dueDate, System.currentTimeMillis()) }
        "FUTURO" -> tasks.filter { it.dueDate > DateUtils.endOfDay() }
        "RECORRENTE" -> tasks.filter { it.isRecurring }
        else -> tasks
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    // Uses logo.png from drawable, falls back to icon if not found during compilation
                    Box(modifier = Modifier.padding(start = 16.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "DRYTODO",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                },
                title = { 
                    Text("DRYTODO", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingTask = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Tarefa")
            }
        },
        containerColor = Color(0xFFF8F5FF)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TaskCircleSelector(selectedFilter) { selectedFilter = it }

            if (filteredTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma tarefa encontrada", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTasks) { task ->
                        TaskCard(
                            task = task,
                            onComplete = { viewModel.completeTask(task) },
                            onDelete = { viewModel.deleteTask(task) },
                            onClick = { showTaskDialog = task }
                        )
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
                onConfirm = { title, desc, date, isRec, pattern ->
                    if (showTaskDialog != null) {
                        viewModel.updateTask(showTaskDialog!!.copy(
                            title = title,
                            description = desc,
                            dueDate = date,
                            isRecurring = isRec,
                            recurrencePattern = pattern
                        ))
                    } else {
                        viewModel.addTask(title, desc, date, isRec, pattern)
                        NotificationUtils.scheduleNotification(context, title, date)
                    }
                    isAddingTask = false
                    showTaskDialog = null
                }
            )
        }
    }
}

@Composable
fun TaskCircleSelector(selected: String, onSelect: (String) -> Unit) {
    val filters = listOf("PASSADO", "HOJE", "FUTURO", "RECORRENTE")
    val labels = listOf("Passado", "Hoje", "Futuro", "Hábitos")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        filters.forEachIndexed { index, filter ->
            val isSelected = selected == filter
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(filter) }
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = when(filter) {
                        "PASSADO" -> Icons.Default.History
                        "HOJE" -> Icons.Default.Today
                        "FUTURO" -> Icons.Default.Event
                        else -> Icons.Default.Repeat
                    },
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                )
                Text(
                    text = labels[index],
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}

@Composable
fun TaskCard(task: Task, onComplete: (Task) -> Unit, onDelete: (Task) -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onComplete(task) }) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Completar",
                    tint = if (task.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) Color.Gray else Color.Black
                )
                Text(
                    text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(task.dueDate)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            IconButton(onClick = { onDelete(task) }) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.6f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditDialog(
    task: Task? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Boolean, String?) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var desc by remember { mutableStateOf(task?.description ?: "") }
    var dueDate by remember { mutableStateOf(task?.dueDate ?: System.currentTimeMillis()) }
    var isRecurring by remember { mutableStateOf(task?.isRecurring ?: false) }
    var pattern by remember { mutableStateOf(task?.recurrencePattern ?: "DIÁRIO") }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().apply { timeInMillis = dueDate }.get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().apply { timeInMillis = dueDate }.get(Calendar.MINUTE)
    )
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task == null) "Nova Tarefa" else "Editar Tarefa", fontWeight = FontWeight.Bold) },
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
                    onConfirm(title, desc, cal.timeInMillis, isRecurring, if(isRecurring) pattern else null) 
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
                TextButton(onClick = { showTimePicker = false }) { Text("OK") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}
