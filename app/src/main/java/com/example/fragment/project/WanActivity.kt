package com.example.fragment.project

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.fragment.project.ui.web.WebViewManager
import com.example.fragment.project.utils.WanHelper
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WanActivity : AppCompatActivity() {

    private val mainScope = MainScope()
    private var exitTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Wan)
        setContentView(parseScheme(intent.data))
        //设置显示模式
        mainScope.launch {
            val mode = WanHelper.getUiMode()
            if (mode != AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.setDefaultNightMode(WanHelper.getUiMode())
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
        WebViewManager.prepare(applicationContext)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let {
            setContentView(parseScheme(it))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
        WanHelper.close()
        WebViewManager.destroy()
    }

    /**
     * 解析 url scheme（如果有的话）导航到指定页面
     * path 为指定页面导航 route，详情参考 WanNavGraph，示例如下：
     * wan://com.fragment.project/rank_route
     * wan://com.fragment.project/search_route/动画
     * wan://com.fragment.project/web_route/https://wanandroid.com
     */
    private fun parseScheme(uri: Uri?): String? {
        return when {
            uri != null && uri.scheme == "wan" && uri.host == "com.fragment.project" ->
                uri.path.toString().substring(1)

            else -> null
        }
    }

    private fun setContentView(route: String?) {
        setContent {
            WanTheme {
                WanNavGraph(route)
            }
        }
    }

}