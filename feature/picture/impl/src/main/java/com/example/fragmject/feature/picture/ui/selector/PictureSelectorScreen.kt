package com.example.fragmject.feature.picture.ui.selector

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.fragmject.core.network.utils.CacheUtils
import com.example.fragmject.feature.picture.model.MediaBean
import java.io.File

@Composable
fun PictureSelectorScreen(
    modifier: Modifier = Modifier,
    onFinish: (List<MediaBean>) -> Unit,
    onDismiss: () -> Unit,
    onPreview: (List<Int>) -> Unit = {},
    viewModel: PictureViewModel = viewModel(),
) {
    val context = LocalContext.current
    val albumResult by viewModel.albumResult.collectAsStateWithLifecycle()
    val currAlbumResult by viewModel.currAlbumResult.collectAsStateWithLifecycle()
    val selectPosition = remember { mutableStateListOf<Int>() }
    val selectPositionSet = remember { mutableStateMapOf<Int, Unit>() }
    var currAlbumName by remember { mutableStateOf("") }
    val selectPositionMap by remember {
        derivedStateOf {
            selectPosition.withIndex().associate { (num, pos) -> pos to (num + 1) }
        }
    }
    var expanded by remember { mutableStateOf(false) }
    var takePictureUri by remember { mutableStateOf<Uri?>(null) }

    // 存储权限
    val storagePermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    var hasPermission by remember {
        mutableStateOf(
            storagePermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    var queryAttempted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = storagePermissions.all { results[it] == true }
        // 权限回调后重新查询（无论结果如何，都尝试查询）
        viewModel.queryAlbum(context)
    }

    // 始终尝试查询，不依赖权限状态
    LaunchedEffect(Unit) {
        viewModel.queryAlbum(context)
        queryAttempted = true
    }

    LaunchedEffect(albumResult) {
        if (albumResult.isNotEmpty() && currAlbumName.isEmpty()) {
            currAlbumName = albumResult[0].name
        }
    }

    // 拍照
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = takePictureUri ?: return@rememberLauncherForActivityResult
            viewModel.updateMediaMap(MediaBean("拍照", uri))
        }
    }

    fun createTakePictureUri(): Uri {
        val pictureName = "${System.currentTimeMillis()}.png"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, pictureName)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: Uri.EMPTY
        } else {
            val cachePath = CacheUtils.getDirPath(context, Environment.DIRECTORY_PICTURES)
            val imageFile = File(cachePath, pictureName)
            val authority = "${context.packageName}.FileProvider"
            FileProvider.getUriForFile(context, authority, imageFile)
        }.also { takePictureUri = it }
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
                            modifier = Modifier.clickable { expanded = true },
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
                            onDismissRequest = { expanded = false },
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
                                        currAlbumName = album.name
                                        viewModel.updateCurrAlbum(album.name)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            val data = selectPosition.map { currAlbumResult[it] }
                            onFinish(data)
                        },
                        enabled = selectPosition.isNotEmpty(),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(
                            if (selectPosition.isEmpty()) "确定" else "确定(${selectPosition.size})",
                            color = if (selectPosition.isNotEmpty()) Color(0xFF508CEE) else Color.Gray,
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
                                    val uri = createTakePictureUri()
                                    takePictureLauncher.launch(uri)
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
                    itemsIndexed(currAlbumResult, key = { _, item -> item.uri }) { index, media ->
                        val realIndex = index // 真实索引（不含相机）
                        val isSelected = selectPositionSet.containsKey(realIndex)
                        val selectNum = selectPositionMap[realIndex] ?: 0

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clickable {
                                    if (isSelected) {
                                        selectPosition.remove(realIndex)
                                        selectPositionSet.remove(realIndex)
                                    } else if (selectPosition.size < 9) {
                                        selectPosition.add(realIndex)
                                        selectPositionSet[realIndex] = Unit
                                    }
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
        if (selectPosition.isNotEmpty()) {
            TextButton(
                onClick = { onPreview(selectPosition.toList()) },
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