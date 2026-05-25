package com.example.fragment.project.ui.demo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.activity.ComponentActivity
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
import com.example.fragment.project.components.StandardDialog
import com.example.miaow.picture.selector.PictureSelectorActivity
import com.example.miaow.picture.selector.bean.MediaBean
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch

@Composable
fun BarcodeScanningScreen() {
    val context = LocalContext.current
    val cameraPermissions = arrayOf(
        Manifest.permission.CAMERA,
    )
    var showDialog by remember { mutableStateOf(false) }
    val requestPermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { ps ->
            var isGranted = true
            ps.entries.forEach {
                if (it.key in cameraPermissions && !it.value)
                    isGranted = false
            }
            showDialog = !isGranted
        }
    val snackState = remember { SnackbarHostState() }
    val snackScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        showDialog = !cameraPermissions.all {
            ContextCompat.checkSelfPermission(
                context, it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    SnackbarHost(hostState = snackState, Modifier)
    StandardDialog(
        show = showDialog,
        title = "申请相机权限",
        text = "玩Android需要使用相机",
        onConfirm = { requestPermissions.launch(cameraPermissions) },
        onDismiss = { showDialog = false }
    )
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        val context = LocalContext.current
        val activity = context as ComponentActivity
        val lifecycleOwner = LocalLifecycleOwner.current
        var barcodeScanner by remember { mutableStateOf<BarcodeScanner?>(null) }
        val startForResult =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val intent = result.data
                    val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent?.getParcelableArrayListExtra("data", MediaBean::class.java)
                    } else {
                        intent?.getParcelableArrayListExtra("data")
                    }
                    data?.first()?.uri?.let { imageUri ->
                        barcodeScanner?.process(
                            InputImage.fromFilePath(context, imageUri)
                        )?.addOnSuccessListener { barcodes ->
                            for (barcode in barcodes) {
                                val qrContent = if (barcode.valueType == Barcode.TYPE_URL) {
                                    barcode.url?.url ?: barcode.rawValue ?: ""
                                } else {
                                    barcode.rawValue ?: ""
                                }
                                snackScope.launch {
                                    snackState.showSnackbar(
                                        qrContent
                                    )
                                }
                                barcodeScanner?.close()
                                return@addOnSuccessListener
                            }
                        }?.addOnFailureListener {
                            // Task failed with an exception
                            // ...
                        }
                    }
                }
            }
        AndroidView(
            factory = { context ->
                PreviewView(context).also { previewView ->
                    val cameraController = LifecycleCameraController(context)
                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                    barcodeScanner = BarcodeScanning.getClient(options).also { barcodeScanner ->
                        cameraController.setImageAnalysisAnalyzer(
                            ContextCompat.getMainExecutor(context),
                            MlKitAnalyzer(
                                listOf(barcodeScanner),
                                COORDINATE_SYSTEM_ORIGINAL,
                                ContextCompat.getMainExecutor(context)
                            ) { result: MlKitAnalyzer.Result? ->
                                val barcodeResults = result?.getValue(barcodeScanner)
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
                                barcodeScanner.close()
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
        AssistChip(
            onClick = {
                startForResult.launch(Intent(activity, PictureSelectorActivity::class.java))
            },
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