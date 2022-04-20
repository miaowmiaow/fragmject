package com.example.fragment.library.base.view

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.content.ContextCompat
import com.example.fragment.library.base.R

class SnailBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatSeekBar(context, attrs, defStyleAttr) {

    init {
        isEnabled = false
        max = 100
        thumb = ContextCompat.getDrawable(context, R.drawable.animation_snail_bar)
        progressDrawable = ContextCompat.getDrawable(context, R.drawable.rectangle_snail_bar)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (thumb as AnimationDrawable).start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        (thumb as AnimationDrawable).stop()
    }

}