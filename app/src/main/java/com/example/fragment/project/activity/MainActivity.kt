package com.example.fragment.project.activity

import android.graphics.PixelFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import com.example.fragment.library.base.bus.SimpleLiveBus
import com.example.fragment.library.common.activity.RouterActivity
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
import com.example.fragment.user.fragment.CoinRankFragment
import com.example.fragment.user.fragment.LoginFragment
import com.example.fragment.user.fragment.MyCoinFragment
import com.example.fragment.user.fragment.RegisterFragment


class MainActivity : RouterActivity() {

    private var id: String? = null

    override fun frameLayoutId(): Int {
        return R.id.frame_layout
    }

    override fun navigation(
        name: Router,
        bundle: Bundle?,
        onBack: Boolean,
        navMode: NavMode
    ) {
        when (name) {
            Router.LOGIN -> {
                switcher(LoginFragment::class.java, bundle, onBack, navMode)
            }
            Router.REGISTER -> {
                switcher(RegisterFragment::class.java, bundle, onBack, navMode)
            }
            Router.SEARCH -> {
                switcher(SearchFragment::class.java, bundle, onBack, navMode)
            }
            Router.MAIN -> {
                switcher(MainFragment::class.java, bundle, onBack, navMode)
            }
            Router.SYSTEM -> {
                switcher(SystemListFragment::class.java, bundle, onBack, navMode)
            }
            Router.WEB -> {
                switcher(WebFragment::class.java, bundle, onBack, navMode)
            }
            Router.COIN_RANK -> {
                switcher(CoinRankFragment::class.java, bundle, onBack, navMode)
            }
            else -> {
                if (id != null && id.toString().isNotBlank()) {
                    when (name) {
                        Router.MY_COIN -> {
                            switcher(MyCoinFragment::class.java, bundle, onBack, navMode)
                        }
                        Router.PUBLISH -> {

                        }
                        else -> {
                            switcher(MainFragment::class.java, bundle, onBack, navMode)
                        }
                    }
                } else {
                    switcher(LoginFragment::class.java, bundle, onBack, navMode)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFormat(PixelFormat.TRANSLUCENT)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        setTheme(R.style.AppTheme)
        setContentView(ActivityMainBinding.inflate(LayoutInflater.from(this)).root)
        navigation(Router.MAIN)
        SimpleLiveBus.with<Boolean>(LiveBus.USER_STATUS_UPDATE).observe(this, {
            WanHelper.getUser().observe(this, { userBean ->
                id = userBean.id
            })
        })
    }

    override fun onStart() {
        super.onStart()
        SimpleLiveBus.with<Boolean>(LiveBus.USER_STATUS_UPDATE).postEvent(true)
    }

}