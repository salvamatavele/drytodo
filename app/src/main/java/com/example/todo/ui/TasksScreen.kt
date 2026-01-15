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
import com.example.todo.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TaskViewModel,
    onTaskClick: (Task) -> Unit,
    onTaskComplete: (Task) -> Unit,
    onBack: () -> Unit,
    onNavigateToAgenda: () -> Unit,
    onNavigateToStats: () -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    
    val todayTasks = tasks.filter { DateUtils.isSameDay(it.dueDate, System.currentTimeMillis()) }
    val upcomingTasks = tasks.filter { it.dueDate > DateUtils.endOfDay() }
    val somedayTasks = tasks.filter { it.dueDate < DateUtils.startOfDay() && !it.isCompleted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tarefas Activas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // Internal Navigation for Tasks context
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(onClick = { /* Already here */ }) {
                        Icon(Icons.Default.List, contentDescription = "Tarefas", tint = Color(0xFF6200EE))
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
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (todayTasks.isNotEmpty()) {
                item { SectionHeader("Para Hoje", "${todayTasks.size} TAREFAS") }
                items(todayTasks) { task -> TaskListItem(task, onTaskClick, onTaskComplete) }
            }
            if (upcomingTasks.isNotEmpty()) {
                item { SectionHeader("Próximas", "") }
                items(upcomingTasks) { task -> TaskListItem(task, onTaskClick, onTaskComplete) }
            }
            if (somedayTasks.isNotEmpty()) {
                item { SectionHeader("Algum dia", "") }
                items(somedayTasks) { task -> TaskListItem(task, onTaskClick, onTaskComplete) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, badge: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (badge.isNotEmpty()) {
            Surface(
                color = Color(0xFF6200EE).copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    badge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = Color(0xFF6200EE),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TaskListItem(task: Task, onClick: (Task) -> Unit, onComplete: (Task) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF5F5F5))
            .clickable { onClick(task) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (task.isCompleted) {
                Box(
                    modifier = Modifier.width(4.dp).height(60.dp).background(Color(0xFF6200EE))
                )
            }
            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(10.dp).clip(CircleShape).background(
                            when (task.priority) {
                                "URGENTE" -> Color.Red
                                "ALTA" -> Color(0xFFFF9800)
                                else -> Color(0xFF6200EE)
                            }
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        task.title,
                        color = if (task.isCompleted) Color.Gray else Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(task.category, fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = { onComplete(task) }) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (task.isCompleted) Color(0xFF6200EE) else Color.LightGray
                )
            }
        }
    }
}
