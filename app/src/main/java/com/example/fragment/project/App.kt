package com.example.fragment.project

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import com.example.fragment.library.base.http.setBaseUrl
import com.example.fragment.library.base.http.setHttpClient
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder(applicationContext))
                } else {
                    add(GifDecoder())
                }
                add(SvgDecoder(applicationContext))
                add(VideoFrameDecoder(applicationContext))
            }
            .build()
    }

    private fun initHttp() {
        setBaseUrl("https://www.wanandroid.com/")
        setHttpClient(OkHelper.httpClient(applicationContext))
    }

}