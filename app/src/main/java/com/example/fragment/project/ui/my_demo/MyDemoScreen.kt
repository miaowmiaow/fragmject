package com.example.fragment.project.ui.my_demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.fragment.project.R
import com.example.fragment.project.components.ArrowRightItem
import com.example.fragment.project.components.DatePicker
import com.example.fragment.project.components.EllipsisText
import com.example.fragment.project.components.TitleBar
import com.example.miaow.base.utils.getMetaData
import com.example.miaow.picture.selector.bean.MediaBean
import com.example.miaow.picture.selector.dialog.PictureSelectorCallback
import com.example.miaow.picture.selector.dialog.PictureSelectorDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MyDemoScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )
    val fullTextDialog = remember { mutableStateOf(false) }
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { scope.launch { sheetState.hide() } },
                    modifier = Modifier
                        .width(50.dp)
                        .height(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, colorResource(R.color.gray)),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(R.color.gray),
                        contentColor = colorResource(R.color.text_666)
                    ),
                    contentPadding = PaddingValues(5.dp, 3.dp, 5.dp, 3.dp)
                ) {
                    Text(text = "取消", fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { scope.launch { sheetState.hide() } },
                    modifier = Modifier
                        .width(50.dp)
                        .height(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, colorResource(R.color.gray)),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(R.color.theme_orange),
                        contentColor = colorResource(R.color.text_fff)
                    ),
                    contentPadding = PaddingValues(5.dp, 3.dp, 5.dp, 3.dp)
                ) {
                    Text(text = "确定", fontSize = 13.sp)
                }
            }
            Spacer(
                Modifier
                    .background(colorResource(R.color.line))
                    .fillMaxWidth()
                    .height(1.dp)
            )
            DatePicker(
                onSelectYear = {
                    println("year: $it")
                },
                onSelectMonth = {
                    println("month: $it")
                },
                onSelectDay = {
                    println("day: $it")
                }
            )
        }
    ) {
        val storagePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            )
        else
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        var storageDialog by remember { mutableStateOf(false) }
        val requestPermissions =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { ps ->
                var isGranted = true
                ps.entries.forEach {
                    if (it.key in storagePermissions && !it.value)
                        isGranted = false
                }
                storageDialog = !isGranted
            }
        if (storageDialog) {
            AlertDialog(
                onDismissRequest = { storageDialog = false },
                title = { Text(text = "申请存储空间权限") },
                text = { Text(text = "玩Android需要使用存储空间，我们想要将文章内容缓存到本地，从而加快打开速度和减少用户流量使用") },
                confirmButton = {
                    TextButton(onClick = { requestPermissions.launch(storagePermissions) }) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { storageDialog = false }) { Text("取消") }
                }
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TitleBar(title = context.getMetaData("app_channel")) {
                if (context is AppCompatActivity) {
                    context.onBackPressedDispatcher.onBackPressed()
                }
            }
            Spacer(Modifier.height(1.dp))
            ArrowRightItem("申请存储权限demo") {
                storageDialog = !storagePermissions.all {
                    ContextCompat.checkSelfPermission(
                        context, it
                    ) == PackageManager.PERMISSION_GRANTED
                }
                if (!storageDialog) {
                    Toast.makeText(context, "存储权限已经获取", Toast.LENGTH_SHORT).show()
                }
            }
            Spacer(Modifier.height(1.dp))
            ArrowRightItem("日期选择器demo") {
                scope.launch {
                    sheetState.show()
                }
            }
            Spacer(Modifier.height(1.dp))
            ArrowRightItem("图片选择器demo") {
                if (context is AppCompatActivity) {
                    PictureSelectorDialog
                        .newInstance()
                        .setPictureSelectorCallback(object : PictureSelectorCallback {
                            override fun onSelectedData(data: List<MediaBean>) {
                            }
                        })
                        .show(context.supportFragmentManager)
                }
            }
            Spacer(Modifier.height(1.dp))
            ArrowRightItem("全文demo") {
                fullTextDialog.value = true
            }
            Spacer(Modifier.height(1.dp))
        }
        if (fullTextDialog.value) {
            AlertDialog(
                onDismissRequest = { fullTextDialog.value = false },
                title = { Text(text = "全文demo") },
                text = {
                    Column {
                        var isEllipsis by remember { mutableStateOf(true) }
                        EllipsisText(
                            text = buildAnnotatedString {
                                append(
                                    "壬戌之秋1，七月既望2，苏子与客泛舟游于赤壁之下。清风徐来3，水波不兴4。" +
                                            "举酒属客5，诵明月之诗6，歌窈窕之章7。少焉8，月出于东山之上，徘徊于斗牛之间9。" +
                                            "白露横江10，水光接天。纵一苇之所如，凌万顷之茫然11。浩浩乎如冯虚御风12，而不知其所止；飘飘乎如遗世独立13，羽化而登仙14。\n" +
                                            "于是饮酒乐甚，扣舷而歌之15。歌曰：“桂棹兮兰桨16，击空明兮溯流光17。渺渺兮予怀18，望美人兮天一方19。”" +
                                            "客有吹洞箫者，倚歌而和之20。其声呜呜然，如怨如慕21，如泣如诉；余音袅袅22，不绝如缕23。舞幽壑之潜蛟24，泣孤舟之嫠妇25。\n" +
                                            "苏子愀然26，正襟危坐27，而问客曰：“何为其然也28？”..."
                                )
                            },
                            fontSize = 14.sp,
                            maxLines = if (isEllipsis) 2 else Int.MAX_VALUE,
                            background = colorResource(R.color.white),
                            ellipsisText = if (isEllipsis) "...展开" else "...收起"
                        ) {
                            isEllipsis = !isEllipsis
                        }
                        Spacer(Modifier.height(10.dp))
                        EllipsisText(
                            text = buildAnnotatedString {
                                append("你的人生格言是什么？")
                            },
                            fontSize = 12.sp,
                            background = colorResource(R.color.white),
                            maxLines = 2,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { fullTextDialog.value = false }) { Text("确定") }
                }
            )
        }
    }
}

