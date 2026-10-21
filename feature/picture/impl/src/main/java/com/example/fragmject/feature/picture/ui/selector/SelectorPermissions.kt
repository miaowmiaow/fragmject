package com.example.fragmject.feature.picture.ui.selector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.fragmject.feature.picture.state.PictureFlowState

/**
 * Android 14 新增的「仅选中的照片」权限。
 *
 * 不直接引用 `Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED`：该常量要求 API 34，
 * 而权限名本身只是字符串，在低版本上把它放进请求数组没有任何副作用。
 * 引用常量会让 lint 把整个 `storagePermissionsForApi`（及其所有调用方）判为 API 34+，
 * 与「按运行时版本挑选权限」的实际语义不符。
 */
private const val READ_MEDIA_VISUAL_USER_SELECTED = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"

/** 按系统版本挑选需要申请的读取权限。 */
internal fun storagePermissionsForApi(api: Int): Array<String> = when {
    // Android 14+：请求部分照片访问，用户可选择「仅选中的照片」
    api >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        READ_MEDIA_VISUAL_USER_SELECTED,
    )

    api >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)

    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

/**
 * 首帧的权限检查 + 授权结果回写。
 *
 * flowReset 用 rememberSaveable 区分「流程开始」与「流程内返回」：点预览再返回时
 * 恢复 true 跳过归零；流程结束再进入时丢弃恢复 false，才归零一次跨流程残留状态。
 */
@Composable
internal fun SelectorPermissionEffect(
    flowState: PictureFlowState,
    storagePermissions: Array<String>,
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
) {
    val context = LocalContext.current
    var flowReset by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!flowReset) {
            flowState.reset()
            flowReset = true
        }
        val granted = storagePermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        flowState.setHasPermission(granted)
        if (granted) {
            flowState.queryAlbum()
        } else {
            permissionLauncher.launch(storagePermissions)
        }
    }
}
