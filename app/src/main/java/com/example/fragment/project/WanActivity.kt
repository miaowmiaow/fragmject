package com.example.fragment.project

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.compose.rememberNavController
import com.example.fragment.project.ui.web.WebViewManager
import com.example.fragment.project.utils.WanHelper

class WanActivity : AppCompatActivity() {

    private var exitTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ComposeView(this).apply {
            setContent {
                WanTheme {
                    val navController = rememberNavController()
                    val startDestination = WanDestinations.MAIN_ROUTE
                    WanNavGraph(navController, startDestination)
                }
            }
        })
        //设置显示模式
        WanHelper.getUiMode { mode ->
            when (mode) {
                "1" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "2" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
        //双击返回键回退桌面
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - exitTime > 2000) {
                    exitTime = System.currentTimeMillis()
                    val msg = getString(R.string.one_more_press_2_back)
                    Toast.makeText(this@WanActivity, msg, Toast.LENGTH_SHORT).show()
                } else {
                    moveTaskToBack(true)
                }
            }
        })
        //WebView预加载
        WebViewManager.prepare(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        WanHelper.close()
        //WebView销毁
        WebViewManager.destroy()
    }

}