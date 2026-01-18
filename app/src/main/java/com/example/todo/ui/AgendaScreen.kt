package com.example.todo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo.data.Task
import com.example.todo.ui.theme.*
import com.example.todo.util.DateUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    viewModel: TaskViewModel,
    onTaskClick: (Task) -> Unit,
    onFocusTask: (Task) -> Unit,
    onTaskComplete: (Task) -> Unit,
    onBack: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToStats: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    val tasks by viewModel.allTasks.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Group all tasks by day for the "Entire Agenda" view
    val groupedTasks = remember(tasks) {
        tasks.groupBy { task ->
            Calendar.getInstance().apply {
                timeInMillis = task.startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.toSortedMap()
    }

    val overdueTasksCount = tasks.count { it.endDate < System.currentTimeMillis() && !it.isCompleted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Agenda Inteligente", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            SimpleDateFormat("MMMM yyyy", Locale("pt", "MZ")).format(selectedDate.time).replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (overdueTasksCount > 0) {
                        IconButton(onClick = { viewModel.rescheduleOverdueTasks(context) }) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = "Otimizar Atrasadas", tint = PriorityUrgent)
                        }
                    }
                    IconButton(onClick = { 
                        selectedDate = Calendar.getInstance()
                        val todayMillis = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        
                        var index = 0
                        var found = false
                        for (date in groupedTasks.keys) {
                            if (date >= todayMillis) {
                                found = true
                                break
                            }
                            index += 1 + (groupedTasks[date]?.size ?: 0)
                        }
                        if (found) {
                            scope.launch { listState.animateScrollToItem(index) }
                        }
                    }) {
                        Icon(Icons.Default.Today, contentDescription = "Hoje")
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
            CalendarMonthView(
                selectedDate = selectedDate,
                allTasks = tasks,
                onDateSelected = { date ->
                    selectedDate = date
                    val targetMillis = date.clone() as Calendar
                    targetMillis.set(Calendar.HOUR_OF_DAY, 0)
                    targetMillis.set(Calendar.MINUTE, 0)
                    targetMillis.set(Calendar.SECOND, 0)
                    targetMillis.set(Calendar.MILLISECOND, 0)
                    
                    var index = 0
                    var found = false
                    for (dMillis in groupedTasks.keys) {
                        if (dMillis >= targetMillis.timeInMillis) {
                            found = true
                            break
                        }
                        index += 1 + (groupedTasks[dMillis]?.size ?: 0)
                    }
                    if (found) {
                        scope.launch { listState.animateScrollToItem(index) }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventAvailable, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Sem tarefas agendadas", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    groupedTasks.forEach { (dateMillis, tasksOnDay) ->
                        item(key = "header_$dateMillis") {
                            DateHeader(dateMillis)
                        }
                        items(tasksOnDay, key = { it.id }) { task ->
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                                AgendaTaskItem(task, onTaskClick, onFocusTask, onTaskComplete)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarMonthView(selectedDate: Calendar, allTasks: List<Task>, onDateSelected: (Calendar) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(bottom = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { day ->
                Text(day, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
            }
        }

        val calendar = selectedDate.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val offset = firstDayOfMonth - 1
        val gridCalendar = calendar.clone() as Calendar
        gridCalendar.add(Calendar.DAY_OF_MONTH, -offset)

        val totalDaysToShow = if (offset + daysInMonth > 35) 42 else 35

        for (row in 0 until (totalDaysToShow / 7)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                for (col in 0..6) {
                    val currentDay = gridCalendar.clone() as Calendar
                    val isSelected = DateUtils.isSameDay(currentDay.timeInMillis, selectedDate.timeInMillis)
                    val isToday = DateUtils.isSameDay(currentDay.timeInMillis, System.currentTimeMillis())
                    val isCurrentMonth = currentDay.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH)
                    val dayNum = currentDay.get(Calendar.DAY_OF_MONTH)
                    
                    val dayTasks = allTasks.filter { DateUtils.isSameDay(it.startDate, currentDay.timeInMillis) }
                    val hasTasksOnDay = dayTasks.isNotEmpty()
                    
                    val dotColor = when {
                        dayTasks.any { it.priority == "URGENTE" && !it.isCompleted } -> PriorityUrgent
                        dayTasks.any { it.priority == "ALTA" && !it.isCompleted } -> PriorityHigh
                        dayTasks.all { it.isCompleted } && hasTasksOnDay -> Color.LightGray
                        else -> PriorityNormal
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(40.dp).padding(vertical = 1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Primary else if (isToday) Primary.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { onDateSelected(currentDay) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNum.toString(),
                                color = when {
                                    isSelected -> Color.White
                                    isToday -> Primary
                                    isCurrentMonth -> Color.Black
                                    else -> Color.LightGray
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        if (hasTasksOnDay) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else dotColor)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(5.dp))
                        }
                    }
                    gridCalendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                val newDate = selectedDate.clone() as Calendar
                newDate.add(Calendar.MONTH, -1)
                onDateSelected(newDate)
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Anterior", fontSize = 11.sp)
            }
            TextButton(onClick = {
                val newDate = selectedDate.clone() as Calendar
                newDate.add(Calendar.MONTH, 1)
                onDateSelected(newDate)
            }) {
                Text("Próximo", fontSize = 11.sp)
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun DateHeader(dateMillis: Long) {
    val isToday = DateUtils.isSameDay(dateMillis, System.currentTimeMillis())
    val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "MZ"))
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isToday) "Hoje" else sdf.format(Date(dateMillis)).replaceFirstChar { it.uppercase() },
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isToday) Primary else Color.Black
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.LightGray.copy(alpha = 0.3f)))
    }
}

@Composable
fun AgendaTaskItem(task: Task, onClick: (Task) -> Unit, onFocus: (Task) -> Unit, onComplete: (Task) -> Unit) {
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(45.dp)) {
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(task.startDate)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(urgencyColor.copy(alpha = 0.2f))
                )
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(task.endDate)),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (task.isCompleted) Color.Gray else Color.Black
                )
                Text(task.category, fontSize = 11.sp, color = Color.Gray)
            }

            IconButton(onClick = { onFocus(task) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Focar", tint = Color.Gray, modifier = Modifier.size(20.dp))
            }

            IconButton(
                onClick = { onComplete(task) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (task.isCompleted) urgencyColor else Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(urgencyColor)
            )
        }
    }
}
