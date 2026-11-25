package com.example.fragmject.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragmject.core.ui.R

@Composable
fun ArrowRightItem(
    title: String,
    description: String = "",
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
            .height(45.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 25.dp),
            fontSize = 13.sp,
        )
        Spacer(Modifier.width(5.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                // 不写死高度：10sp 描述行在字号放大后会超过 17dp，固定高度会被裁
                .heightIn(min = 17.dp)
                .padding(end = 25.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = description,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Image(
            painter = painterResource(id = R.mipmap.ic_right),
            contentDescription = "",
            modifier = Modifier.padding(start = 25.dp, end = 25.dp)
        )
    }
}
