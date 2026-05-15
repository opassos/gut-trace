package com.example.guttrace.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.guttrace.viewmodel.MedicationViewModel

val PREDEFINED_MEDS = listOf(
    Pair("Famox 40mg", "Famotidine 40mg"),
    Pair("Vonau Flash 4mg", "Ondansetron 4mg"),
    Pair("Trimeb 200mg", "Trimebutine 200mg"),
    Pair("Enzima DAO", "DAO Enzyme")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen(navController: NavController, vm: MedicationViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gut_prefs", Context.MODE_PRIVATE) }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var newAlias by remember { mutableStateOf("") }
    var newGeneric by remember { mutableStateOf("") }
    var eventTime by remember { mutableStateOf(java.time.LocalDateTime.now()) }

    val customMedsSet = prefs.getStringSet("custom_meds", emptySet()) ?: emptySet()
    val dynamicMeds = remember {
        val list = mutableStateListOf<Pair<String, String>>()
        list.addAll(PREDEFINED_MEDS)
        customMedsSet.forEach {
            val parts = it.split("|", limit = 2)
            if (parts.size == 2) list.add(Pair(parts[0], parts[1]))
        }
        list
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F0F1A), Color(0xFF1A1A2E))))
    ) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Text("Registrar Remédio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.height(16.dp))
            
            DateTimeSelector(
                selectedDateTime = eventTime,
                onDateTimeSelected = { eventTime = it }
            )
            
            Spacer(Modifier.height(16.dp))

            LazyColumn(Modifier.weight(1f)) {
                items(dynamicMeds) { med ->
                    Button(
                        onClick = {
                            vm.saveMedication(context, med.second, med.first, eventTime)
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth().height(70.dp).padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E35))
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Medication, contentDescription = null, tint = Color(0xFF4DB6AC), modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(med.first, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(med.second, color = Color(0xFF888899), fontSize = 12.sp)
                            }
                        }
                    }
                }
                
                item {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth().height(70.dp).padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A4A))
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF7C83FD))
                            Spacer(Modifier.width(8.dp))
                            Text("Adicionar Novo", color = Color(0xFF7C83FD), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Novo Remédio") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newAlias,
                            onValueChange = { newAlias = it },
                            label = { Text("Nome Comercial") },
                            placeholder = { Text("ex: Advil") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newGeneric,
                            onValueChange = { newGeneric = it },
                            label = { Text("Nome Genérico") },
                            placeholder = { Text("ex: Ibuprofeno 400mg") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newAlias.isNotBlank() && newGeneric.isNotBlank()) {
                            val newEntry = "${newAlias.trim()}|${newGeneric.trim()}"
                            val currentCustom = prefs.getStringSet("custom_meds", emptySet()) ?: emptySet()
                            prefs.edit().putStringSet("custom_meds", currentCustom + newEntry).apply()
                            dynamicMeds.add(Pair(newAlias.trim(), newGeneric.trim()))
                        }
                        newAlias = ""
                        newGeneric = ""
                        showAddDialog = false
                    }) {
                        Text("Salvar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}
