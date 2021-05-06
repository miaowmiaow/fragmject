package com.example.fragment.project

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.example.fragment.library.base.http.SimpleHttp

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        SimpleHttp.setBaseUrl("https://www.wanandroid.com/")
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(base)
    }
}