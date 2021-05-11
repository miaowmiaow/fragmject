package com.example.fragment.library.base.utils

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class SimpleBannerHelper(
    private val recyclerView: RecyclerView,
    @RecyclerView.Orientation
    private val orientation: Int = RecyclerView.HORIZONTAL
) {

    private var repeatLayoutManager = RepeatLayoutManager(recyclerView.context)

    private var smoothScrollDuration = 500
    private var bannerDelay = 5000L
    private var offsetX = 0
    private var offsetY = 0
    private var isUp = false
    private var isSettling = false
    private val timerTask = Runnable {
        recyclerView.post {
            if (repeatLayoutManager.itemCount > 1) {
                val position = repeatLayoutManager.childCount - 1
                if (orientation == RecyclerView.VERTICAL) {
                    repeatLayoutManager.getChildAt(position)?.let { view ->
                        val dy = view.height
                        recyclerView.smoothScrollBy(0, dy, null, smoothScrollDuration)
                    }
                } else if (orientation == RecyclerView.HORIZONTAL) {
                    repeatLayoutManager.getChildAt(position)?.let { view ->
                        val dx = view.width
                        recyclerView.smoothScrollBy(dx, 0, null, smoothScrollDuration)
                    }
                }
                startTimerTask()
            }
        }
    }

    init {
        repeatLayoutManager.orientation = orientation
        recyclerView.layoutManager = repeatLayoutManager
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isSettling = false
                    if (isUp) {
                        isUp = false
                        offsetItem()
                        startTimerTask()
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isUp = true
                    if (!isSettling) {
                        offsetX = 0
                        offsetY = 0
                    }
                    stopTimerTask()
                } else if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    isSettling = true
                    stopTimerTask()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                offsetX += dx
                offsetY += dy
            }
        })
    }

    private fun offsetItem() {
        val position = repeatLayoutManager.childCount - 1
        if (orientation == RecyclerView.VERTICAL) {
            repeatLayoutManager.getChildAt(position)?.let { view ->
                val height = view.height
                val offset = offsetY % height
                if (abs(offset) >= height / 2) {
                    if (offset >= 0) {
                        recyclerView.smoothScrollBy(0, height - offset)
                    } else {
                        recyclerView.smoothScrollBy(0, abs(offset) - height)
                    }
                } else {
                    recyclerView.smoothScrollBy(0, -offset)
                }
            }
        } else if (orientation == RecyclerView.HORIZONTAL) {
            repeatLayoutManager.getChildAt(position)?.let { view ->
                val width = view.width
                val offset = offsetX % width
                if (abs(offset) >= width / 2) {
                    if (offset >= 0) {
                        recyclerView.smoothScrollBy(width - offset, 0)
                    } else {
                        recyclerView.smoothScrollBy(abs(offset) - width, 0)
                    }
                } else {
                    recyclerView.smoothScrollBy(-offset, 0)
                }
            }
        }
    }

    fun startTimerTask() {
        recyclerView.postDelayed(timerTask, bannerDelay)
    }

    fun stopTimerTask() {
        recyclerView.removeCallbacks(timerTask)
    }

}


/**
 * 修改自jiarWang的RepeatLayoutManager
 * https://github.com/jiarWang/RepeatLayoutManager
 * 并通过继承LinearLayoutManager这种取巧的方式来
 * 解决软键盘弹出或收起导致onLayoutChildren()方法被重新调用的问题
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