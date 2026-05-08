package com.example.guttrace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.guttrace.data.EventEntity
import com.example.guttrace.viewmodel.HomeViewModel

import androidx.compose.ui.platform.LocalContext

@Composable
fun HomeScreen(navController: NavController, vm: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val recentEvents by vm.recentEvents.collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F0F1A), Color(0xFF1A1A2E))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.align(Alignment.Center)) {
                    Text(
                        text = "GutTrace",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C83FD)
                    )
                    Text(
                        text = "diário intestinal",
                        fontSize = 14.sp,
                        color = Color(0xFF888899),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                IconButton(
                    onClick = { navController.navigate("settings") },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Configurações", tint = Color(0xFF888899))
                }
            }

            Spacer(Modifier.height(52.dp))

            // MAIN ACTION: Meal photo
            BigActionButton(
                icon = Icons.Default.CameraAlt,
                label = "Registrar Refeição",
                sublabel = "foto + tags rápidas",
                gradientColors = listOf(Color(0xFF7C83FD), Color(0xFF4E54C8)),
                onClick = { navController.navigate("meal_log") }
            )

            Spacer(Modifier.height(20.dp))

            // Secondary Actions
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    BigActionButton(
                        icon = Icons.Default.MonitorHeart,
                        label = "Sintomas",
                        sublabel = "agora",
                        gradientColors = listOf(Color(0xFFFF6B6B), Color(0xFFEE0979)),
                        onClick = { navController.navigate("symptom") }
                    )
                }
                Box(Modifier.weight(1f)) {
                    BigActionButton(
                        icon = Icons.Default.Medication,
                        label = "Remédio",
                        sublabel = "intake",
                        gradientColors = listOf(Color(0xFF4DB6AC), Color(0xFF00897B)),
                        onClick = { navController.navigate("medication") }
                    )
                }
                Box(Modifier.weight(1f)) {
                    BigActionButton(
                        icon = Icons.Default.WaterDrop,
                        label = "Banheiro",
                        sublabel = "bristol",
                        gradientColors = listOf(Color(0xFF8D6E63), Color(0xFF5D4037)),
                        onClick = { navController.navigate("bowel") }
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Recent events
            if (recentEvents.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Recentes",
                        color = Color(0xFF888899),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Forçar Sync",
                        color = Color(0xFF7C83FD),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { vm.forceSync(context) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                recentEvents.take(100).forEach { event ->
                    EventRowCard(event) {
                        vm.deleteEvent(event, context)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun BigActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sublabel: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(gradientColors))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(sublabel, color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
        }
    }
}

@Composable
fun EventRowCard(event: EventEntity, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val emoji = when (event.type) {
        "meal" -> "🍽️"
        "symptom" -> "🫀"
        "medication" -> "💊"
        "bowel" -> "💩"
        else -> "❓"
    }
    val typeLabel = when (event.type) {
        "meal" -> "Refeição"
        "symptom" -> "Sintomas"
        "medication" -> "Remédio"
        "bowel" -> "Evacuação"
        else -> "Evento"
    }
    val time = event.localDatetime.takeLast(14).take(5) // HH:mm
    val synced = event.syncStatus == "synced"

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { showDeleteDialog = true },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1E35)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(typeLabel, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(time, color = Color(0xFF888899), fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (synced) Color(0xFF4CAF50) else Color(0xFFFF9800))
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir Evento") },
            text = { Text("Tem certeza que deseja apagar este $typeLabel?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) { Text("Excluir", color = Color(0xFFFF6B6B)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
