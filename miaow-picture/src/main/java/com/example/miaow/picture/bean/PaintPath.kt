package com.example.miaow.picture.bean

import android.graphics.Paint
import android.graphics.Path

class PaintPath(path: Path, paint: Paint) {
    val paint = Paint(paint)
    val path = Path(path)
}