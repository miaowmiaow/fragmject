package com.example.fragment.project.activity

import android.graphics.PixelFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import com.example.fragment.library.base.bus.SimpleLiveBus
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.LiveBus
import com.example.fragment.library.common.constant.NavMode
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.WebFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.home.fragment.SearchFragment
import com.example.fragment.module.system.fragment.SystemListFragment
import com.example.fragment.project.R
import com.example.fragment.project.databinding.ActivityMainBinding
import com.example.fragment.project.fragment.MainFragment
import com.example.fragment.user.fragment.*

class MainActivity : RouterActivity() {

    private var userId: String? = null

    override fun frameLayoutId(): Int {
        return R.id.frame_layout
    }

    /**
     * 导航方法，根据路由名跳转切换Fragment
     */
    override fun navigation(name: Router, bundle: Bundle?, onBack: Boolean, navMode: NavMode) {
        when (name) {
            Router.MAIN ->
                switcher(MainFragment::class.java, bundle, false, navMode)
            Router.LOGIN ->
                switcher(LoginFragment::class.java, bundle, onBack, navMode)
            Router.REGISTER ->
                switcher(RegisterFragment::class.java, bundle, onBack, navMode)
            Router.WEB ->
                switcher(WebFragment::class.java, bundle, onBack, navMode)
            Router.SEARCH ->
                switcher(SearchFragment::class.java, bundle, onBack, navMode)
            Router.SYSTEM_LIST ->
                switcher(SystemListFragment::class.java, bundle, onBack, navMode)
            Router.COIN_RANK ->
                switcher(CoinRankFragment::class.java, bundle, onBack, navMode)
            Router.USER_SHARE ->
                switcher(UserShareFragment::class.java, bundle, onBack, navMode)
            Router.SETTING ->
                switcher(SettingFragment::class.java, bundle, onBack, navMode)
            else -> {
                if (isLogin()) {
                    when (name) {
                        Router.MY_COIN ->
                            switcher(MyCoinFragment::class.java, bundle, onBack, navMode)
                        Router.MY_COLLECT_ARTICLE ->
                            switcher(MyCollectArticleFragment::class.java, bundle, onBack, navMode)
                        Router.MY_SHARE_ARTICLE ->
                            switcher(MyShareArticleFragment::class.java, bundle, onBack, navMode)
                        Router.SHARE_ARTICLE ->
                            switcher(ShareArticleFragment::class.java, bundle, onBack, navMode)
                        else -> switcher(MainFragment::class.java, bundle, onBack, navMode)
                    }
                } else {
                    switcher(LoginFragment::class.java, bundle, onBack, navMode)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        window.setFormat(PixelFormat.TRANSLUCENT)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        setContentView(ActivityMainBinding.inflate(LayoutInflater.from(this)).root)
        initUIMode()
        setupView()
        update()
    }

    override fun onStart() {
        super.onStart()
        WanHelper.getUser().observe(this, { userBean ->
            SimpleLiveBus.with<UserBean>(LiveBus.USER_STATUS_UPDATE).postEvent(userBean)
        })
    }

    private fun setupView() {
        navigation(Router.MAIN)
    }

    private fun update() {
        SimpleLiveBus.with<UserBean>(LiveBus.USER_STATUS_UPDATE).observe(this, { userBean ->
            userId = userBean.id
        })
    }

    /**
     * 登录状态校验
     */
    private fun isLogin(): Boolean {
        return userId != null && userId.toString().isNotBlank()
    }

}