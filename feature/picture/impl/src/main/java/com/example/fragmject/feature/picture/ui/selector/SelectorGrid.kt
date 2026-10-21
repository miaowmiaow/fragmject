package com.example.fragmject.feature.picture.ui.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.feature.picture.model.MediaItem

/**
 * 四列相册网格：首格是相机入口，其余是媒体缩略图与选中序号。
 *
 * 相机权限的检查留在调用方，本组件只负责「点击拍照」这一个动作，便于单独预览与测试。
 */
@Composable
internal fun SelectorGrid(
    items: List<MediaItem>,
    selectedUriSet: Set<String>,
    selectPositionMap: Map<String, Int>,
    onTakePicture: () -> Unit,
    onToggleSelection: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            CameraEntry(onClick = onTakePicture)
        }

        itemsIndexed(items, key = { _, item -> item.uri }) { _, media ->
            val uriStr = media.uri.toString()
            SelectorMediaCell(
                media = media,
                isSelected = uriStr in selectedUriSet,
                selectNum = selectPositionMap[uriStr] ?: 0,
                onClick = { onToggleSelection(uriStr) },
            )
        }
    }
}

/** 网格首格：拉起相机。 */
@Composable
private fun CameraEntry(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(AppColors.mediaPlaceholder)
            .clickable(onClick = onClick),
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

/** 单个媒体格：缩略图 + 选中遮罩 + 序号 + 长图/动图标签。 */
@Composable
private fun SelectorMediaCell(
    media: MediaItem,
    isSelected: Boolean,
    selectNum: Int,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = media.uri,
            contentDescription = media.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.editorOnBackground.copy(alpha = 0.3f))
            )
        }

        SelectBadge(
            modifier = Modifier.align(Alignment.TopEnd),
            isSelected = isSelected,
            selectNum = selectNum,
        )

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

/** 右上角选择框：选中显示蓝底序号，未选中显示描边圆圈。 */
@Composable
private fun SelectBadge(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    selectNum: Int,
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .size(22.dp)
            .clip(CircleShape)
            .background(if (isSelected) AppColors.blue else Color.Transparent)
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
}
