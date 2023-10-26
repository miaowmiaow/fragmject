package com.example.fragment.project

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.platform.ComposeView
import com.example.fragment.project.utils.WanHelper

class WanActivity : AppCompatActivity() {

    private var exitTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Wan)
        setContentView(intent)
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
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setContentView(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        WanHelper.close()
    }

    private fun setContentView(intent: Intent?) {
        setContentView(ComposeView(this).apply {
            /**
             * 解析 scheme 协议（如果有的话）导航到指定页面
             * path 为指定页面导航 route，详情参考 WanNavGraph，示例如下：
             * wan://com.fragment.project/rank_route
             * wan://com.fragment.project/search_route/动画
             * wan://com.fragment.project/web_route/https://wanandroid.com
             */
            val scheme = intent?.data?.scheme
            val host = intent?.data?.host
            val path = intent?.data?.path
            val route = when {
                scheme == "wan" && host == "com.fragment.project" ->
                    path.toString().substring(1)

                else -> null
            }
            setContent {
                WanTheme {
                    WanNavGraph(route)
                }
            }
        })
    }

}