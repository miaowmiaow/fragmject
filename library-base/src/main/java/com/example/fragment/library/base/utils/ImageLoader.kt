package com.example.fragment.library.base.utils

import android.widget.ImageView
import coil.clear
import coil.load
import coil.transform.BlurTransformation
import coil.transform.CircleCropTransformation
import coil.transform.GrayscaleTransformation
import coil.transform.RoundedCornersTransformation
import java.io.File

object ImageLoader {

    fun load(imageView: ImageView, file: File) {
        load(imageView, file, 0, 0)
    }

    fun load(imageView: ImageView, file: File, placeholderId: Int, errorId: Int) {
        imageView.load(file) {
            crossfade(true)
            placeholder(placeholderId)
            error(errorId)
        }
    }

    fun load(imageView: ImageView, id: Int) {
        load(imageView, id, 0, 0)
    }

    fun load(imageView: ImageView, id: Int, placeholderId: Int, errorId: Int) {
        imageView.load(id) {
            crossfade(true)
            placeholder(placeholderId)
            error(errorId)
        }
    }

    fun load(imageView: ImageView, url: String) {
        load(imageView, url, 0, 0)
    }

    fun load(imageView: ImageView, url: String, placeholderId: Int, errorId: Int) {
        imageView.load(url) {
            crossfade(true)
            placeholder(placeholderId)
            error(errorId)
        }
    }

    fun loadBlur(imageView: ImageView, url: String) {
        loadBlur(imageView, url, 0, 0)
    }

    fun loadBlur(imageView: ImageView, url: String, placeholderId: Int, errorId: Int) {
        imageView.load(url) {
            crossfade(true)
            placeholder(placeholderId)
            error(errorId)
            transformations(BlurTransformation(imageView.context))
        }
    }

    fun loadCircleCrop(imageView: ImageView, id: Int) {
        loadCircleCrop(imageView, id, 0, 0)
    }

    fun loadCircleCrop(imageView: ImageView, id: Int, placeholderId: Int, errorId: Int) {
        imageView.load(id) {
            crossfade(true)
            placeholder(placeholderId)
            error(errorId)
            transformations(CircleCropTransformation())
        }
    }

    fun loadCircleCrop(imageView: ImageView, url: String) {
        loadCircleCrop(imageView, url, 0, 0)
    }

    fun loadCircleCrop(imageView: ImageView, url: String, placeholderId: Int, errorId: Int) {
        imageView.load(url) {
            crossfade(true)
            placeholder(placeholderId)
            error(errorId)
            transformations(CircleCropTransformation())
        }
    }

    fun loadGrayscale(imageView: ImageView, url: String) {
        loadGrayscale(imageView, url, 0, 0)
    }

    fun loadGrayscale(imageView: ImageView, url: String, placeholderId: Int, errorId: Int) {
        imageView.load(url) {
            crossfade(true)
            placeholder(placeholderId)
            error(errorId)
            transformations(GrayscaleTransformation())
        }
    }

    fun loadRoundedCorners(imageView: ImageView, file: File, radius: Float) {
        loadRoundedCorners(imageView, file, radius, radius, radius, radius, 0, 0)
    }

    fun loadRoundedCorners(
        imageView: ImageView,
        file: File,
        topLeft: Float,
        topRight: Float,
        bottomLeft: Float,
        bottomRight: Float
    ) {
        loadRoundedCorners(imageView, file, topLeft, topRight, bottomLeft, bottomRight, 0, 0)
    }

    fun loadRoundedCorners(
        imageView: ImageView,
        file: File,
        topLeft: Float,
        topRight: Float,
        bottomLeft: Float,
        bottomRight: Float,
        placeholderId: Int,
        errorId: Int
    ) {
        imageView.load(file) {
            crossfade(true)
            placeholder(placeholderId)
            error(errorId)
            transformations(
                RoundedCornersTransformation(
                    topLeft,
                    topRight,
                    bottomLeft,
                    bottomRight
                )
            )
        }
    }

    fun loadRoundedCorners(imageView: ImageView, url: String, radius: Float) {
        loadRoundedCorners(imageView, url, radius, radius, radius, radius, 0, 0)
    }

    fun loadRoundedCorners(
        imageView: ImageView,
        url: String,
        topLeft: Float,
        topRight: Float,
        bottomLeft: Float,
        bottomRight: Float
    ) {
        loadRoundedCorners(imageView, url, topLeft, topRight, bottomLeft, bottomRight, 0, 0)
    }

    fun loadRoundedCorners(
        imageView: ImageView,
        url: String,
        radius: Float,
        placeholderId: Int,
        errorId: Int
    ) {
        loadRoundedCorners(imageView, url, radius, radius, radius, radius, placeholderId, errorId)
    }

    fun loadRoundedCorners(
        imageView: ImageView,
        url: String,
        topLeft: Float,
        topRight: Float,
        bottomLeft: Float,
        bottomRight: Float,
        placeholderId: Int,
        errorId: Int
    ) {
        imageView.load(url) {
            crossfade(true)
            placeholder(placeholderId)
            error(errorId)
            transformations(
                RoundedCornersTransformation(
                    topLeft,
                    topRight,
                    bottomLeft,
                    bottomRight
                )
            )
        }
    }

    fun clear(imageView: ImageView) {
        imageView.clear()
    }

}