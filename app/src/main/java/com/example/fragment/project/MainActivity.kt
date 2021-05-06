package com.example.fragment.project

import android.os.Bundle
import android.view.LayoutInflater
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.constant.Router
import com.example.fragment.project.databinding.ActivityMainBinding
import java.util.*
import kotlin.concurrent.schedule

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
        val binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        navigation(Router.MAIN)
    }
}