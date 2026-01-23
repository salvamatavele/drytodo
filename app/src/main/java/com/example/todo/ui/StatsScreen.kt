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
import com.example.todo.data.TaskLog
import com.example.todo.ui.theme.*
import com.example.todo.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    val logs by viewModel.allLogs.collectAsState()

    var selectedInterval by remember { mutableStateOf("Sempre") }
    val intervals = listOf("Hoje", "Últimos 7 dias", "Este Mês", "Sempre")
    var showIntervalPicker by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, selectedInterval) {
        val now = System.currentTimeMillis()
        val startOfToday = DateUtils.startOfDay()
        
        when (selectedInterval) {
            "Hoje" -> logs.filter { it.date >= startOfToday }
            "Últimos 7 dias" -> {
                val sevenDaysAgo = startOfToday - (7 * 24 * 60 * 60 * 1000L)
                logs.filter { it.date >= sevenDaysAgo }
            }
            "Este Mês" -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                val startOfMonth = cal.timeInMillis
                logs.filter { it.date >= startOfMonth }
            }
            else -> logs
        }
    }

    val totalLogged = filteredLogs.size
    val completedLogged = filteredLogs.count { it.wasCompleted }
    val averageCompletion = if (totalLogged > 0) filteredLogs.sumOf { it.completionPercentage } / totalLogged else 0
    val successRate = if (totalLogged > 0) (completedLogged.toFloat() / totalLogged * 100).toInt() else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance e Histórico", fontWeight = FontWeight.Bold, color = PurpleDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = PurpleDark)
                    }
                },
                actions = {
                    TextButton(onClick = { showIntervalPicker = true }) {
                        Text(selectedInterval, color = Primary, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                PerformanceCard(successRate, averageCompletion, totalLogged, completedLogged)
            }

            if (filteredLogs.isNotEmpty()) {
                item {
                    Text("Histórico de Actividades", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PurpleDark)
                }
                
                items(filteredLogs) { log ->
                    LogStatItem(log)
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("Sem dados para este período", color = Color.Gray)
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showIntervalPicker) {
            AlertDialog(
                onDismissRequest = { showIntervalPicker = false },
                title = { Text("Selecionar Período") },
                text = {
                    Column {
                        intervals.forEach { interval ->
                            ListItem(
                                headlineContent = { Text(interval) },
                                modifier = Modifier.clickable {
                                    selectedInterval = interval
                                    showIntervalPicker = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

@Composable
fun PerformanceCard(successRate: Int, avgProgress: Int, total: Int, completed: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Taxa de Sucesso (100%)", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    Text("$successRate%", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { avgProgress / 100f },
                        modifier = Modifier.size(70.dp),
                        color = Color.White,
                        strokeWidth = 8.dp,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    Text("${avgProgress}%", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Text("Progresso Médio", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItemSmall("Instâncias", total.toString())
                StatItemSmall("Concluídas", completed.toString())
                StatItemSmall("Incompletas", (total - completed).toString())
            }
        }
    }
}

@Composable
fun StatItemSmall(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LogStatItem(log: TaskLog) {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("pt", "MZ"))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background((if (log.wasCompleted) PriorityLow else PriorityUrgent).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (log.wasCompleted) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (log.wasCompleted) PriorityLow else PriorityUrgent
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(log.taskTitle, fontWeight = FontWeight.Bold, color = PurpleDark, fontSize = 15.sp)
                Text(sdf.format(Date(log.date)), fontSize = 12.sp, color = Color.Gray)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("${log.completionPercentage}%", fontWeight = FontWeight.Bold, color = if (log.wasCompleted) Primary else Color.Gray)
                Text(if (log.wasCompleted) "Concluída" else "Pendente", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}
