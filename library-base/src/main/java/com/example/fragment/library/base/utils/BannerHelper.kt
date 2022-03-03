package com.example.fragment.library.base.utils

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BannerHelper(
    private val recyclerView: RecyclerView,
    @RecyclerView.Orientation
    private val orientation: Int = RecyclerView.HORIZONTAL
) {

    private var layoutManager = RepeatLayoutManager(recyclerView.context)
    private var isDragging = false
    private var bannerDelay = 5000L
    private val bannerTask = Runnable {
        recyclerView.post {
            offsetItem()
            startTimerTask()
        }
    }

    init {
        layoutManager.orientation = orientation
        recyclerView.layoutManager = layoutManager
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (isDragging) {
                        isDragging = false
                        offsetItem()
                        startTimerTask()
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isDragging = true
                    stopTimerTask()
                } else if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    stopTimerTask()
                }
            }
        })
    }

    private fun offsetItem() {
        if (layoutManager.itemCount > 1) {
            val position = layoutManager.findFirstVisibleItemPosition()
            if (orientation == RecyclerView.VERTICAL) {
                layoutManager.findViewByPosition(position)?.let {
                    val height = it.height
                    val bottom = it.bottom
                    if (bottom > height / 2 && bottom < height) {
                        recyclerView.smoothScrollBy(0, bottom - height)
                    } else {
                        recyclerView.smoothScrollBy(0, bottom)
                    }
                    recyclerView.smoothScrollBy(0, it.bottom)
                }
            } else if (orientation == RecyclerView.HORIZONTAL) {
                layoutManager.findViewByPosition(position)?.let {
                    val width = it.width
                    val right = it.right
                    if (right > width / 2 && right < width) {
                        recyclerView.smoothScrollBy(right - width, 0)
                    } else {
                        recyclerView.smoothScrollBy(right, 0)
                    }
                }
            }
        }
    }

    fun findItemPosition(): Int {
        return layoutManager.findLastVisibleItemPosition()
    }

    fun startTimerTask() {
        stopTimerTask()
        recyclerView.postDelayed(bannerTask, bannerDelay)
    }

    fun stopTimerTask() {
        recyclerView.removeCallbacks(bannerTask)
    }

}

/**
 * 修改自jiarWang的RepeatLayoutManager
 * https://github.com/jiarWang/RepeatLayoutManager
 */
open class RepeatLayoutManager(val context: Context) : LinearLayoutManager(context) {

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        fillHorizontal(dx, recycler)
        offsetChildrenHorizontal(-dx)
        recyclerChildView(dx > 0, recycler)
        return dx
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        fillVertical(dy, recycler)
        offsetChildrenVertical(-dy)
        recyclerChildView(dy > 0, recycler)
        return dy
    }

    private fun fillHorizontal(dx: Int, recycler: RecyclerView.Recycler) {
        if (childCount == 0) return
        if (dx > 0) {
            getChildAt(childCount - 1)?.let {
                var anchorView = it
                val anchorPosition = getPosition(anchorView)
                while (anchorView.right < width - paddingRight + dx) {
                    var position = (anchorPosition + 1) % itemCount
                    if (position < 0) position += itemCount
                    val scrapItem = recycler.getViewForPosition(position)
                    addView(scrapItem)
                    measureChildWithMargins(scrapItem, 0, 0)
                    val left = anchorView.right
                    val top = paddingTop
                    val right = left + getDecoratedMeasuredWidth(scrapItem)
                    val bottom = top + getDecoratedMeasuredHeight(scrapItem)
                    layoutDecorated(scrapItem, left, top, right, bottom)
                    anchorView = scrapItem
                }
            }
        } else {
            getChildAt(0)?.let {
                var anchorView = it
                val anchorPosition = getPosition(anchorView)
                while (anchorView.left > paddingLeft + dx) {
                    var position = (anchorPosition - 1) % itemCount
                    if (position < 0) position += itemCount
                    val scrapItem = recycler.getViewForPosition(position)
                    addView(scrapItem, 0)
                    measureChildWithMargins(scrapItem, 0, 0)
                    val right = anchorView.left
                    val top = paddingTop
                    val left = right - getDecoratedMeasuredWidth(scrapItem)
                    val bottom = top + getDecoratedMeasuredHeight(scrapItem)
                    layoutDecorated(
                        scrapItem, left, top,
                        right, bottom
                    )
                    anchorView = scrapItem
                }
            }
        }
        return
    }

    private fun fillVertical(dy: Int, recycler: RecyclerView.Recycler) {
        if (childCount == 0) return
        if (dy > 0) {
            //填充尾部
            getChildAt(childCount - 1)?.let {
                var anchorView = it
                val anchorPosition = getPosition(anchorView)
                while (anchorView.bottom < height - paddingBottom + dy) {
                    var position = (anchorPosition + 1) % itemCount
                    if (position < 0) position += itemCount
                    val scrapItem = recycler.getViewForPosition(position)
                    addView(scrapItem)
                    measureChildWithMargins(scrapItem, 0, 0)
                    val left = paddingLeft
                    val top = anchorView.bottom
                    val right = left + getDecoratedMeasuredWidth(scrapItem)
                    val bottom = top + getDecoratedMeasuredHeight(scrapItem)
                    layoutDecorated(scrapItem, left, top, right, bottom)
                    anchorView = scrapItem
                }
            }
        } else {
            //填充头部
            getChildAt(0)?.let {
                var anchorView = it
                val anchorPosition = getPosition(anchorView)
                while (anchorView.top > paddingTop + dy) {
                    var position = (anchorPosition - 1) % itemCount
                    if (position < 0) position += itemCount
                    val scrapItem = recycler.getViewForPosition(position)
                    addView(scrapItem, 0)
                    measureChildWithMargins(scrapItem, 0, 0)
                    val left = paddingLeft
                    val right = left + getDecoratedMeasuredWidth(scrapItem)
                    val bottom = anchorView.top
                    val top = bottom - getDecoratedMeasuredHeight(scrapItem)
                    layoutDecorated(
                        scrapItem, left, top,
                        right, bottom
                    )
                    anchorView = scrapItem
                }
            }
        }
        return
    }

    /**
     * 回收界面不可见的view
     */
    private fun recyclerChildView(
        fillEnd: Boolean,
        recycler: RecyclerView.Recycler
    ) {
        if (fillEnd) {
            //回收头部
            for (i in 0 until childCount) {
                val view = getChildAt(i) ?: return
                val needRecycler = if (orientation == RecyclerView.HORIZONTAL)
                    view.right < paddingLeft
                else
                    view.bottom < paddingTop
                if (!needRecycler) return
                removeAndRecycleView(view, recycler)
            }
        } else {
            //回收尾部
            for (i in childCount - 1 downTo 0) {
                val view = getChildAt(i) ?: return
                val needRecycler = if (orientation == RecyclerView.HORIZONTAL)
                    view.left > width - paddingRight
                else
                    view.top > height - paddingBottom
                if (!needRecycler) return
                removeAndRecycleView(view, recycler)
            }
        }
    }

}