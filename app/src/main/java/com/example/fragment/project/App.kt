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

class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        initHttp()
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

}