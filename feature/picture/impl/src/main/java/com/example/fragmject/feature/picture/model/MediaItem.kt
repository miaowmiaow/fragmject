package com.example.fragmject.feature.picture.model

import android.net.Uri
import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.Parcelize

@Parcelize
class MediaItem(
    val name: String,
    var uri: Uri,
    var width: Int = 0,
    var height: Int = 0,
    var mimeType: String = "*/*",
) : Cloneable, Parcelable {

    /**
     * 长图
     */
    fun longImage(): Boolean {
        val h = width * 3
        return height > h
    }

    /**
     * gif图
     */
    fun gifImage(): Boolean {
        return when (mimeType) {
            "image/gif", "image/GIF" -> true
            else -> false
        }
    }

    public override fun clone(): MediaItem {
        return try {
            super.clone() as MediaItem
        } catch (e: CloneNotSupportedException) {
            Log.e("MediaItem", "clone failed: $name", e)
            MediaItem("EMPTY", Uri.EMPTY)
        }
    }

}