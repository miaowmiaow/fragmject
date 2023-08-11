package com.example.fragment.project.ui.my_demo

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.project.R
import com.example.fragment.project.components.WheelPicker
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
    val years = List(151) { i -> "${i + 1900}" }
    val months = List(12) { i -> "${i + 1}" }
    val days = List(31) { i -> "${i + 1}" }
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
                    onClick = {
                        scope.launch { sheetState.hide() }
                    },
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
                    Text(
                        text = "取消",
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }
                    },
                    modifier = Modifier
                        .width(50.dp)
                        .height(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, colorResource(R.color.gray)),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(R.color.green),
                        contentColor = colorResource(R.color.text_fff)
                    ),
                    contentPadding = PaddingValues(5.dp, 3.dp, 5.dp, 3.dp)
                ) {
                    Text(
                        text = "保密",
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.width(5.dp))
                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }
                    },
                    modifier = Modifier
                        .width(50.dp)
                        .height(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, colorResource(R.color.gray)),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(R.color.blue),
                        contentColor = colorResource(R.color.text_fff)
                    ),
                    contentPadding = PaddingValues(5.dp, 3.dp, 5.dp, 3.dp)
                ) {
                    Text(
                        text = "确定",
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(
                Modifier
                    .background(colorResource(R.color.line))
                    .fillMaxWidth()
                    .height(1.dp)
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(175.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    WheelPicker(
                        data = years,
                        selectIndex = 0,
                        visibleCount = 5,
                        modifier = Modifier
                            .width(70.dp)
                            .height(175.dp),
                        onSelect = { _, item ->
                            println("year: $item")
                        }
                    ) {
                        Text(
                            text = it,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "/",
                        fontSize = 15.sp
                    )
                    WheelPicker(
                        data = months,
                        selectIndex = 0,
                        visibleCount = 5,
                        modifier = Modifier
                            .width(70.dp)
                            .height(175.dp),
                        onSelect = { _, item ->
                            println("month: $item")
                        }
                    ) {
                        Text(
                            text = it,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "/",
                        fontSize = 15.sp
                    )
                    WheelPicker(
                        data = days,
                        selectIndex = 0,
                        visibleCount = 5,
                        modifier = Modifier
                            .width(70.dp)
                            .height(175.dp),
                        onSelect = { _, item ->
                            println("day: $item")
                        }
                    ) {
                        Text(
                            text = it,
                            fontSize = 14.sp
                        )
                    }
                }
                Column(Modifier.height(175.dp)) {
                    Spacer(
                        Modifier
                            .background(colorResource(R.color.bb_white))
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Spacer(
                        Modifier
                            .background(
                                colorResource(R.color.one_b_black),
                                RoundedCornerShape(5.dp)
                            )
                            .fillMaxWidth()
                            .height(30.dp)
                    )
                    Spacer(
                        Modifier
                            .background(colorResource(R.color.bb_white))
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .background(colorResource(R.color.theme))
            ) {
                IconButton(
                    modifier = Modifier.height(45.dp),
                    onClick = {
                        if (context is AppCompatActivity) {
                            context.onBackPressedDispatcher.onBackPressed()
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = null,
                        tint = colorResource(R.color.white)
                    )
                }
                Text(
                    text = "我的Demo",
                    fontSize = 16.sp,
                    color = colorResource(R.color.text_fff),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(
                Modifier
                    .background(colorResource(R.color.line))
                    .fillMaxWidth()
                    .height(1.dp)
            )
            Row(
                modifier = Modifier
                    .background(colorResource(R.color.white))
                    .fillMaxWidth()
                    .height(45.dp)
                    .clickable {
                        scope.launch {
                            sheetState.show()
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "日期选择器demo",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 25.dp, end = 25.dp),
                    fontSize = 13.sp,
                    color = colorResource(R.color.text_333),
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_right),
                    contentDescription = "",
                    modifier = Modifier.padding(start = 25.dp, end = 25.dp)
                )
            }
            Spacer(
                Modifier
                    .background(colorResource(R.color.line))
                    .fillMaxWidth()
                    .height(1.dp)
            )
            Row(
                modifier = Modifier
                    .background(colorResource(R.color.white))
                    .fillMaxWidth()
                    .height(45.dp)
                    .clickable {
                        if (context is AppCompatActivity) {
                            PictureSelectorDialog
                                .newInstance()
                                .setPictureSelectorCallback(object : PictureSelectorCallback {
                                    override fun onSelectedData(data: List<MediaBean>) {
                                    }
                                })
                                .show(context.supportFragmentManager)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "图片选择器demo",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 25.dp, end = 25.dp),
                    fontSize = 13.sp,
                    color = colorResource(R.color.text_333),
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_right),
                    contentDescription = "",
                    modifier = Modifier.padding(start = 25.dp, end = 25.dp)
                )
            }
            Spacer(
                Modifier
                    .background(colorResource(R.color.line))
                    .fillMaxWidth()
                    .height(1.dp)
            )
        }
    }
}

