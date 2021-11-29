package com.example.fragment.project.activity

import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import com.example.fragment.library.base.bus.SharedFlowBus
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.project.R
import com.example.fragment.project.databinding.ActivityMainBinding

class MainActivity : RouterActivity() {

    private var userId: String? = null
    private val loginRequired = arrayOf(
        Router.MY_COIN,
        Router.MY_COLLECT,
        Router.MY_SHARE,
        Router.SHARE_ARTICLE,
        Router.USER_AVATAR
    )

    override fun controllerId(): Int {
        return R.id.nav_host_fragment_main
    }

    /**
     * 导航方法，根据路由名跳转Fragment
     */
    override fun navigation(name: Router, bundle: Bundle?) {
        if (loginRequired.contains(name) && !isLogin()) {
            //登录态校验
            navigate("http://fragment.example.com/login")
        } else {
            when (name) {
                Router.COIN2RANK -> navigate(R.id.action_coin_to_rank, bundle)
                Router.MY_COIN -> navigate(R.id.action_main_to_my_coin, bundle)
                Router.MY_COLLECT -> navigate(R.id.action_main_to_my_collect, bundle)
                Router.MY_SHARE -> navigate(R.id.action_main_to_my_share, bundle)
                Router.SEARCH -> navigate(
                    StringBuilder()
                        .append("http://fragment.example.com/search")
                        .append("/${bundle?.getString(Keys.VALUE)}").toString()
                )
                Router.SETTING -> navigate(R.id.action_main_to_setting, bundle)
                Router.SETTING2WEB -> navigate(R.id.action_setting_to_web, bundle)
                Router.SHARE_ARTICLE -> navigate("http://fragment.example.com/share-article")
                Router.SYSTEM -> navigate(
                    StringBuilder()
                        .append("http://fragment.example.com/system")
                        .append("/${bundle?.getString(Keys.CID)}").toString()
                )
                Router.SYSTEM_URL -> navigate(
                    StringBuilder()
                        .append("http://fragment.example.com/system-url")
                        .append("/${Uri.encode(bundle?.getString(Keys.URL))}").toString()
                )
                Router.USER_AVATAR -> navigate("http://fragment.example.com/avatar")
                Router.USER_LOGIN -> navigate("http://fragment.example.com/login")
                Router.USER_REGISTER -> navigate("http://fragment.example.com/register")
                Router.USER_SHARE -> navigate(
                    StringBuilder()
                        .append("http://fragment.example.com/user-share")
                        .append("/${bundle?.getString(Keys.UID)}").toString()
                )
                Router.WEB -> navigate(
                    StringBuilder()
                        .append("http://fragment.example.com/web")
                        .append("/${Uri.encode(bundle?.getString(Keys.URL))}").toString()
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        window.setFormat(PixelFormat.TRANSLUCENT)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        setContentView(ActivityMainBinding.inflate(LayoutInflater.from(this)).root)
        //获取本地用户信息
        WanHelper.getUser {
            userId = it.id
        }
        //根据本地配置设置显示模式
        WanHelper.getUIMode {
            if (it != AppCompatDelegate.getDefaultNightMode()) {
                when (it) {
                    1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
        }
        //监听用户状态
        SharedFlowBus.onSticky(UserBean::class.java).observe(this) { userBean ->
            userId = userBean.id
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //关闭Database
        WanHelper.close()
    }

    /**
     * 登录状态校验
     */
    private fun isLogin(): Boolean {
        return !userId.isNullOrBlank()
    }

}