package com.example.fragment.project

import android.app.Application
import com.example.fragment.library.base.http.SimpleHttp
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        initSimpleHttp()
        initQbSdk()
    }

    private fun initSimpleHttp() {
        SimpleHttp.setBaseUrl("https://www.wanandroid.com/")
    }

    /**
     * 初始化x5内核
     */
    private fun initQbSdk() {
        QbSdk.initX5Environment(applicationContext, object : QbSdk.PreInitCallback {
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