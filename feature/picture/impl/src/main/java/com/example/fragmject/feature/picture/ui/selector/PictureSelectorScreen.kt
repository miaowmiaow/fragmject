package com.example.fragmject.feature.picture.ui.selector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.feature.picture.state.PictureFlowState
import kotlinx.coroutines.launch

@Composable
fun PictureSelectorScreen(
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onPreview: (List<String>) -> Unit = {},
    flowState: PictureFlowState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

    // 存储权限
    val storagePermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+：请求部分照片访问，用户可选择「仅选中的照片」
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        flowState.setHasPermission(storagePermissions.all { results[it] == true })
        // 权限回调后重新查询（无论结果如何，都尝试查询）
        flowState.queryAlbum()
    }

    // 初始权限检查：有权限直接查询，无权限则自动弹出系统授权对话框
    // flowReset 用 rememberSaveable 区分「流程开始」与「流程内返回」：点预览再返回时
    // 恢复 true 跳过归零；流程结束再进入时丢弃恢复 false，才归零一次跨流程残留状态。
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

    // 拍照
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

    Box(modifier = modifier.fillMaxSize()) {
        if (queryAttempted && queryError) {
            // 查询失败：显示错误占位与重试
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.editorBackground)
                    .windowInsetsPadding(WindowInsets.systemBars),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "相册加载失败",
                    color = AppColors.editorOnBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("请检查权限后重试", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { flowState.queryAlbum() }) {
                    Text("重试")
                }
            }
        } else if (queryAttempted && albumResult.isEmpty() && !hasPermission) {
            // 权限未授权：显示授权提示
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.editorBackground)
                    .windowInsetsPadding(WindowInsets.systemBars),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = AppColors.editorOnBackground
                    )
                }
                Text(
                    "需要访问您的相册",
                    color = AppColors.editorOnBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "请授予存储权限以浏览照片",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { permissionLauncher.launch(storagePermissions) }) {
                    Text("授予权限")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.editorBackground)
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                // 顶部栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = AppColors.editorOnBackground
                        )
                    }

                    // 相册选择（始终居中）
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        Row(
                            modifier = Modifier.clickable { flowState.setAlbumMenuExpanded(true) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                currAlbumName.ifEmpty { "相册" },
                                color = AppColors.editorOnBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = AppColors.editorOnBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { flowState.setAlbumMenuExpanded(false) },
                            containerColor = AppColors.menuContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            albumResult.forEach { album ->
                                DropdownMenuItem(
                                    colors = MenuDefaults.itemColors(
                                        textColor = AppColors.editorOnBackground
                                    ),
                                    text = {
                                        Text(
                                            "${album.name} (${album.size})",
                                            color = AppColors.editorOnBackground
                                        )
                                    },
                                    onClick = {
                                        flowState.updateCurrAlbum(album.name)
                                        flowState.setAlbumMenuExpanded(false)
                                    }
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = { onConfirm() },
                        enabled = selectedUris.isNotEmpty(),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(
                            if (selectedUris.isEmpty()) "确定" else "确定(${selectedUris.size})",
                            color = if (selectedUris.isNotEmpty()) AppColors.blue else Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }

                // 图片网格
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 相机入口
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(AppColors.mediaPlaceholder)
                                .clickable {
                                    if (
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA,
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        launchCamera()
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.CameraAlt,
                                contentDescription = "拍照",
                                tint = AppColors.editorOnBackground,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // 图片列表
                    itemsIndexed(currAlbumResult, key = { _, item -> item.uri }) { _, media ->
                        val uriStr = media.uri.toString()
                        val isSelected = uriStr in selectedUriSet
                        val selectNum = selectPositionMap[uriStr] ?: 0

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clickable {
                                    flowState.toggleSelection(uriStr)
                                }
                        ) {
                            AsyncImage(
                                model = media.uri,
                                contentDescription = media.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // 选中遮罩
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(AppColors.editorOnBackground.copy(alpha = 0.3f))
                                )
                            }

                            // 选择框
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) AppColors.blue
                                        else Color.Transparent
                                    )
                                    .then(
                                        if (isSelected) Modifier
                                        else Modifier.border(2.dp, AppColors.editorOnBackground, CircleShape)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Text(
                                        selectNum.toString(),
                                        color = AppColors.editorOnBackground,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // 标签（长图/动图）
                            if (media.longImage() || media.gifImage()) {
                                Text(
                                    text = if (media.longImage()) "长图" else "动图",
                                    color = AppColors.editorOnBackground,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                        .background(
                                            AppColors.editorBackground.copy(alpha = 0.5f),
                                            RoundedCornerShape(2.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
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