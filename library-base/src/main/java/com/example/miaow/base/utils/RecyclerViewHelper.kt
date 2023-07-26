package com.example.miaow.base.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.toppingToPosition(position: Int) {
    if (layoutManager == null || layoutManager !is LinearLayoutManager) return
    val lm = layoutManager as LinearLayoutManager
    val firstItemPosition = lm.findFirstVisibleItemPosition()
    val lastItemPosition = lm.findLastVisibleItemPosition()
    when {
        position <= firstItemPosition -> smoothScrollToPosition(position)
        position <= lastItemPosition -> {
            val childView = getChildAt(position - firstItemPosition)
            smoothScrollBy(0, childView.top)
        }
        else -> {
            smoothScrollToPosition(position)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState != RecyclerView.SCROLL_STATE_IDLE) return
                    removeOnScrollListener(this)
                    toppingToPosition(position)
                }
            })
        }
    }
}