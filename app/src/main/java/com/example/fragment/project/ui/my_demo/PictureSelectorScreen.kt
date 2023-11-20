package com.example.fragment.project.ui.my_demo

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.project.R
import com.example.miaow.picture.selector.bean.MediaBean
import com.example.miaow.picture.selector.dialog.PictureSelectorCallback
import com.example.miaow.picture.selector.dialog.PictureSelectorDialog

@Composable
fun PictureSelectorScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
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
            modifier = Modifier.height(30.dp),
            shape = RoundedCornerShape(3.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.white),
                contentColor = colorResource(R.color.theme_orange)
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
            border = BorderStroke(1.dp, colorResource(R.color.theme_orange)),
            contentPadding = PaddingValues(3.dp, 2.dp, 3.dp, 2.dp)
        ) {
            Text(
                text = "打开相册",
                fontSize = 12.sp
            )
        }
    }
}