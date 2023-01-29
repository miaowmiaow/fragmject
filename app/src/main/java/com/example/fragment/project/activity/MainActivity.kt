package com.example.fragment.project.activity

import android.graphics.PixelFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.dialog.StandardDialog
import com.example.fragment.library.common.utils.TestAnnotation
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.model.SettingViewModel
import com.example.fragment.module.user.model.UserViewModel
import com.example.fragment.project.R
import com.example.fragment.project.databinding.MainActivityBinding

class MainActivity : RouterActivity() {

    private val settingViewModel: SettingViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
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
        WanHelper.privacyAgreement({ initContentView() }, {
            window.setBackgroundDrawableResource(R.drawable.bg)
            val listener = object : StandardDialog.OnDialogClickListener {
                override fun onConfirm(dialog: StandardDialog) {
                    WanHelper.allowPrivacyAgreement()
                    initContentView()
                }

                override fun onCancel(dialog: StandardDialog) {
                    WanHelper.denyPrivacyAgreement()
                    finish()
                }
            }
            StandardDialog.newInstance()
                .setTitle(getString(R.string.privacy_agreement_title))
                .setContent(getString(R.string.privacy_agreement_content))
                .setOnDialogClickListener(listener)
                .show(supportFragmentManager)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        WanHelper.close()
    }

    private fun initContentView() {
        window.setFormat(PixelFormat.TRANSLUCENT)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        setContentView(MainActivityBinding.inflate(LayoutInflater.from(this)).root)
        settingViewModel.uiModeResult().observe(this) { mode ->
            when (mode) {
                "1" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                "2" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
        userViewModel.userResult()
    }

    //测试自动埋点注解请忽略
    @TestAnnotation(code = 1, message = "qwe")
    private fun loginRequired(name: Router): Boolean {
        return loginRouter.contains(name) && userViewModel.getUserId().isBlank()
    }

}