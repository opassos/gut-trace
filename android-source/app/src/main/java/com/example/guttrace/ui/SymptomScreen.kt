package com.example.guttrace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.guttrace.viewmodel.SymptomViewModel

@Composable
fun SymptomScreen(navController: NavController, vm: SymptomViewModel = viewModel()) {
    val context = LocalContext.current
    var globalScore by remember { mutableStateOf(-1) }
    var saved by remember { mutableStateOf(false) }

    val scoreOptions = listOf(
        0 to ("👍" to "Ótimo"),
        1 to ("😐" to "Leve"),
        2 to ("😕" to "Incômodo"),
        3 to ("😣" to "Ruim"),
        4 to ("😫" to "Muito ruim"),
        5 to ("🤢" to "Péssimo"),
    )

    // Detailed symptom scores
    var bloating by remember { mutableStateOf(0) }
    var nausea by remember { mutableStateOf(0) }
    var belching by remember { mutableStateOf(0) }
    var heartburn by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F0F1A), Color(0xFF1A1A2E))))
    ) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            // Top bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Text("Como você está?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            Spacer(Modifier.height(24.dp))

            // Global Score — big tappable buttons
            Text("Sensação geral", color = Color(0xFF888899), fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                scoreOptions.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (score, pair) ->
                            val (emoji, label) = pair
                            val selected = globalScore == score
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) scoreColor(score).copy(alpha = 0.85f)
                                        else Color(0xFF1E1E35)
                                    )
                                    .clickable { globalScore = score },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(emoji, fontSize = 24.sp)
                                    Text(
                                        label,
                                        color = if (selected) Color.White else Color(0xFF888899),
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Show detailed sliders only if at least "leve" was chosen
            if (globalScore >= 1) {
                Spacer(Modifier.height(28.dp))
                Text("Detalhar (opcional)", color = Color(0xFF888899), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))

                SymptomSlider("🫃 Estufamento", bloating) { bloating = it }
                SymptomSlider("🤢 Náusea", nausea) { nausea = it }
                SymptomSlider("💨 Arrotos", belching) { belching = it }
                SymptomSlider("🔥 Queimação", heartburn) { heartburn = it }
            }

            Spacer(Modifier.weight(1f))

            // Save
            if (globalScore >= 0 && !saved) {
                Button(
                    onClick = {
                        vm.saveSymptom(
                            context = context,
                            globalScore = globalScore,
                            bloating = bloating,
                            nausea = nausea,
                            belching = belching,
                            heartburn = heartburn
                        )
                        saved = true
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE0979))
                ) {
                    Text("Registrar sintomas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else if (saved) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Registrado!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1200)
                    navController.popBackStack()
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E35)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Selecione como você está ↑", color = Color(0xFF888899))
                }
            }
        }
    }
}

@Composable
fun SymptomSlider(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 14.sp)
            Text("$value/5", color = Color(0xFF7C83FD), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..5f,
            steps = 4,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF7C83FD),
                activeTrackColor = Color(0xFF7C83FD),
                inactiveTrackColor = Color(0xFF2A2A45)
            )
        )
    }
}

fun scoreColor(score: Int): Color = when (score) {
    0 -> Color(0xFF4CAF50)
    1 -> Color(0xFF8BC34A)
    2 -> Color(0xFFFFEB3B)
    3 -> Color(0xFFFF9800)
    4 -> Color(0xFFF44336)
    5 -> Color(0xFF8B0000)
    else -> Color(0xFF888899)
}
