package com.example.todo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Como usar o DRYTODO", fontWeight = FontWeight.Bold, color = PurpleDark) },
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
                HelpSection(
                    title = "Marcar Tarefas",
                    icon = Icons.Default.RadioButtonUnchecked,
                    description = "Pressione e arraste o botão circular na tarefa. Deslize para a DIREITA para aumentar o progresso até 100%. Deslize para a ESQUERDA para diminuir ou desmarcar."
                )
            }

            item {
                HelpSection(
                    title = "Agenda Inteligente",
                    icon = Icons.Default.AutoFixHigh,
                    description = "As tarefas não concluídas são automaticamente movidas para o próximo dia (ou próxima recorrência) se passarem 8 horas do prazo. A agenda mostra o histórico de tudo o que foi feito ou expirou."
                )
            }

            item {
                HelpSection(
                    title = "Modo Foco",
                    icon = Icons.Default.CenterFocusStrong,
                    description = "Toque no ícone de alvo em qualquer tarefa para entrar no Modo Foco. Use o temporizador (Pomodoro) para se concentrar. Quando o tempo acabar, o telemóvel irá tocar e vibrar persistentemente."
                )
            }

            item {
                HelpSection(
                    title = "Eliminar e Editar",
                    icon = Icons.Default.SwipeLeft,
                    description = "No ecrã de Tarefas, deslize qualquer item para a ESQUERDA para eliminá-lo permanentES. Toque no corpo da tarefa para editar detalhes ou mudar a prioridade."
                )
            }

            item {
                HelpSection(
                    title = "Funcionamento em Segundo Plano",
                    icon = Icons.Default.BatteryChargingFull,
                    description = "Para que os alertas e a reorganização automática funcionem, certifique-se de que a aplicação tem permissão para ignorar as otimizações de bateria."
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("DRYTODO v1.0", color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HelpSection(
    title: String,
    icon: ImageVector,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, color = PurpleDark, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}
