package com.example.todo.ui

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo.data.Task
import com.example.todo.ui.theme.*
import com.example.todo.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: TaskViewModel,
    onTaskClick: (Task) -> Unit,
    onFocusTask: (Task) -> Unit,
    onTaskComplete: (Task) -> Unit,
    onAddSuggestion: (String, String, Long, Boolean, String?) -> Unit,
    onSeeAllClick: () -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    val todayTasks = tasks.filter { DateUtils.isSameDay(it.dueDate, System.currentTimeMillis()) }
    val urgentTask = todayTasks.filter { !it.isCompleted }.find { it.priority == "URGENTE" } ?: todayTasks.filter { !it.isCompleted }.firstOrNull { it.priority == "ALTA" }
    
    val progress = if (todayTasks.isNotEmpty()) {
        (todayTasks.count { it.isCompleted }.toFloat() / todayTasks.size.toFloat() * 100).toInt()
    } else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            DashboardHeader(progress)
        }

        item {
            SmartSuggestionsCard(onAddSuggestion)
        }

        item {
            HighPrioritySection(urgentTask, onTaskClick, onFocusTask)
        }

        item {
            TodayTasksHeader(onSeeAllClick)
        }

        items(todayTasks) { task ->
            DashboardTaskItem(
                task = task,
                onClick = { onTaskClick(task) },
                onFocus = { onFocusTask(task) },
                onComplete = { onTaskComplete(task) }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun DashboardHeader(progress: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            val sdf = SimpleDateFormat("EEEE, d MMM", Locale("pt", "MZ"))
            Text(
                text = sdf.format(Date()).replaceFirstChar { it.uppercase() },
                color = Color.Gray,
                fontSize = 14.sp
            )
            Text(
                text = "Olá, Utilizador",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PurpleDark
            )
        }
        
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.size(50.dp),
                color = Primary,
                strokeWidth = 4.dp,
                trackColor = Color(0xFFE0E0E0)
            )
            Text(
                text = "$progress%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
    }
}

@Composable
fun SmartSuggestionsCard(onAddSuggestion: (String, String, Long, Boolean, String?) -> Unit) {
    var dismissed by remember { mutableStateOf(false) }
    if (dismissed) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sugestões Inteligentes", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PurpleDark)
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("HÁBITO RECORRENTE", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Revisão Semanal", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp), color = PurpleDark)
                Text(
                    "Geralmente feito às Sextas às 10:00. Gostaria de adicionar?",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { 
                            val startCal = Calendar.getInstance()
                            startCal.set(Calendar.HOUR_OF_DAY, 10)
                            startCal.set(Calendar.MINUTE, 0)
                            onAddSuggestion("Revisão Semanal", "Sugestão inteligente", startCal.timeInMillis, true, "SEMANAL")
                            dismissed = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Adicionar", modifier = Modifier.padding(start = 4.dp))
                    }
                    OutlinedButton(
                        onClick = { dismissed = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ignorar", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun HighPrioritySection(urgentTask: Task?, onTaskClick: (Task) -> Unit, onFocusTask: (Task) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Alta Prioridade", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PurpleDark)
        
        if (urgentTask != null) {
            val urgencyColor = when(urgentTask.priority) {
                "URGENTE" -> PriorityUrgent
                "ALTA" -> PriorityHigh
                else -> Primary
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(urgencyColor, urgencyColor.copy(alpha = 0.7f))))
                    .clickable { onTaskClick(urgentTask) }
                    .padding(20.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                urgentTask.priority,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { onFocusTask(urgentTask) }) {
                            Icon(Icons.Default.CenterFocusStrong, contentDescription = "Focar", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(urgentTask.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Hoje às ${SimpleDateFormat("HH:mm").format(Date(urgentTask.dueDate))}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallPriorityCard("Sincronia", "11:30", Icons.Default.AccessTime, Color(0xFFFF9800))
                SmallPriorityCard("Ginásio", "18:00", Icons.Default.FitnessCenter, Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun SmallPriorityCard(title: String, time: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PurpleDark)
            Text(time, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun TodayTasksHeader(onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Tarefas de Hoje", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PurpleDark)
        Text(
            "Ver Tudo", 
            color = Primary, 
            fontSize = 14.sp, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onSeeAllClick() }
        )
    }
}

@Composable
fun DashboardTaskItem(task: Task, onClick: () -> Unit, onFocus: () -> Unit, onComplete: () -> Unit) {
    val urgencyColor = when(task.priority) {
        "URGENTE" -> PriorityUrgent
        "ALTA" -> PriorityHigh
        "NORMAL" -> PriorityNormal
        else -> PriorityLow
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (task.isCompleted) urgencyColor else Color.Transparent)
                    .then(if (!task.isCompleted) Modifier.background(urgencyColor.copy(alpha = 0.1f)) else Modifier)
                    .clickable { onComplete() },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) Color.Gray else PurpleDark
                )
                Text("${task.category} • ${SimpleDateFormat("HH:mm").format(Date(task.dueDate))}", color = Color.Gray, fontSize = 12.sp)
            }

            IconButton(onClick = onFocus) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Focar", tint = Color.Gray, modifier = Modifier.size(20.dp))
            }

            // Urgency indicator bar
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
