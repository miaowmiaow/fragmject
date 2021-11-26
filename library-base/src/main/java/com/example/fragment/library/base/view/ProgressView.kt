package com.example.fragment.library.base.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.example.fragment.library.base.R

class ProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var progress: HeartView

    init {
        inflate(context, R.layout.view_progress, this)
        progress = findViewById(R.id.progress)
        visibility = View.GONE
        setOnClickListener {
            dismiss()
        }
    }

    fun show() {
        if(visibility == View.GONE){
            visibility = View.VISIBLE
            post {
                if (progress.isStopped) {
                    progress.start()
                }
            }
        }
    }

    fun dismiss() {
        if(visibility == View.VISIBLE){
            if (!progress.isStopped) {
                progress.stop()
            }
            visibility = View.GONE
        }
    }

}