package com.example.fragment.project.activity

import android.graphics.PixelFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import com.example.fragment.library.base.bus.SharedFlowBus
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.project.R
import com.example.fragment.project.databinding.ActivityMainBinding

class MainActivity : RouterActivity() {

    private var userId: String? = null
    private val loginRequired = arrayOf(
        Router.AVATAR,
        Router.LOGIN,
        Router.MY_COIN,
        Router.MY_COLLECT,
        Router.MY_SHARE,
        Router.SHARE
    )

    override fun controllerId(): Int {
        return R.id.nav_host_fragment_main
    }

    /**
     * 导航方法，根据id跳转Fragment
     */
    override fun navigation(resId: Int, bundle: Bundle?) {
        navController.navigate(resId, bundle)
    }

    /**
     * 导航方法，根据路由名跳转Fragment
     */
    override fun navigation(name: Router, bundle: Bundle?) {
        if (loginRequired.contains(name) && !isLogin()) {
            navController.navigate(R.id.action_main_to_login)
        } else {
            when (name) {
                Router.AVATAR -> navController.navigate(R.id.action_main_to_avatar, bundle)
                Router.LOGIN -> navController.navigate(R.id.action_main_to_login, bundle)
                Router.MY_COIN -> navController.navigate(R.id.action_main_to_my_coin, bundle)
                Router.MY_COLLECT -> navController.navigate(R.id.action_main_to_my_collect, bundle)
                Router.MY_SHARE -> navController.navigate(R.id.action_main_to_my_share, bundle)
                Router.PROJECT -> navController.navigate(R.id.action_main_to_project_list, bundle)
                Router.SEARCH -> navController.navigate(R.id.action_main_to_search, bundle)
                Router.SETTING -> navController.navigate(R.id.action_main_to_setting, bundle)
                Router.SHARE -> navController.navigate(R.id.action_main_to_share_article, bundle)
                Router.SYSTEM -> navController.navigate(R.id.action_main_to_system_list, bundle)
                Router.USER_SHARE -> navController.navigate(R.id.action_main_to_user_share, bundle)
                Router.WEB -> navController.navigate(R.id.action_main_to_web, bundle)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        window.setFormat(PixelFormat.TRANSLUCENT)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        setContentView(ActivityMainBinding.inflate(LayoutInflater.from(this)).root)
        initViewModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        WanHelper.close()
    }

    private fun initViewModel() {
        WanHelper.getUser().observe(this) { userBean ->
            userId = userBean.id
        }
        WanHelper.getUIMode().observe(this, {
            if (it != AppCompatDelegate.getDefaultNightMode()) {
                when (it) {
                    1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
        })
        SharedFlowBus.onSticky(UserBean::class.java).observe(this) { userBean ->
            userId = userBean.id
        }
    }

    /**
     * 登录状态校验
     */
    private fun isLogin(): Boolean {
        return !userId.isNullOrBlank()
    }

}