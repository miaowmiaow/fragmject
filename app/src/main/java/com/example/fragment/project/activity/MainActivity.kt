package com.example.fragment.project.activity

import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import com.example.fragment.library.base.utils.WebViewManager
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.project.R
import com.example.fragment.project.databinding.ActivityMainBinding
import com.tencent.smtt.sdk.QbSdk

class MainActivity : RouterActivity() {

    private val loginRequired = arrayOf(
        Router.MY_COIN,
        Router.MY_COLLECT,
        Router.MY_SHARE,
        Router.SHARE_ARTICLE,
        Router.USER_AVATAR
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
            navigate("http://fragment.example.com/login")
        } else when (name) {
            Router.COIN2RANK -> navigate(R.id.action_coin_to_rank, bundle)
            Router.MY_COIN -> navigate(R.id.action_main_to_my_coin, bundle)
            Router.MY_COLLECT -> navigate(R.id.action_main_to_my_collect, bundle)
            Router.MY_SHARE -> navigate(R.id.action_main_to_my_share, bundle)
            Router.SEARCH -> navigate(
                StringBuilder().append("http://fragment.example.com/search")
                    .append("/${bundle?.getString(Keys.VALUE)}").toString()
            )
            Router.SETTING -> navigate(R.id.action_main_to_setting, bundle)
            Router.SHARE_ARTICLE -> navigate("http://fragment.example.com/share-article")
            Router.SYSTEM -> navigate(
                StringBuilder().append("http://fragment.example.com/system")
                    .append("/${bundle?.getString(Keys.CID)}").toString()
            )
            Router.SYSTEM_URL -> navigate(
                StringBuilder().append("http://fragment.example.com/system-url")
                    .append("/${Uri.encode(bundle?.getString(Keys.URL))}").toString()
            )
            Router.USER_AVATAR -> navigate("http://fragment.example.com/avatar")
            Router.USER_LOGIN -> navigate("http://fragment.example.com/login")
            Router.USER_REGISTER -> navigate("http://fragment.example.com/register")
            Router.USER_SHARE -> navigate(
                StringBuilder().append("http://fragment.example.com/user-share")
                    .append("/${bundle?.getString(Keys.UID)}").toString()
            )
            Router.WEB -> navigate(
                StringBuilder().append("http://fragment.example.com/web")
                    .append("/${Uri.encode(bundle?.getString(Keys.URL))}").toString()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        window.setFormat(PixelFormat.TRANSLUCENT)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        setContentView(ActivityMainBinding.inflate(LayoutInflater.from(this)).root)
        //监听用户状态
        WanHelper.registerUser(this) { userBean = it }
        WanHelper.getUser()
        //设置显示模式
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

    override fun onDestroy() {
        super.onDestroy()
        WanHelper.close()
        WebViewManager.destroy()
    }

}