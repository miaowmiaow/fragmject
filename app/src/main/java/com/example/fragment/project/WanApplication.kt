package com.example.fragment.project

import android.app.Application
import android.os.Build
import coil.ComponentRegistry
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import com.example.miaow.base.http.OkHelper
import com.example.miaow.base.http.setBaseUrl
import com.example.miaow.base.http.setHttpClient

class WanApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        setBaseUrl("https://www.wanandroid.com/")
        setHttpClient(OkHelper.httpClient(applicationContext))
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(applicationContext)
            .crossfade(true)
            .okHttpClient { OkHelper.httpClient(applicationContext) }
            .components(fun ComponentRegistry.Builder.() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
                add(VideoFrameDecoder.Factory())
            })
            .build()
    }

}