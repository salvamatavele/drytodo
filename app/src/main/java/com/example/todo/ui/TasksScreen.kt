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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo.data.Task
import com.example.todo.ui.theme.*
import com.example.todo.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TaskViewModel,
    onTaskClick: (Task) -> Unit,
    onFocusTask: (Task) -> Unit,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onBack: () -> Unit,
    onNavigateToAgenda: () -> Unit,
    onNavigateToStats: () -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    
    val activeTasks = tasks.filter { !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("As minhas tarefas", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Sort options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Mais")
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
                    IconButton(onClick = { /* Already here */ }) {
                        Icon(Icons.Default.List, contentDescription = "Tarefas", tint = Primary)
                    }
                    IconButton(onClick = onNavigateToAgenda) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Agenda", tint = Color.Gray)
                    }
                    IconButton(onClick = onNavigateToStats) {
                        Icon(Icons.Default.BarChart, contentDescription = "Estatísticas", tint = Color.Gray)
                    }
                }
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (activeTasks.isNotEmpty()) {
                items(activeTasks, key = { it.id }) { task ->
                    SwipeToDeleteContainer(onDelete = { onDeleteTask(task) }) {
                        GoogleTaskItem(task, onTaskClick, onFocusTask, onToggleTask)
                    }
                }
            }

            if (completedTasks.isNotEmpty()) {
                item {
                    Text(
                        "Concluídas (${completedTasks.size})",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
                items(completedTasks, key = { it.id }) { task ->
                    SwipeToDeleteContainer(onDelete = { onDeleteTask(task) }) {
                        GoogleTaskItem(task, onTaskClick, onFocusTask, onToggleTask)
                    }
                }
            }
            
            if (tasks.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Não existem tarefas", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleTaskItem(
    task: Task,
    onClick: (Task) -> Unit,
    onFocus: (Task) -> Unit,
    onToggle: (Task) -> Unit
) {
    val urgencyColor = when(task.priority) {
        "URGENTE" -> PriorityUrgent
        "ALTA" -> PriorityHigh
        "NORMAL" -> PriorityNormal
        else -> PriorityLow
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(task) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onToggle(task) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (task.isCompleted) Primary else urgencyColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) Color.Gray else Color.Black
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.isRecurring) {
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    val sdf = SimpleDateFormat("EEE, d MMM • HH:mm", Locale("pt", "MZ"))
                    Text(
                        text = sdf.format(Date(task.dueDate)),
                        fontSize = 13.sp,
                        color = if (task.isCompleted) Color.LightGray else Color.Gray
                    )
                    if (task.category.isNotEmpty()) {
                        Text(" • ", fontSize = 13.sp, color = Color.Gray)
                        Text(task.category, fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }

            IconButton(onClick = { onFocus(task) }) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Focar", tint = Color.LightGray, modifier = Modifier.size(20.dp))
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color.White,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
        content = { content() }
    )
}
