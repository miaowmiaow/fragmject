package com.example.miaow.picture.selector

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.miaow.picture.selector.bean.MediaBean
import com.example.miaow.picture.selector.dialog.PictureSelectorCallback
import com.example.miaow.picture.selector.dialog.PictureSelectorDialog

class PictureSelectorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PictureSelectorDialog.newInstance()
            .setPictureSelectorCallback(object : PictureSelectorCallback {
                override fun onSelectedData(data: List<MediaBean>) {
                    this@PictureSelectorActivity.setResult(RESULT_OK, intent.apply {
                        putParcelableArrayListExtra("data", ArrayList(data))
                    })
                    this@PictureSelectorActivity.finish()
                }
            })
            .show(supportFragmentManager)
    }
}