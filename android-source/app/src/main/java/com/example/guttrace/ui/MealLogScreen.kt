package com.example.guttrace.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.guttrace.viewmodel.MealViewModel
import java.io.File

val TAG_GROUPS = mapOf(
    "Alergênicos" to listOf("lactose", "glúten/trigo", "amendoim", "soja", "crustáceos"),
    "Gatilhos Comuns" to listOf("frango frito", "alho/cebola", "alto-fodmap", "histamina", "fermentado", "picante", "café", "álcool", "refrigerante"),
    "Tipo / Volume" to listOf("restaurante", "fast-food", "refeição grande", "refeição tardia", "proteína em pó", "lanche rápido")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MealLogScreen(navController: NavController, vm: MealViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gut_prefs", Context.MODE_PRIVATE) }
    
    val customTags = remember {
        mutableStateListOf<String>().apply {
            addAll(prefs.getStringSet("custom_tags", emptySet()) ?: emptySet())
        }
    }
    
    var searchText by remember { mutableStateOf("") }
    
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val selectedTags = remember { mutableStateListOf<String>() }
    var saved by remember { mutableStateOf(false) }
    var eventTime by remember { mutableStateOf(java.time.LocalDateTime.now()) }

    // Create a temp file for CameraX
    val photoFile = remember { File(context.cacheDir, "meal_${System.currentTimeMillis()}.jpg") }
    val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) photoUri = fileUri
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(fileUri)
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(fileUri)
        } else {
            permLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Auto-launch camera when screen opens
    LaunchedEffect(Unit) { launchCamera() }

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
                Text("Registrar Refeição", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            Spacer(Modifier.height(16.dp))

            // Scrollable Content
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                DateTimeSelector(
                    selectedDateTime = eventTime,
                    onDateTimeSelected = { eventTime = it }
                )
                Spacer(Modifier.height(16.dp))

                // Photo area
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Foto da refeição",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { launchCamera() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF7C83FD))
                        Spacer(Modifier.width(6.dp))
                        Text("Refazer foto", color = Color(0xFF7C83FD))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E1E35))
                            .clickable { launchCamera() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF7C83FD), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Toque para fotografar", color = Color(0xFF888899))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Search / Add Tag
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Buscar ou adicionar nova tag...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF7C83FD),
                        unfocusedBorderColor = Color(0xFF2A2A4A)
                    ),
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { searchText = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpar", tint = Color(0xFF888899))
                            }
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))

                val allTags = TAG_GROUPS.values.flatten() + customTags
                val isSearching = searchText.isNotBlank()

                if (isSearching) {
                    val filtered = allTags.filter { it.contains(searchText.lowercase().trim()) }.distinct()
                    
                    Text("Resultados da Busca", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        filtered.forEach { tag ->
                            TagChip(tag, selectedTags.contains(tag)) {
                                if (selectedTags.contains(tag)) selectedTags.remove(tag) else selectedTags.add(tag)
                            }
                        }
                    }
                    
                    if (filtered.isEmpty()) {
                        Button(
                            onClick = {
                                val newTag = searchText.lowercase().trim()
                                customTags.add(newTag)
                                val current = prefs.getStringSet("custom_tags", emptySet()) ?: emptySet()
                                prefs.edit().putStringSet("custom_tags", current + newTag).apply()
                                selectedTags.add(newTag)
                                searchText = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A4A)),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF7C83FD))
                            Spacer(Modifier.width(8.dp))
                            Text("Criar tag: '$searchText'", color = Color.White)
                        }
                    }
                } else {
                    // Show Logical Groups
                    TAG_GROUPS.forEach { (groupName, tags) ->
                        Text(groupName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            tags.forEach { tag ->
                                TagChip(tag, selectedTags.contains(tag)) {
                                    if (selectedTags.contains(tag)) selectedTags.remove(tag) else selectedTags.add(tag)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    if (customTags.isNotEmpty()) {
                        Text("Minhas Tags", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            customTags.forEach { tag ->
                                TagChip(tag, selectedTags.contains(tag)) {
                                    if (selectedTags.contains(tag)) selectedTags.remove(tag) else selectedTags.add(tag)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Save button fixed at bottom
            if (!saved) {
                Button(
                    onClick = {
                        vm.saveMeal(
                            context = context,
                            photoFile = if (photoUri != null) photoFile else null,
                            tags = selectedTags.toList(),
                            eventTime = eventTime
                        )
                        saved = true
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C83FD))
                ) {
                    Text("Salvar refeição", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        Text("Salvo! Sync pendente...", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1200)
                    navController.popBackStack()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagChip(tag: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(tag, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF7C83FD),
            selectedLabelColor = Color.White,
            containerColor = Color(0xFF1E1E35),
            labelColor = Color(0xFF888899)
        ),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
