package com.example.fragment.project

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.example.fragment.library.base.http.SimpleHttp
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.QbSdk.PreInitCallback
import kotlin.concurrent.thread

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        thread {
            initSimpleHttp()
            initQbSdk()
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(base)
    }

    private fun initSimpleHttp() {
        SimpleHttp.setBaseUrl("https://www.wanandroid.com/")
    }

    private fun initQbSdk() {
        //x5内核初始化接口
        QbSdk.initX5Environment(applicationContext, object : PreInitCallback {
            override fun onViewInitFinished(arg0: Boolean) {
            }

            override fun onCoreInitFinished() {
            }
        })
        val map = HashMap<String, Any>()
        map[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
        QbSdk.initTbsSettings(map)
    }
}