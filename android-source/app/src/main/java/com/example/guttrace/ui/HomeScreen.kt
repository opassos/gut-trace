package com.example.guttrace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

            // Secondary: Symptoms
            BigActionButton(
                icon = Icons.Default.MonitorHeart,
                label = "Como estou agora?",
                sublabel = "registrar sintomas",
                gradientColors = listOf(Color(0xFFFF6B6B), Color(0xFFEE0979)),
                onClick = { navController.navigate("symptom") }
            )

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
                recentEvents.take(5).forEach { event ->
                    EventRowCard(event.type, event.localDatetime, event.syncStatus)
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
            .background(Brush.horizontalGradient(gradientColors))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(sublabel, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun EventRowCard(type: String, timestamp: String, syncStatus: String) {
    val emoji = if (type == "meal") "🍽️" else "🫀"
    val typeLabel = if (type == "meal") "Refeição" else "Sintomas"
    val time = timestamp.takeLast(14).take(5) // HH:mm
    val synced = syncStatus == "synced"

    Surface(
        modifier = Modifier.fillMaxWidth(),
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
}
