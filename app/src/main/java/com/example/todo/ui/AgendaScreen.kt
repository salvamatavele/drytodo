package com.example.todo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo.data.Task
import com.example.todo.ui.theme.*
import com.example.todo.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    viewModel: TaskViewModel,
    onTaskClick: (Task) -> Unit,
    onTaskComplete: (Task) -> Unit,
    onBack: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToStats: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    val tasks by viewModel.allTasks.collectAsState()
    
    val dateTasks = tasks.filter { DateUtils.isSameDay(it.dueDate, selectedDate.timeInMillis) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agenda", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(onClick = onNavigateToTasks) {
                        Icon(Icons.Default.List, contentDescription = "Tarefas", tint = Color.Gray)
                    }
                    IconButton(onClick = { /* Already here */ }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Agenda", tint = Primary)
                    }
                    IconButton(onClick = onNavigateToStats) {
                        Icon(Icons.Default.BarChart, contentDescription = "Estatísticas", tint = Color.Gray)
                    }
                }
            }
        },
        containerColor = Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CalendarView(
                selectedDate = selectedDate,
                allTasks = tasks,
                onDateSelected = { selectedDate = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Agenda para " + SimpleDateFormat("d 'de' MMMM", Locale("pt", "MZ")).format(selectedDate.time),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            if (dateTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sem tarefas para este dia", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dateTasks) { task ->
                        AgendaTaskItem(task, onTaskClick, onTaskComplete)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarView(selectedDate: Calendar, allTasks: List<Task>, onDateSelected: (Calendar) -> Unit) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("pt", "MZ"))
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthFormat.format(selectedDate.time).replaceFirstChar { it.uppercase() },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Row {
                IconButton(onClick = { 
                    val newDate = selectedDate.clone() as Calendar
                    newDate.add(Calendar.MONTH, -1)
                    onDateSelected(newDate)
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                }
                IconButton(onClick = { 
                    val newDate = selectedDate.clone() as Calendar
                    newDate.add(Calendar.MONTH, 1)
                    onDateSelected(newDate)
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { day ->
                Text(day, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            val weekCalendar = selectedDate.clone() as Calendar
            weekCalendar.set(Calendar.DAY_OF_WEEK, weekCalendar.firstDayOfWeek)
            
            for (i in 0..6) {
                val isSelected = DateUtils.isSameDay(weekCalendar.timeInMillis, selectedDate.timeInMillis)
                val dayNum = weekCalendar.get(Calendar.DAY_OF_MONTH)
                val currentWeekDay = weekCalendar.clone() as Calendar
                
                val dayTasks = allTasks.filter { DateUtils.isSameDay(it.dueDate, currentWeekDay.timeInMillis) }
                val hasTasksOnDay = dayTasks.isNotEmpty()
                
                // Determine dot color based on highest priority task of the day
                val dotColor = when {
                    dayTasks.any { it.priority == "URGENTE" } -> PriorityUrgent
                    dayTasks.any { it.priority == "ALTA" } -> PriorityHigh
                    dayTasks.any { it.priority == "NORMAL" } -> PriorityNormal
                    else -> PriorityLow
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Primary else Color.Transparent)
                            .clickable { onDateSelected(currentWeekDay) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayNum.toString(),
                            color = if (isSelected) Color.White else Color.Black,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (hasTasksOnDay) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else dotColor)
                        )
                    }
                }
                weekCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }
}

@Composable
fun AgendaTaskItem(task: Task, onClick: (Task) -> Unit, onComplete: (Task) -> Unit) {
    val urgencyColor = when(task.priority) {
        "URGENTE" -> PriorityUrgent
        "ALTA" -> PriorityHigh
        "NORMAL" -> PriorityNormal
        else -> PriorityLow
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(task) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(task.dueDate)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(20.dp)
                        .background(urgencyColor.copy(alpha = 0.3f))
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (task.isCompleted) Color.Gray else Color.Black
                )
                Text(task.category, fontSize = 12.sp, color = Color.Gray)
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (task.isCompleted) urgencyColor else Color.Transparent)
                    .then(if (!task.isCompleted) Modifier.background(urgencyColor.copy(alpha = 0.1f)) else Modifier)
                    .clickable { onComplete(task) },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Side urgency bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(urgencyColor)
            )
        }
    }
}
