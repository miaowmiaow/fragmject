package com.example.miaow.picture.selector.bean

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Media(
    val name: String,
    val uri: Uri,
    var selected: Boolean = false
) : Parcelable