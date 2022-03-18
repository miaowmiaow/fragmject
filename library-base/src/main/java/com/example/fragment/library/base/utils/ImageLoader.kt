package com.example.fragment.library.base.utils

import android.widget.ImageView
import coil.loadAny
import coil.transform.BlurTransformation
import coil.transform.CircleCropTransformation
import coil.transform.GrayscaleTransformation
import coil.transform.RoundedCornersTransformation

fun ImageView.load(data: Any, placeholderId: Int = 0, errorId: Int = 0) {
    this.loadAny(data) {
        crossfade(true)
        placeholder(placeholderId)
        error(errorId)
    }
}

fun ImageView.loadBlur(data: Any, placeholderId: Int = 0, errorId: Int = 0) {
    this.loadAny(data) {
        crossfade(true)
        placeholder(placeholderId)
        error(errorId)
        transformations(BlurTransformation(this@loadBlur.context))
    }
}

fun ImageView.loadCircleCrop(data: Any, placeholderId: Int = 0, errorId: Int = 0) {
    this.loadAny(data) {
        crossfade(true)
        placeholder(placeholderId)
        error(errorId)
        transformations(CircleCropTransformation())
    }
}

fun ImageView.loadGrayscale(data: Any, placeholderId: Int, errorId: Int) {
    this.loadAny(data) {
        crossfade(true)
        placeholder(placeholderId)
        error(errorId)
        transformations(GrayscaleTransformation())
    }
}

fun ImageView.loadRoundedCorners(data: Any, radius: Float) {
    this.loadRoundedCorners(data, radius, radius, radius, radius, 0, 0)
}

fun ImageView.loadRoundedCorners(
    data: Any,
    topLeft: Float,
    topRight: Float,
    bottomLeft: Float,
    bottomRight: Float
) {
    this.loadRoundedCorners(data, topLeft, topRight, bottomLeft, bottomRight, 0, 0)
}

fun ImageView.loadRoundedCorners(
    data: Any,
    topLeft: Float,
    topRight: Float,
    bottomLeft: Float,
    bottomRight: Float,
    placeholderId: Int = 0,
    errorId: Int = 0
) {
    this.loadAny(data) {
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
