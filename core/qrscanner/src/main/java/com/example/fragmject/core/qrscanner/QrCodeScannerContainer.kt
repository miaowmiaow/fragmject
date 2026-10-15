package com.example.fragmject.core.qrscanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

/**
 * 二维码扫描容器：CameraX + ML Kit，只识别 [Barcode.FORMAT_QR_CODE]。
 *
 * 归属 core 引擎层（与 `core:webview` / `core:image-editor` 同类）：feature 之间不能互相
 * 依赖，因此这类「平台相关 UI 能力」必须有共享落点，不能留在某个 feature 内。
 *
 * 职责边界：
 * - **负责**：CAMERA 权限申请、生命周期绑定、识别到后关闭分析器并只回调一次 [onResult]；
 * - **不负责**结果展示（提示、弹窗、后续业务），这些由调用方决定，
 *   避免引擎层夹带业务 UI。
 *
 * 调用方可用 [overlay] 在预览之上绘制取景框、扫描线、状态指示器等自有装饰；
 * 若自带装饰已足够表达「识别到了」，可用 [drawBoundingBox] = false 关掉引擎自带的黄框。
 * 两个参数都有默认值，不影响既有调用方（如 demo）。
 */
@Composable
fun QrCodeScannerContainer(
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPermissionDenied: () -> Unit = {},
    drawBoundingBox: Boolean = true,
    overlay: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            hasPermission = true
        } else {
            onPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) requestPermission.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) return

    val lifecycleOwner = LocalLifecycleOwner.current
    var scanner by remember { mutableStateOf<BarcodeScanner?>(null) }

    Box(modifier = modifier) {
    AndroidView(
        modifier = Modifier.matchParentSize(),
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                val cameraController = LifecycleCameraController(ctx)
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                scanner = BarcodeScanning.getClient(options).also { barcodeScanner ->
                    cameraController.setImageAnalysisAnalyzer(
                        ContextCompat.getMainExecutor(ctx),
                        MlKitAnalyzer(
                            listOf(barcodeScanner),
                            COORDINATE_SYSTEM_ORIGINAL,
                            ContextCompat.getMainExecutor(ctx),
                        ) { result: MlKitAnalyzer.Result? ->
                            val barcode = result?.getValue(barcodeScanner)?.firstOrNull()
                                ?: return@MlKitAnalyzer
                            val content = if (barcode.valueType == Barcode.TYPE_URL) {
                                barcode.url?.url ?: barcode.rawValue
                            } else {
                                barcode.rawValue
                            }
                            val rect = barcode.boundingBox
                            if (drawBoundingBox && rect != null) {
                                previewView.overlay.clear()
                                previewView.overlay.add(QrCodeDrawable(content.orEmpty(), rect))
                            }
                            // 关闭分析器，确保只回调一次
                            barcodeScanner.close()
                            content?.let(onResult)
                            return@MlKitAnalyzer
                        },
                    )
                }
                cameraController.bindToLifecycle(lifecycleOwner)
                previewView.controller = cameraController
            }
        },
        onRelease = { scanner?.close() },
    )
        overlay()
    }
}
