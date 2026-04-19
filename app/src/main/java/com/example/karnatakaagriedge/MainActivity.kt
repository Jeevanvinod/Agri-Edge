package com.example.karnatakaagriedge

import GeminiService
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class MainActivity : ComponentActivity() {
    private val geminiService = GeminiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { granted -> hasCameraPermission = granted }
                )

                LaunchedEffect(Unit) {
                    if (!hasCameraPermission) {
                        launcher.launch(Manifest.permission.CAMERA)
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (hasCameraPermission) {
                        AgriEdgeApp(geminiService)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Please grant camera permission to use the app.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarketWatchBar(marketViewModel: MarketViewModel = viewModel()) {
    // Replace with your actual 40-character API key from data.gov.in
    val apiKey = "579b464db66ec23bdd0000016f22cb7963b349915afbc8cf66e052b1"

    LaunchedEffect(Unit) {
        marketViewModel.fetchKarnatakaPrices(apiKey)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "LIVE MARKET RATES (KARNATAKA)",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            if (marketViewModel.isLoading.value) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = Color(0xFF43A047)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    // Filter specifically for your farm interests: Arecanut and Coconut
                    val filteredList = marketViewModel.marketData.value.filter {
                        it.commodity.contains("Arecanut", ignoreCase = true) ||
                                it.commodity.contains("Coconut", ignoreCase = true)
                    }.ifEmpty { marketViewModel.marketData.value }

                    items(filteredList) { record ->
                        Column {
                            Text(
                                text = record.commodity,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${record.market}: ₹${record.modal_price}",
                                fontSize = 11.sp,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgriEdgeApp(service: GeminiService) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }
    var resultText by remember { mutableStateOf("Ready to scan. Point at a crop and tap analyze.") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Karnataka Agri-Edge", color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF2E7D32)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF1F8E9))
        ) {
            // NEW: Live Market Rates Ticker
            MarketWatchBar()

            // 1. Live Camera Feed
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                AndroidView(
                    factory = { ctx -> PreviewView(ctx).apply { this.controller = controller } },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. AI Advice Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "DIAGNOSIS & CURE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 22.sp
                    )
                }
            }

            // 3. Scan Button
            Button(
                onClick = {
                    resultText = "AI is thinking... (Please wait)"
                    captureAndAnalyze(context, controller, service, scope) { response ->
                        resultText = response
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("SCAN CROP HEALTH", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun captureAndAnalyze(
    context: Context,
    controller: LifecycleCameraController,
    service: GeminiService,
    scope: CoroutineScope,
    onResult: (String) -> Unit
) {
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer: ByteBuffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                image.close()

                if (bitmap != null) {
                    scope.launch {
                        val response = service.analyzeCropImage(bitmap)
                        onResult(response)
                    }
                } else {
                    onResult("Failed to process image.")
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onResult("Camera Error: ${exception.message}")
            }
        }
    )
}