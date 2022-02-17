package com.example.fragment.library.common.activity

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.fragment.library.base.R
import com.example.fragment.library.base.activity.BaseActivity
import com.example.fragment.library.base.utils.WebViewManager
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.utils.WanHelper
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk
import java.util.regex.Pattern

/**
 * 路由类，方便模块之间调用
 */
abstract class RouterActivity : BaseActivity() {

    private lateinit var navController: NavController
    private var exitTime = 0L

    /**
     * NavController的视图id
     */
    abstract fun controllerId(): Int

    /**
     * 导航方法，根据路由名跳转
     */
    abstract fun navigation(
        name: Router,
        bundle: Bundle? = null,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /**
         * 纯粹为以下知识点服务：
         * 1、SharedFlowBus 消息总线
         */
        WanHelper.registerUIMode(this) { eventBean ->
            if (eventBean.key == WanHelper.UI_MODE) {
                when (eventBean.value) {
                    "1" -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        QbSdk.unForceSysWebView()
                    }
                    "2" -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        QbSdk.forceSysWebView()
                    }
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
        }
        WanHelper.getUIMode()
        Thread {
            //X5内核初始化
            val map = HashMap<String, Any>()
            map[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
            map[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
            QbSdk.initTbsSettings(map)
            QbSdk.initX5Environment(applicationContext, object : QbSdk.PreInitCallback {
                override fun onViewInitFinished(arg0: Boolean) {}

                override fun onCoreInitFinished() {}
            })
        }.start()
        //WebView预加载
        WebViewManager.prepare(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        WanHelper.close()
        WebViewManager.destroy()
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        val navHostFragment = supportFragmentManager.findFragmentById(controllerId())
        navController = (navHostFragment as NavHostFragment).navController
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - exitTime > 2000) {
                    exitTime = System.currentTimeMillis()
                    val msg = getString(R.string.one_more_press_2_back)
                    Toast.makeText(this@RouterActivity, msg, Toast.LENGTH_SHORT).show()
                } else {
                    moveTaskToBack(true)
                }
            }
        })
    }

    fun navigate(@IdRes resId: Int, args: Bundle? = null) {
        navController.navigate(
            resId, args, NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
                .build()
        )
    }

    /**
     * 通过正则匹配“{}”内的参数并替换
     */
    fun navigate(deepLink: String, args: Bundle? = null) {
        var newDeepLink = "http://fragment.example.com/$deepLink"
        args?.apply {
            val matcher = Pattern.compile("(\\{)(.+?)(\\})").matcher(newDeepLink)
            while (matcher.find()) {
                val key = matcher.group(2)
                if (containsKey(key)) {
                    newDeepLink = newDeepLink.replace("{$key}", get(key).toString())
                }
            }
        }
        navController.navigate(
            Uri.parse(newDeepLink), NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
                .build()
        )
    }

    fun popBackStack(@IdRes destinationId: Int, inclusive: Boolean) {
        navController.popBackStack(destinationId, inclusive)
    }

}