package com.example.fragment.library.common.activity

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.fragment.library.base.R
import com.example.fragment.library.base.activity.BaseActivity
import com.example.fragment.library.common.constant.Router
import java.util.regex.Pattern

/**
 * 路由类，方便模块之间调用
 */
abstract class RouterActivity : BaseActivity() {

    private lateinit var navController: NavController
    private var exitTime = 0L
    private val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_right)
        .setExitAnim(R.anim.slide_out_left)
        .setPopEnterAnim(R.anim.slide_in_left)
        .setPopExitAnim(R.anim.slide_out_right)
        .build()

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
        val navHostFragment = supportFragmentManager.findFragmentById(controllerId())
        navController = (navHostFragment as NavHostFragment).navController
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

    fun navigate(@IdRes resId: Int, args: Bundle? = null) {
        navController.navigate(resId, args, navOptions)
    }

    /**
     * 通过正则匹配“{}”内的参数并替换
     */
    fun navigate(deepLink: String, args: Bundle? = null) {
        var newDeepLink = deepLink
        args?.apply {
            val matcher = Pattern.compile("(\\{)(.+?)(\\})").matcher(deepLink)
            while (matcher.find()) {
                matcher.group(2)?.let { key ->
                    if (containsKey(key)) {
                        newDeepLink = deepLink.replace("{$key}", get(key).toString())
                    }
                }
            }
        }
        navController.navigate(Uri.parse(newDeepLink), navOptions)
    }

    fun popBackStack(@IdRes destinationId: Int, inclusive: Boolean) {
        navController.popBackStack(destinationId, inclusive)
    }

}