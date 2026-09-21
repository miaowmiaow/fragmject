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
import androidx.compose.runtime.remember
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
import com.example.fragmject.feature.picture.model.MediaBean

@Composable
fun PictureSelectorScreen(
    modifier: Modifier = Modifier,
    onFinish: (List<MediaBean>) -> Unit,
    onDismiss: () -> Unit,
    onPreview: (List<String>) -> Unit = {},
    viewModel: PictureViewModel,
) {
    val context = LocalContext.current
    val albumResult by viewModel.albumResult.collectAsStateWithLifecycle()
    val currAlbumResult by viewModel.currAlbumResult.collectAsStateWithLifecycle()
    val selectedUris by viewModel.selectedUris.collectAsStateWithLifecycle()
    val selectedUriSet by viewModel.selectedUriSet.collectAsStateWithLifecycle()
    val currAlbumName by viewModel.currAlbumName.collectAsStateWithLifecycle()
    val expanded by viewModel.albumMenuExpanded.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
    val queryAttempted by viewModel.queryAttempted.collectAsStateWithLifecycle()

    val selectPositionMap = remember(selectedUris) {
        selectedUris.withIndex().associate { (num, uri) -> uri to (num + 1) }
    }

    // 存储权限
    val storagePermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.setHasPermission(storagePermissions.all { results[it] == true })
        // 权限回调后重新查询（无论结果如何，都尝试查询）
        viewModel.queryAlbum()
    }

    // 初始权限检查：有权限直接查询，无权限则自动弹出系统授权对话框
    LaunchedEffect(Unit) {
        val granted = storagePermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.setHasPermission(granted)
        if (granted) {
            viewModel.queryAlbum()
        } else {
            permissionLauncher.launch(storagePermissions)
        }
    }

    // 拍照
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // 清除 pending 状态，让真实照片对系统相册与查询可见
            viewModel.finishTakePictureUri()
            // 重新查询相册，获取相机写入后的真实数据并按最新修改时间排序到头部
            viewModel.queryAlbum()
        } else {
            // 拍照失败或用户取消：删除预创建但未写入数据的记录，避免相册残留透明图
            viewModel.deleteTakePictureUri()
        }
    }

    // 启动相机：预创建 Uri 后拉起系统相机
    fun launchCamera() {
        val uri = viewModel.createTakePictureUri()
        if (uri != null) {
            takePictureLauncher.launch(uri)
        } else {
            Toast.makeText(context, "无法创建照片文件", Toast.LENGTH_SHORT).show()
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
        if (queryAttempted && albumResult.isEmpty() && !hasPermission) {
            // 权限未授权：显示授权提示
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .windowInsetsPadding(WindowInsets.systemBars),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
                Text(
                    "需要访问您的相册",
                    color = Color.White,
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
                    .background(Color.Black)
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                // 顶部栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }

                    // 相册选择（始终居中）
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        Row(
                            modifier = Modifier.clickable { viewModel.setAlbumMenuExpanded(true) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                currAlbumName.ifEmpty { "相册" },
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { viewModel.setAlbumMenuExpanded(false) },
                            containerColor = Color(0xFF2B2B2B),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            albumResult.forEach { album ->
                                DropdownMenuItem(
                                    colors = MenuDefaults.itemColors(
                                        textColor = Color.White
                                    ),
                                    text = {
                                        Text(
                                            "${album.name} (${album.size})",
                                            color = Color.White
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateCurrAlbum(album.name)
                                        viewModel.setAlbumMenuExpanded(false)
                                    }
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            val data = selectedUris.mapNotNull { uriStr ->
                                currAlbumResult.find { it.uri.toString() == uriStr }
                            }
                            onFinish(data)
                        },
                        enabled = selectedUris.isNotEmpty(),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(
                            if (selectedUris.isEmpty()) "确定" else "确定(${selectedUris.size})",
                            color = if (selectedUris.isNotEmpty()) Color(0xFF508CEE) else Color.Gray,
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
                                .background(Color(0xFF333333))
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
                                tint = Color.White,
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
                                    viewModel.toggleSelection(uriStr)
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
                                        .background(Color.White.copy(alpha = 0.3f))
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
                                        if (isSelected) Color(0xFF508CEE)
                                        else Color.Transparent
                                    )
                                    .then(
                                        if (isSelected) Modifier
                                        else Modifier.border(2.dp, Color.White, CircleShape)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Text(
                                        selectNum.toString(),
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // 标签（长图/动图）
                            if (media.longImage() || media.gifImage()) {
                                Text(
                                    text = if (media.longImage()) "长图" else "动图",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.5f),
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
                    .background(Color.Black.copy(alpha = 0.6f))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .align(Alignment.BottomCenter)
            ) {
                Text("预览", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}