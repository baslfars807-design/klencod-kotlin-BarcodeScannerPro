package com.example.barcodescanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.barcodescanner.scanner.BarcodeAnalyzer
import com.example.barcodescanner.scanner.formatName
import com.example.barcodescanner.ui.theme.BarcodeScannerTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var hasPermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            BarcodeScannerTheme {
                ScannerScreen(
                    hasPermission = hasPermission,
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    cameraExecutor = cameraExecutor
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    cameraExecutor: ExecutorService
) {
    val context = LocalContext.current
    var scannedText by remember { mutableStateOf<String?>(null) }
    var scannedFormat by remember { mutableStateOf<Int?>(null) }
    var flashEnabled by remember { mutableStateOf(false) }
    var camera: Camera? by remember { mutableStateOf(null) }

    fun vibrate() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") vibrator.vibrate(80)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Barcode Scanner", fontWeight = FontWeight.Medium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    IconButton(onClick = {
                        flashEnabled = !flashEnabled
                        camera?.cameraControl?.enableTorch(flashEnabled)
                    }) {
                        Icon(if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff, contentDescription = "Flash")
                    }
                }
            )
        },
        floatingActionButton = {
            if (scannedText != null) {
                ExtendedFloatingActionButton(
                    onClick = { scannedText = null },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    text = { Text("مسح مرة أخرى") }
                )
            }
        }
    ) { padding ->

        Box(Modifier.fillMaxSize().padding(padding)) {

            if (hasPermission) {
                CameraPreview(
                    onBarcode = { text, format ->
                        if (scannedText == null) {
                            vibrate()
                            scannedText = text
                            scannedFormat = format
                        }
                    },
                    cameraExecutor = cameraExecutor,
                    onCameraReady = { cam -> camera = cam }
                )

                // Google-style overlay
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.Transparent)
                    ) {
                        // Corners like Google Lens
                        CornerOverlay()
                    }
                    Text(
                        "ضع الباركود داخل الإطار",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 120.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

            } else {
                // Permission UI - Google style
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.VideocamOff, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("الكاميرا مطلوبة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("امنح الإذن عشان نقدر نقرأ الباركود بسرعة وبدون نت", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onRequestPermission, shape = RoundedCornerShape(20.dp)) {
                        Text("السماح بالكاميرا")
                    }
                }
            }

            // Bottom Sheet Result - Google style
            AnimatedVisibility(
                visible = scannedText != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                scannedText?.let { text ->
                    Card(
                        Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    scannedFormat?.let { formatName(it) } ?: "تم المسح",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("barcode", text))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("نسخ")
                                }
                                FilledButton(
                                    onClick = {
                                        val intent = try {
                                            if (text.startsWith("http")) Intent(Intent.ACTION_VIEW, Uri.parse(text))
                                            else Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                                        } catch (e: Exception) { Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) } }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.OpenInNew, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (text.startsWith("http")) "فتح" else "مشاركة")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CornerOverlay() {
    Box(Modifier.fillMaxSize()) {
        val cornerSize = 32.dp
        val stroke = 4.dp
        // TopStart
        Box(Modifier.align(Alignment.TopStart).size(cornerSize).background(Color.White, RoundedCornerShape(topStart = 28.dp)))
        // Simplified - full border with Google style
        Box(
            Modifier.fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Transparent)
        )
    }
}

@Composable
fun CameraPreview(
    onBarcode: (String, Int) -> Unit,
    cameraExecutor: ExecutorService,
    onCameraReady: (Camera) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also {
                        it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { txt, fmt -> onBarcode(txt, fmt) })
                    }
                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    val cam = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analyzer)
                    onCameraReady(cam)
                } catch (e: Exception) { e.printStackTrace() }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}