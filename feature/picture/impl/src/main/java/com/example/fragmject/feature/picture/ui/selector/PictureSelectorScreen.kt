package com.example.fragmject.feature.picture.ui.selector

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.feature.picture.state.PictureFlowState

@Composable
fun PictureSelectorScreen(
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onPreview: (List<String>) -> Unit = {},
    flowState: PictureFlowState,
) {
    val albumResult by flowState.albumResult.collectAsStateWithLifecycle()
    val currAlbumResult by flowState.currAlbumResult.collectAsStateWithLifecycle()
    val selectedUris by flowState.selectedUris.collectAsStateWithLifecycle()
    val selectedUriSet by flowState.selectedUriSet.collectAsStateWithLifecycle()
    val currAlbumName by flowState.currAlbumName.collectAsStateWithLifecycle()
    val expanded by flowState.albumMenuExpanded.collectAsStateWithLifecycle()
    val hasPermission by flowState.hasPermission.collectAsStateWithLifecycle()
    val queryAttempted by flowState.queryAttempted.collectAsStateWithLifecycle()
    val queryError by flowState.queryError.collectAsStateWithLifecycle()

    val selectPositionMap = remember(selectedUris) {
        selectedUris.withIndex().associate { (num, uri) -> uri to (num + 1) }
    }

    val storagePermissions = remember { storagePermissionsForApi(Build.VERSION.SDK_INT) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        flowState.setHasPermission(storagePermissions.all { results[it] == true })
        // 权限回调后重新查询（无论结果如何，都尝试查询）
        flowState.queryAlbum()
    }

    SelectorPermissionEffect(
        flowState = flowState,
        storagePermissions = storagePermissions,
        permissionLauncher = permissionLauncher,
    )

    val onTakePicture = rememberTakePictureAction(flowState)
    val actions = SelectorActions(
        onBack = onDismiss,
        onConfirm = onConfirm,
        onExpandMenu = flowState::setAlbumMenuExpanded,
        onSelectAlbum = { albumName ->
            flowState.updateCurrAlbum(albumName)
            flowState.setAlbumMenuExpanded(false)
        },
        onTakePicture = onTakePicture,
        onToggleSelection = flowState::toggleSelection,
    )

    Box(modifier = modifier.fillMaxSize()) {
        when {
            queryAttempted && queryError -> SelectorErrorState(
                onRetry = { flowState.queryAlbum() }
            )

            queryAttempted && albumResult.isEmpty() && !hasPermission -> SelectorPermissionState(
                onDismiss = onDismiss,
                onRequestPermission = { permissionLauncher.launch(storagePermissions) },
            )

            else -> SelectorContent(
                topBarState = SelectorTopBarState(
                    currentAlbumName = currAlbumName,
                    albums = albumResult,
                    menuExpanded = expanded,
                    selectedCount = selectedUris.size,
                ),
                items = currAlbumResult,
                selectedUriSet = selectedUriSet,
                selectPositionMap = selectPositionMap,
                actions = actions,
            )
        }

        // 预览按钮
        if (selectedUris.isNotEmpty()) {
            TextButton(
                onClick = { onPreview(selectedUris) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.editorBackground.copy(alpha = 0.6f))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .align(Alignment.BottomCenter)
            ) {
                Text("预览", color = AppColors.editorOnBackground, fontSize = 14.sp)
            }
        }
    }
}
