package com.example.fragment.library.common.activity

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.fragment.library.base.R
import com.example.fragment.library.base.activity.BaseActivity
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.common.constant.Router
import java.util.regex.Pattern

/**
 * 路由类，方便模块之间调用
 */
abstract class RouterActivity : BaseActivity() {

    private lateinit var navController: NavController
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    override fun setContentView(view: View) {
        super.setContentView(view)
        val navHostFragment = supportFragmentManager.findFragmentById(controllerId())
        navController = (navHostFragment as NavHostFragment).navController
    }

    fun navigate(@IdRes resId: Int, args: Bundle? = null) {
        navController.navigate(
            resId, args, NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
                .build()
        )
    }

    /**
     * 通过正则匹配“{}”内的参数并替换
     */
    fun navigate(deepLink: String, args: Bundle? = null) {
        var newDeepLink = "http://fragment.example.com/$deepLink"
        args?.let {
            val matcher = Pattern.compile("(\\{)(.+?)(\\})").matcher(newDeepLink)
            while (matcher.find()) {
                val key = matcher.group(2)
                if (it.containsKey(key)) {
                    newDeepLink = newDeepLink.replace("{$key}", it.getString(key, ""))
                }
            }
        }
        navController.navigate(
            Uri.parse(newDeepLink), NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
                .build()
        )
    }

    fun popBackStack(@IdRes destinationId: Int, inclusive: Boolean) {
        navController.popBackStack(destinationId, inclusive)
    }

    /**
     * 网络请求code处理
     * 封装在网络框架中扩展太差，暂时写在此处待优化
     */
    fun <T : HttpResponse> httpParseSuccess(result: T?, success: ((T) -> Unit)) {
        result?.let {
            when (it.errorCode) {
                "0" -> success.invoke(it)
                "-1001" -> navigation(Router.USER_LOGIN)
                else -> showTips(it.errorMsg)
            }
        }
    }

}