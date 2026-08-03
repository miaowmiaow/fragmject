package com.example.fragmject.feature.wan.demo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavKey
import com.example.fragmject.feature.picture.PictureSelectorNavKey
import com.example.fragmject.feature.wan.*
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.launch

@Composable
fun BarcodeScanningScreen(onNavigate: (NavKey) -> Unit = {}) {
    val context = LocalContext.current
    val cameraPermissions = arrayOf(
        Manifest.permission.CAMERA,
    )
    val snackState = remember { SnackbarHostState() }
    val snackScope = rememberCoroutineScope()
    var hasPermission by remember {
        mutableStateOf(
            cameraPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val requestPermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { ps ->
            val isGranted = ps.entries.all {
                it.key !in cameraPermissions || it.value
            }
            if (isGranted) {
                hasPermission = true
            } else {
                snackScope.launch {
                    snackState.showSnackbar("相机权限被拒绝，请前往设置页面手动授权")
                }
            }
        }
    LaunchedEffect(context) {
        if (!hasPermission) {
            requestPermissions.launch(cameraPermissions)
        }
    }
    SnackbarHost(hostState = snackState, Modifier)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        var barcodeScanner by remember { mutableStateOf<BarcodeScanner?>(null) }
        if (hasPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        val cameraController = LifecycleCameraController(ctx)
                        val options = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build()
                        barcodeScanner = BarcodeScanning.getClient(options).also { scanner ->
                            cameraController.setImageAnalysisAnalyzer(
                                ContextCompat.getMainExecutor(ctx),
                                MlKitAnalyzer(
                                    listOf(scanner),
                                    COORDINATE_SYSTEM_ORIGINAL,
                                    ContextCompat.getMainExecutor(ctx)
                                ) { result: MlKitAnalyzer.Result? ->
                                    val barcodeResults = result?.getValue(scanner)
                                    if (barcodeResults.isNullOrEmpty() ||
                                        (barcodeResults.first() == null)
                                    ) {
                                        return@MlKitAnalyzer
                                    }

                                    val barcode = barcodeResults[0]
                                    val qrContent = if (barcode.valueType == Barcode.TYPE_URL) {
                                        barcode.url?.url ?: barcode.rawValue ?: ""
                                    } else {
                                        barcode.rawValue ?: ""
                                    }
                                    val boundingRect = barcode.boundingBox ?: return@MlKitAnalyzer
                                    val qrCodeDrawable =
                                        QrCodeDrawable(qrContent, boundingRect)
                                    previewView.overlay.clear()
                                    // add qr drawable first so scan line is on top
                                    previewView.overlay.add(qrCodeDrawable)
                                    scanner.close()
                                    snackScope.launch {
                                        snackState.showSnackbar(
                                            qrContent
                                        )
                                    }
                                    return@MlKitAnalyzer
                                }
                            )
                        }
                        cameraController.bindToLifecycle(lifecycleOwner)
                        previewView.controller = cameraController
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = {
                    barcodeScanner?.close()
                }
            )
        }
        AssistChip(
            onClick = { onNavigate(PictureSelectorNavKey) },
            label = {},
            leadingIcon = {
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    Modifier.size(AssistChipDefaults.IconSize)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = MaterialTheme.colorScheme.primaryContainer,
                leadingIconContentColor = MaterialTheme.colorScheme.primaryContainer
            ),
            border = AssistChipDefaults.assistChipBorder(
                true,
                borderColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }

}

class QrCodeDrawable(val qrContent: String, val boundingRect: Rect) : Drawable() {
    private val boundingRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.YELLOW
        strokeWidth = 5F
        alpha = 200
    }

    private val contentRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.YELLOW
        alpha = 255
    }

    private val contentTextPaint = Paint().apply {
        color = Color.DKGRAY
        alpha = 255
        textSize = 36F
    }

    private val contentPadding = 25
    private var textWidth = contentTextPaint.measureText(qrContent).toInt()

    override fun draw(canvas: Canvas) {
        canvas.drawRect(boundingRect, boundingRectPaint)
        canvas.drawRect(
            Rect(
                boundingRect.left,
                boundingRect.bottom + contentPadding / 2,
                boundingRect.left + textWidth + contentPadding * 2,
                boundingRect.bottom + contentTextPaint.textSize.toInt() + contentPadding
            ),
            contentRectPaint
        )
        canvas.drawText(
            qrContent,
            (boundingRect.left + contentPadding).toFloat(),
            (boundingRect.bottom + contentPadding * 2).toFloat(),
            contentTextPaint
        )
    }

    override fun setAlpha(alpha: Int) {
        boundingRectPaint.alpha = alpha
        contentRectPaint.alpha = alpha
        contentTextPaint.alpha = alpha
    }

    override fun setColorFilter(colorFiter: ColorFilter?) {
        boundingRectPaint.colorFilter = colorFilter
        contentRectPaint.colorFilter = colorFilter
        contentTextPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}