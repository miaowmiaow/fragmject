package com.example.fragment.project.activity

import android.os.Bundle
import android.view.LayoutInflater
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.constant.Router
import com.example.fragment.project.fragment.MainFragment
import com.example.fragment.project.R
import com.example.fragment.project.databinding.ActivityMainBinding

class MainActivity : RouterActivity() {

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
            Router.MAIN -> {
                switcher(MainFragment::class.java, bundle, onBack, navMode)
            }
            Router.LOGIN -> {
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(ActivityMainBinding.inflate(LayoutInflater.from(this)).root)
        navigation(Router.MAIN)
    }
}