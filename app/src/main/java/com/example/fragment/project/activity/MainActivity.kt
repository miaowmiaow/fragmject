package com.example.fragment.project.activity

import android.graphics.PixelFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import com.example.fragment.library.base.utils.WebViewManager
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.model.UserViewModel
import com.example.fragment.project.R
import com.example.fragment.project.databinding.MainActivityBinding
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk

class MainActivity : RouterActivity() {

    private val viewModel: UserViewModel by viewModels()
    private val loginRouter = arrayOf(
        Router.MY_COIN,
        Router.MY_COLLECT,
        Router.MY_SHARE,
        Router.USER_AVATAR,
        Router.USER_SHARE
    )

    override fun controllerId(): Int {
        return R.id.nav_host_fragment_main
    }

    /**
     * 导航方法，根据路由名跳转Fragment
     * [一文看懂Navigation](https://juejin.cn/post/7036296113573347364)
     */
    override fun navigation(name: Router, bundle: Bundle?) {
        //登录态校验
        if (loginRequired(name)) {
            navigate("user/login", bundle)
        } else when (name) {
            Router.COIN2RANK -> navigate("coin/rank", bundle)
            Router.MAIN -> popBackStack(R.id.main, false)
            Router.MY_COIN -> navigate("my/coin", bundle)
            Router.MY_COLLECT -> navigate("my/collect", bundle)
            Router.MY_SHARE -> navigate("my/share", bundle)
            Router.SEARCH -> navigate("search/{value}", bundle)
            Router.SETTING -> navigate(R.id.action_main_to_setting, bundle)
            Router.SHARE_ARTICLE -> navigate("share/article/{uid}", bundle)
            Router.SYSTEM -> navigate("system/{cid}", bundle)
            Router.USER_AVATAR -> navigate("user/avatar", bundle)
            Router.USER_CITY -> navigate("user/city", bundle)
            Router.USER_INFO -> navigate("user/info", bundle)
            Router.USER_LOGIN -> navigate("user/login", bundle)
            Router.USER_REGISTER -> navigate("user/register", bundle)
            Router.USER_SHARE -> navigate("user/share", bundle)
            Router.WEB -> navigate("web/{url}", bundle)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        window.setFormat(PixelFormat.TRANSLUCENT)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        setContentView(MainActivityBinding.inflate(LayoutInflater.from(this)).root)
        initViewModel()
        initSDK()
        initUIMode()
    }

    override fun onDestroy() {
        super.onDestroy()
        WanHelper.close()
        WebViewManager.destroy()
    }

    private fun initViewModel() {
        viewModel.userResult()
    }

    /**
     * 注意: 第三方sdk需在用户同意隐私协议后初始化
     */
    private fun initSDK() {
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

    /**
     * 单 Activity 通过 ViewModel 来实现消息总线更优雅（参考UserViewModel）
     * 此处为用而用: SharedFlowBus 消息总线
     */
    private fun initUIMode() {
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
    }

    private fun loginRequired(name: Router): Boolean {
        return loginRouter.contains(name) && viewModel.getUserId().isBlank()
    }

}