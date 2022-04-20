package com.example.miaow.picture.selector.bean

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class MediaBean(
    val name: String,
    var uri: Uri
) : Parcelable