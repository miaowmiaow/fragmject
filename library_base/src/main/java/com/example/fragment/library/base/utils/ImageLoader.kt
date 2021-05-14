package com.example.fragment.library.base.utils

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition


class ImageLoader private constructor() {

    class Builder(context: Context) {

        private val imageLoader = ImageLoader()

        init {
            imageLoader.context = context
        }

        fun load(path: String): Builder {
            imageLoader.path = path
            return this
        }

        fun override(width: Int, height: Int): Builder {
            imageLoader.width = width
            imageLoader.height = height
            return this
        }

        fun into(view: ImageView) {
            imageLoader.into(view)
        }

        fun into(target: DrawableTarget) {
            imageLoader.into(target)
        }
    }

    companion object {
        fun with(context: Context): Builder {
            return Builder(context)
        }
    }

    private var context: Context? = null
    private var path: String = ""
    private var width = 0
    private var height = 0

    fun into(view: ImageView) {
        context?.apply {
            if (viewDestroyed(this)) {
                return
            }
            val requestManager = Glide.with(this)
            if (path.isBlank()) {
                return
            }
            requestManager.load(path).into(view)
        }
    }

    fun into(target: DrawableTarget) {
        context?.apply {
            if (viewDestroyed(this)) {
                return
            }
            val requestManager = Glide.with(this)
            if (path.isBlank()) {
                return
            }
            val requestBuilder = requestManager.asDrawable().load(path)
            if (width > 0 && height > 0) {
                requestBuilder.override(width, height)
                requestBuilder.centerCrop()
            }
            requestBuilder.into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    target.onResourceReady(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }

            })
        }
    }

    private fun viewDestroyed(context: Context): Boolean {
        if (context is Activity) {
            if (context.isFinishing || context.isDestroyed) {
                return true
            }
        }
        return false
    }

    interface DrawableTarget {
        fun onResourceReady(resource: Drawable)
    }

}