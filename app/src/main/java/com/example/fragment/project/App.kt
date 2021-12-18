package com.example.fragment.project

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.fragment.library.base.http.GSonConverter
import com.example.fragment.library.base.http.SimpleHttp
import com.example.fragment.library.base.utils.OkHelper
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk

class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        initHttp()
        initQbSdk()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(applicationContext)
            .crossfade(true)
            .okHttpClient { OkHelper.httpClient(applicationContext) }
            .componentRegistry {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder(applicationContext))
                } else {
                    add(GifDecoder())
                }
            }
            .build()
    }

    private fun initHttp() {
        SimpleHttp.setBaseUrl("https://www.wanandroid.com/")
            .setHttpClient(OkHelper.httpClient(applicationContext))
            .setConverter(GSonConverter.create())
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