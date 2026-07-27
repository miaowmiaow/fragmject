package com.example.miaow.picture.components.layer

import android.graphics.Canvas
import android.view.MotionEvent

interface ILayer {
    fun onTouchEvent(event: MotionEvent): Boolean
    fun onSizeChanged(viewWidth: Int, viewHeight: Int, bitmapWidth: Int, bitmapHeight: Int)
    fun onDraw(canvas: Canvas)
}