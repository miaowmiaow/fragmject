package com.example.miaow.picture.data

import android.graphics.Bitmap

class StickerAttrs(
    var bitmap: Bitmap,
    var description: String = "",
    var rotation: Float = 0f,
    var scale: Float = 1f,
    var translateX: Float = 0f,
    var translateY: Float = 0f,
    var isFlipped: Boolean = false,
)