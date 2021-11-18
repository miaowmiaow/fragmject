package com.example.fragment.library.common.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.example.fragment.library.base.R
import com.example.fragment.library.base.activity.BaseActivity
import com.example.fragment.library.common.constant.Router

/**
 * 路由类，方便模块之间调用
 */
abstract class RouterActivity : BaseActivity() {

    lateinit var navController: NavController
    private var exitTime = 0L

    /**
     * NavController的视图id
     */
    abstract fun controllerId(): Int

    /**
     * 导航方法，根据路由名跳转
     */
    abstract fun navigation(
        name: Router,
        bundle: Bundle? = null,
    )

    override fun setContentView(view: View) {
        super.setContentView(view)
        navController = findNavController(controllerId())
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - exitTime > 2000) {
                    exitTime = System.currentTimeMillis()
                    val msg = getString(R.string.one_more_press_2_back)
                    Toast.makeText(this@RouterActivity, msg, Toast.LENGTH_SHORT).show()
                } else {
                    moveTaskToBack(true)
                }
            }
        })
    }

}