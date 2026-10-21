package com.example.fragmject.feature.picture.ui.selector

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.fragmject.feature.picture.state.PictureFlowState
import kotlinx.coroutines.launch

/**
 * 组装「点击拍照」这一动作：预创建 Uri → 申请相机权限 → 拉起系统相机 → 回写查询。
 *
 * 返回的 lambda 直接用于相机入口的点击回调；内部三个 launcher 都是 remember 过的，
 * 重复调用不会丢失进行中的拍照结果。
 */
@Composable
internal fun rememberTakePictureAction(flowState: PictureFlowState): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        scope.launch {
            if (success) {
                // 清除 pending 状态，让真实照片对系统相册与查询可见
                flowState.finishTakePictureUri()
                // 重新查询相册，获取相机写入后的真实数据并按最新修改时间排序到头部
                flowState.queryAlbum()
            } else {
                // 拍照失败或用户取消：删除预创建但未写入数据的记录，避免相册残留透明图
                flowState.deleteTakePictureUri()
            }
        }
    }

    // 启动相机：预创建 Uri 后拉起系统相机
    fun launchCamera() {
        scope.launch {
            val uri = flowState.createTakePictureUri()
            if (uri != null) {
                takePictureLauncher.launch(uri)
            } else {
                Toast.makeText(context, "无法创建照片文件", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 相机权限申请：授权成功后自动继续拍照
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    return {
        val cameraGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}
