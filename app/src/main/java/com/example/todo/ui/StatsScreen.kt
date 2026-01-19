package com.example.todo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()

    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val pendingTasks = totalTasks - completedTasks
    val completionRate = if (totalTasks > 0) (completedTasks.toFloat() / totalTasks * 100).toInt() else 0

    val urgentTasks = tasks.count { it.priority == "URGENTE" && !it.isCompleted }
    val highPriorityTasks = tasks.count { it.priority == "ALTA" && !it.isCompleted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estatísticas", fontWeight = FontWeight.Bold, color = PurpleDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = PurpleDark)
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
                SummaryCard(totalTasks, completedTasks, completionRate)
            }

            item {
                Text("Prioridades Pendentes", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PurpleDark)
                Spacer(modifier = Modifier.height(12.dp))
                PriorityStatsRow(urgentTasks, highPriorityTasks)
            }

            item {
                Text("Produtividade por Categoria", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PurpleDark)
                Spacer(modifier = Modifier.height(12.dp))
                CategoryBreakdown(tasks)
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SummaryCard(total: Int, completed: Int, rate: Int) {
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
                    Text("Taxa de Conclusão", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text("$rate%", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
                CircularProgressIndicator(
                    progress = { rate / 100f },
                    modifier = Modifier.size(60.dp),
                    color = Color.White,
                    strokeWidth = 6.dp,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Total", total.toString())
                StatItem("Concluídas", completed.toString())
                StatItem("Pendentes", (total - completed).toString())
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PriorityStatsRow(urgent: Int, high: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Urgentes",
            value = urgent.toString(),
            color = PriorityUrgent
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Alta",
            value = high.toString(),
            color = PriorityHigh
        )
    }
}

@Composable
fun StatCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(value, color = PurpleDark, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CategoryBreakdown(tasks: List<Task>) {
    val categories = tasks.groupBy { it.category }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (tasks.isEmpty()) {
                Text("Sem dados disponíveis", color = Color.Gray, fontSize = 14.sp)
            } else {
                categories.forEach { (category, catTasks) ->
                    val catTotal = catTasks.size
                    val catCompleted = catTasks.count { it.isCompleted }
                    val catRate = (catCompleted.toFloat() / catTotal)
                    
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(category, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PurpleDark)
                            Text("$catCompleted/$catTotal", fontSize = 12.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { catRate },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = Primary,
                            trackColor = Primary.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}
