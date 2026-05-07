package com.guttrace.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
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
import com.guttrace.viewmodel.MealViewModel
import java.io.File

val MEAL_TAGS = listOf(
    "lactose", "glúten/trigo", "frango frito", "feijão/lentilha",
    "alho/cebola", "alto-fodmap", "histamina", "fermentado",
    "peixe cru", "picante", "restaurante", "fast-food",
    "proteína em pó", "refeição grande", "refeição tardia", "café"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealLogScreen(navController: NavController, vm: MealViewModel = viewModel()) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val selectedTags = remember { mutableStateListOf<String>() }
    var saved by remember { mutableStateOf(false) }

    // Create a temp file for CameraX
    val photoFile = remember {
        File(context.cacheDir, "meal_${System.currentTimeMillis()}.jpg")
    }
    val fileUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) photoUri = fileUri
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(fileUri)
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            cameraLauncher.launch(fileUri)
        } else {
            permLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Auto-launch camera when screen opens (zero friction!)
    LaunchedEffect(Unit) {
        launchCamera()
    }

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

            // Photo area
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Foto da refeição",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
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
                // Placeholder — camera launched automatically
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
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

            Spacer(Modifier.height(20.dp))

            // Tags (optional, up to 3 taps)
            Text("Tags opcionais (toque até 3)", color = Color(0xFF888899), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))

            // Tags grid
            val rows = MEAL_TAGS.chunked(3)
            rows.forEach { rowTags ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowTags.forEach { tag ->
                        val selected = tag in selectedTags
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) selectedTags.remove(tag)
                                else if (selectedTags.size < 3) selectedTags.add(tag)
                            },
                            label = { Text(tag, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF7C83FD),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E1E35),
                                labelColor = Color(0xFF888899)
                            )
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.weight(1f))

            // Save button
            if (!saved) {
                Button(
                    onClick = {
                        vm.saveMeal(
                            context = context,
                            photoFile = if (photoUri != null) photoFile else null,
                            tags = selectedTags.toList()
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
                // Success state
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
