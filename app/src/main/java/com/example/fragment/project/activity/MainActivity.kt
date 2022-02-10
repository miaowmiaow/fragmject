package com.example.fragment.project.activity

import android.graphics.PixelFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import com.example.fragment.library.base.utils.WebViewManager
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.project.R
import com.example.fragment.project.databinding.MainActivityBinding
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk

class MainActivity : RouterActivity() {

    private val baseDeepLink = "http://fragment.example.com"

    private val loginRequired = arrayOf(
        Router.MY_COIN,
        Router.MY_COLLECT,
        Router.MY_SHARE,
        Router.USER_AVATAR,
        Router.USER_SHARE
    )

    private var userBean = UserBean()

    override fun controllerId(): Int {
        return R.id.nav_host_fragment_main
    }

    /**
     * 导航方法，根据路由名跳转Fragment
     */
    override fun navigation(name: Router, bundle: Bundle?) {
        //登录态校验
        if (loginRequired.contains(name) && userBean.id.isBlank()) {
            navigate("$baseDeepLink/user/login")
        } else when (name) {
            Router.COIN2RANK -> navigate("$baseDeepLink/coin/rank", bundle)
            Router.MAIN -> popBackStack(R.id.main, false)
            Router.MY_COIN -> navigate("$baseDeepLink/my/coin", bundle)
            Router.MY_COLLECT -> navigate("$baseDeepLink/my/collect", bundle)
            Router.MY_SHARE -> navigate("$baseDeepLink/my/share", bundle)
            Router.SEARCH -> navigate("$baseDeepLink/search/{value}", bundle)
            Router.SETTING -> navigate(R.id.action_main_to_setting, bundle)
            Router.SHARE_ARTICLE -> navigate("$baseDeepLink/share/article/{uid}", bundle)
            Router.SYSTEM -> navigate("$baseDeepLink/system/{cid}", bundle)
            Router.SYSTEM_URL -> navigate("$baseDeepLink/system/url/{url}", bundle)
            Router.USER_AVATAR -> navigate("$baseDeepLink/user/avatar", bundle)
            Router.USER_INFO -> navigate("$baseDeepLink/user/info", bundle)
            Router.USER_LOGIN -> navigate("$baseDeepLink/user/login", bundle)
            Router.USER_REGISTER -> navigate("$baseDeepLink/user/register", bundle)
            Router.USER_SHARE -> navigate("$baseDeepLink/user/share", bundle)
            Router.WEB -> navigate("$baseDeepLink/web/{url}", bundle)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        window.setFormat(PixelFormat.TRANSLUCENT)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        setContentView(MainActivityBinding.inflate(LayoutInflater.from(this)).root)
        initWanHelper()
        initQbSdk()
    }

    override fun onDestroy() {
        super.onDestroy()
        WanHelper.close()
        WebViewManager.destroy()
    }

    private fun initWanHelper(){
        WanHelper.registerUser(this) { userBean = it }
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
        WanHelper.getUser()
        WanHelper.getUIMode()
    }

    /**
     * 初始化X5内核
     */
    private fun initQbSdk() {
        Thread {
            val map = HashMap<String, Any>()
            map[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
            map[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
            QbSdk.initTbsSettings(map)
            QbSdk.initX5Environment(applicationContext, object : QbSdk.PreInitCallback {
                override fun onViewInitFinished(arg0: Boolean) {
                }

                override fun onCoreInitFinished() {
                }
            })
        }.start()
        //WebView预加载
        WebViewManager.prepare(applicationContext)
    }

}