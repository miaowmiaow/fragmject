package com.example.fragment.library.base.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.io.ByteArrayOutputStream


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

        fun submit(): ByteArray? {
            return imageLoader.submit()
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

    fun submit(): ByteArray? {
        context?.apply {
            if (path.isBlank()) {
                return null
            }
            if (viewDestroyed(this)) {
                return null
            }
            val bitmap = Glide.with(this).asBitmap().load(path).submit().get()
            try {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                return baos.toByteArray()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    fun into(view: ImageView) {
        context?.apply {
            if (path.isBlank()) {
                return
            }
            if (viewDestroyed(this)) {
                return
            }
            Glide.with(this).load(path).into(view)
        }
    }

    fun into(target: DrawableTarget) {
        context?.apply {
            if (path.isBlank()) {
                return
            }
            if (viewDestroyed(this)) {
                return
            }
            val requestBuilder = Glide.with(this).asDrawable().load(path)
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