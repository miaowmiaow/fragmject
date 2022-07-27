package com.example.fragment.library.base.utils

import android.graphics.Outline
import android.graphics.Rect
import android.view.View
import android.view.ViewOutlineProvider

class RoundViewOutlineProvider(val radius: Float) : ViewOutlineProvider() {

    override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(Rect(0, 0, view.width, view.height), radius)
    }

}

class LeftRoundViewOutlineProvider(val radius: Float) : ViewOutlineProvider() {

    override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(Rect(-radius.toInt(), 0, view.width, view.height), radius)
    }

}

class TopRoundViewOutlineProvider(val radius: Float) : ViewOutlineProvider() {

    override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(Rect(0, 0, view.width, view.height + radius.toInt()), radius)
    }

}

class RightRoundViewOutlineProvider(val radius: Float) : ViewOutlineProvider() {

    override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(Rect(0, 0, view.width + radius.toInt(), view.height), radius)
    }

}

class BottomRoundViewOutlineProvider(val radius: Float) : ViewOutlineProvider() {

    override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(Rect(0, -radius.toInt(), view.width, view.height), radius)
    }

}