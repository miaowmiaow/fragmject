package com.example.fragment.library.base.component.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.RelativeLayout
import android.widget.TextView
import com.example.fragment.library.base.R
import com.example.fragment.library.base.utils.MetricsUtils

class SimpleTitleBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private var statusBar: SimpleStatusBar
    private var left: TextView
    private var center: TextView
    private var right: TextView

    init {
        inflate(context, R.layout.view_simple_title_bar, this)
        statusBar = findViewById(R.id.status_bar)
        left = findViewById(R.id.left)
        center = findViewById(R.id.center)
        right = findViewById(R.id.right)
    }

    fun setLeft(
        text: String = "",
        size: Float = -1f,
        color: Int = -1,
        resId: Int = -1,
        listener: (() -> Unit)? = null
    ) {
        if (TextUtils.isEmpty(text)) {
            left.text = text
        }
        left.text = text
        if (size != -1f) {
            left.textSize = size
        }
        if (color != -1) {
            left.setTextColor(color)
        }
        if (resId != -1) {
            left.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0)
        }
        left.setOnClickListener {
            listener?.invoke()
        }
    }

    fun setCenter(text: String, size: Float = -1f, color: Int = -1) {
        center.text = text
        if (size != -1f) {
            center.textSize = size
        }
        if (color != -1) {
            center.setTextColor(color)
        }
    }

    fun setRight(
        text: String = "",
        size: Float = -1f,
        color: Int = -1,
        resId: Int = -1,
        listener: (() -> Unit)? = null
    ) {
        if (TextUtils.isEmpty(text)) {
            right.text = text
        }
        right.text = text
        if (size != -1f) {
            right.textSize = size
        }
        if (color != -1) {
            right.setTextColor(color)
        }
        if (resId != -1) {
            right.setCompoundDrawablesWithIntrinsicBounds(0, 0, resId, 0)
        }
        right.setOnClickListener {
            listener?.invoke()
        }
    }

    fun setBgDrawable(drawable: Drawable) {
        background = drawable
    }

}