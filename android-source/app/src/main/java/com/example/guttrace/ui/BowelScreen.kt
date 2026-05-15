package com.example.guttrace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.guttrace.viewmodel.BowelViewModel
import kotlinx.coroutines.delay

val BRISTOL_DESCRIPTIONS = mapOf(
    1 to "Caroços duros separados (difíceis de passar)",
    2 to "Formato de salsicha, mas irregular/grumoso",
    3 to "Formato de salsicha, com rachaduras",
    4 to "Formato de salsicha/cobra, liso e macio",
    5 to "Pedaços macios com bordas nítidas",
    6 to "Pedaços fofos, bordas irregulares, pastoso",
    7 to "Aquoso, nenhum pedaço sólido (líquido)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BowelScreen(navController: NavController, vm: BowelViewModel = viewModel()) {
    val context = LocalContext.current
    var selectedScale by remember { mutableStateOf<Int?>(null) }
    var notes by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    var eventTime by remember { mutableStateOf(java.time.LocalDateTime.now()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F0F1A), Color(0xFF1A1A2E))))
    ) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Text("Evacuação (Escala de Bristol)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.height(16.dp))
            
            DateTimeSelector(
                selectedDateTime = eventTime,
                onDateTimeSelected = { eventTime = it }
            )
            
            Spacer(Modifier.height(16.dp))

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                (1..7).forEach { scale ->
                    val isSelected = selectedScale == scale
                    val bgColor = if (isSelected) Color(0xFF7C83FD) else Color(0xFF1E1E35)
                    val textColor = if (isSelected) Color.White else Color(0xFF888899)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .clickable { selectedScale = scale }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color.White.copy(alpha=0.2f) else Color(0xFF2A2A4A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tipo $scale", color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(BRISTOL_DESCRIPTIONS[scale] ?: "", color = textColor, fontSize = 14.sp)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Anotações (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF7C83FD),
                        unfocusedBorderColor = Color(0xFF1E1E35)
                    )
                )
            }

            Spacer(Modifier.height(16.dp))

            if (!saved) {
                Button(
                    onClick = {
                        if (selectedScale != null) {
                            vm.saveBowelMovement(context, selectedScale!!, notes.trim(), eventTime)
                            saved = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = selectedScale != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8D6E63),
                        disabledContainerColor = Color(0xFF3E2723).copy(alpha=0.5f)
                    )
                ) {
                    Text("Salvar Evacuação", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            } else {
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
                        Text("Salvo!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                LaunchedEffect(Unit) {
                    delay(1000)
                    navController.popBackStack()
                }
            }
        }
    }
}
