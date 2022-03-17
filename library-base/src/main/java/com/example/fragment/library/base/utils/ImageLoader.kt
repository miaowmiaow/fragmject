package com.example.fragment.library.base.utils

import android.widget.ImageView
import coil.load
import coil.transform.BlurTransformation
import coil.transform.CircleCropTransformation
import coil.transform.GrayscaleTransformation
import coil.transform.RoundedCornersTransformation
import java.io.File

fun ImageView.load(file: File) {
    this.load(file, 0, 0)
}

fun ImageView.load(file: File, placeholderId: Int, errorId: Int) {
    this.load(file) {
        crossfade(true)
        placeholder(placeholderId)
        error(errorId)
    }
}

fun ImageView.load(id: Int) {
    this.load(id, 0, 0)
}

fun ImageView.load(id: Int, placeholderId: Int, errorId: Int) {
    this.load(id) {
        crossfade(true)
        placeholder(placeholderId)
        error(errorId)
    }
}

fun ImageView.load(url: String) {
    this.load(url, 0, 0)
}

fun ImageView.load(url: String, placeholderId: Int, errorId: Int) {
    this.load(url) {
        crossfade(true)
        placeholder(placeholderId)
        error(errorId)
    }
}

fun ImageView.loadBlur(url: String) {
    this.loadBlur(url, 0, 0)
}

fun ImageView.loadBlur(url: String, placeholderId: Int, errorId: Int) {
    this.load(url) {
        crossfade(true)
        placeholder(placeholderId)
        error(errorId)
        transformations(BlurTransformation(this@loadBlur.context))
    }
}

fun ImageView.loadCircleCrop(id: Int) {
    this.loadCircleCrop(id, 0, 0)
}

fun ImageView.loadCircleCrop(id: Int, placeholderId: Int, errorId: Int) {
    this.load(id) {
        crossfade(true)
        placeholder(placeholderId)
        error(errorId)
        transformations(CircleCropTransformation())
    }
}

fun ImageView.loadCircleCrop(url: String) {
    this.loadCircleCrop(url, 0, 0)
}

fun ImageView.loadCircleCrop(url: String, placeholderId: Int, errorId: Int) {
    this.load(url) {
        crossfade(true)
        placeholder(placeholderId)
        error(errorId)
        transformations(CircleCropTransformation())
    }
}

fun ImageView.loadGrayscale(url: String) {
    this.loadGrayscale(url, 0, 0)
}

fun ImageView.loadGrayscale(url: String, placeholderId: Int, errorId: Int) {
    this.load(url) {
        crossfade(true)
        placeholder(placeholderId)
        error(errorId)
        transformations(GrayscaleTransformation())
    }
}

fun ImageView.loadRoundedCorners(file: File, radius: Float) {
    this.loadRoundedCorners(file, radius, radius, radius, radius, 0, 0)
}

fun ImageView.loadRoundedCorners(
    file: File,
    topLeft: Float,
    topRight: Float,
    bottomLeft: Float,
    bottomRight: Float
) {
    this.loadRoundedCorners(file, topLeft, topRight, bottomLeft, bottomRight, 0, 0)
}

fun ImageView.loadRoundedCorners(
    file: File,
    topLeft: Float,
    topRight: Float,
    bottomLeft: Float,
    bottomRight: Float,
    placeholderId: Int,
    errorId: Int
) {
    this.load(file) {
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

fun ImageView.loadRoundedCorners(url: String, radius: Float) {
    this.loadRoundedCorners(url, radius, radius, radius, radius, 0, 0)
}

fun ImageView.loadRoundedCorners(
    url: String,
    topLeft: Float,
    topRight: Float,
    bottomLeft: Float,
    bottomRight: Float
) {
    this.loadRoundedCorners(url, topLeft, topRight, bottomLeft, bottomRight, 0, 0)
}

fun ImageView.loadRoundedCorners(
    url: String,
    radius: Float,
    placeholderId: Int,
    errorId: Int
) {
    this.loadRoundedCorners(url, radius, radius, radius, radius, placeholderId, errorId)
}

fun ImageView.loadRoundedCorners(
    url: String,
    topLeft: Float,
    topRight: Float,
    bottomLeft: Float,
    bottomRight: Float,
    placeholderId: Int,
    errorId: Int
) {
    this.load(url) {
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
