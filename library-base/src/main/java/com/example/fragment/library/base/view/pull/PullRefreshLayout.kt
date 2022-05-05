package com.example.fragment.library.base.view.pull

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.Transformation
import android.widget.ImageView
import android.widget.ListView
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.core.widget.ListViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fragment.library.base.R

class PullRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), NestedScrollingParent3, NestedScrollingChild3 {

    companion object {
        private const val DEFAULT_MAX_DRAG_DISTANCE = 250
        private const val DECELERATE_INTERPOLATION_FACTOR = 2f
        private const val ANIMATE_DURATION = 250L
        private const val DRAG_RATE = .5f
    }

    private val parentHelper = NestedScrollingParentHelper(this)
    private val childHelper = NestedScrollingChildHelper(this)
    private val scrollConsumed = IntArray(2)

    private var refreshView = ImageView(context)
    private val refreshDrawable = RefreshDrawable(this)
    private lateinit var targetView: View
    private var targetViewOffset = 0

    private var refreshing = false
    private var loading = false
    private var loadMore = false
    private var loadMoreAdapter: LoadMoreAdapter? = null
    var refreshListener: OnRefreshListener? = null
    var loadMoreListener: OnLoadMoreListener? = null

    init {
        setWillNotDraw(false)
        isChildrenDrawingOrderEnabled = true
        isNestedScrollingEnabled = true
    }

    fun getMaxDragDistance(): Int {
        return DEFAULT_MAX_DRAG_DISTANCE
    }

    fun isRefresh(): Boolean {
        return refreshing
    }

    fun setRefreshing() {
        if (!isRefresh()) {
            refreshViewOffsetToEnd()
        }
    }

    fun finishRefresh() {
        if (isRefresh()) {
            refreshing = false
            setLoadMore(true)
            refreshDrawable.stop()
            refreshViewOffsetToStart()
        }
    }

    fun setOnRefreshListener(listener: OnRefreshListener) {
        refreshListener = listener
    }

    fun isLoading(): Boolean {
        return loading
    }

    fun canLoadMore(): Boolean {
        return loadMore
    }

    fun setLoadMore(b: Boolean) {
        loading = b
        loadMore = b
        loadMoreAdapter?.apply {
            notifyItemChanged(this.itemCount - 1)
        }
    }

    fun finishLoadMore() {
        loadMore = false
    }

    fun setOnLoadMoreListener(
        recyclerView: RecyclerView,
        listener: OnLoadMoreListener
    ) {
        val adapter = recyclerView.adapter
        if (adapter != null) {
            loadMoreAdapter = LoadMoreAdapter(this, adapter)
            recyclerView.adapter = loadMoreAdapter
            loadMoreListener = listener
        } else {
            throw IllegalStateException("PullRefreshLayout attached before RecyclerView has an adapter")
        }
    }

    private fun canChildScrollUp(): Boolean {
        return if (targetView is ListView) {
            ListViewCompat.canScrollList((targetView as ListView), -1)
        } else targetView.canScrollVertically(-1)
    }

    private fun targetViewOffsetTopAndBottom(offset: Int) {
        val y = when {
            targetViewOffset + offset > getMaxDragDistance() -> {
                getMaxDragDistance() - targetViewOffset
            }
            targetViewOffset + offset < 0 -> {
                -targetViewOffset
            }
            else -> {
                offset
            }
        }
        refreshDrawable.offsetTopAndBottom(y)
        ViewCompat.offsetTopAndBottom(refreshView, y)
        ViewCompat.offsetTopAndBottom(targetView, y)
        targetViewOffset = targetView.top
    }

    private fun refreshViewToStart(percent: Float) {
        refreshDrawable.invalidate()
        val offset = targetViewOffset - (targetViewOffset * percent).toInt()
        targetViewOffsetTopAndBottom(offset - targetViewOffset)
    }

    private fun refreshViewToEnd(percent: Float) {
        refreshDrawable.invalidate()
        val offset = (getMaxDragDistance() * percent).toInt()
        targetViewOffsetTopAndBottom(offset - targetViewOffset)
    }

    private fun refreshViewMoveSpinner(offset: Int) {
        refreshDrawable.invalidate()
        targetViewOffsetTopAndBottom(offset)
    }

    private fun refreshViewFinishSpinner(offset: Float) {
        if (offset >= getMaxDragDistance() && !isRefresh()) {
            refreshing = true
            refreshDrawable.start()
            refreshListener?.onRefresh(this)
        } else {
            refreshing = false
            refreshViewOffsetToStart()
        }
    }

    private fun refreshViewOffsetToStart() {
        refreshView.clearAnimation()
        refreshView.startAnimation(object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                refreshViewToStart(interpolatedTime)
            }
        }.apply {
            reset()
            duration = ANIMATE_DURATION
            interpolator = DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR)
        })
    }

    private fun refreshViewOffsetToEnd() {
        refreshView.clearAnimation()
        refreshView.startAnimation(object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                refreshViewToEnd(interpolatedTime)
                if (interpolatedTime >= DRAG_RATE && !isRefresh()) {
                    refreshing = true
                    refreshDrawable.start()
                    refreshListener?.onRefresh(this@PullRefreshLayout)
                }
            }
        }.apply {
            reset()
            duration = ANIMATE_DURATION
            interpolator = DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR)
        })
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount > 0) {
            targetView = getChildAt(0)
        }
        refreshView.setBackgroundColor(ContextCompat.getColor(context, R.color.background))
        refreshView.setImageDrawable(refreshDrawable)
        addView(refreshView)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val childWidth = measuredWidth - paddingLeft - paddingRight
        val childHeight = measuredHeight - paddingTop - paddingBottom
        targetView.layout(
            paddingLeft,
            paddingTop + targetViewOffset,
            paddingLeft + childWidth,
            paddingTop + childHeight + targetViewOffset
        )
        refreshView.layout(
            paddingLeft,
            paddingTop - getMaxDragDistance() + targetViewOffset,
            paddingLeft + childWidth,
            paddingTop + targetViewOffset
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val viewWidth = measuredWidth - paddingLeft - paddingRight
        val viewHeight = measuredHeight - paddingTop - paddingBottom
        targetView.measure(
            MeasureSpec.makeMeasureSpec(viewWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(viewHeight, MeasureSpec.EXACTLY)
        )
        refreshView.measure(
            MeasureSpec.makeMeasureSpec(viewWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(getMaxDragDistance(), MeasureSpec.EXACTLY)
        )
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        childHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return childHelper.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return childHelper.startNestedScroll(axes, type)
    }

    override fun stopNestedScroll(type: Int) {
        childHelper.stopNestedScroll(type)
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        return childHelper.hasNestedScrollingParent(type)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int,
        consumed: IntArray
    ) {
        childHelper.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow,
            type,
            consumed
        )
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return childHelper.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow,
            type
        )
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return type == ViewCompat.TYPE_TOUCH && axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        parentHelper.onNestedScrollAccepted(child, target, axes, type)
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, type)
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        parentHelper.onStopNestedScroll(target, type)
        stopNestedScroll(type)
        refreshViewFinishSpinner(targetViewOffset.toFloat())
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        onNestedScrollInternal(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed)
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        onNestedScrollInternal(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            type,
            scrollConsumed
        )
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (!dispatchNestedPreScroll(dx, dy, consumed, null, type)) {
            if (dy > 0 && targetViewOffset > 0) {
                refreshViewMoveSpinner(-dy)
                consumed[1] = if (dy > targetViewOffset) targetViewOffset else dy
            }
        }
    }

    private fun onNestedScrollInternal(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        var myDyConsumed = 0
        if (type == ViewCompat.TYPE_TOUCH && !canChildScrollUp()) {
            refreshViewMoveSpinner(-dyUnconsumed)
            myDyConsumed = if (targetViewOffset + dyUnconsumed >= getMaxDragDistance()) {
                getMaxDragDistance() - targetViewOffset
            } else {
                dyUnconsumed
            }
            consumed[1] += myDyConsumed
        }
        childHelper.dispatchNestedScroll(
            dxConsumed,
            dyConsumed + myDyConsumed,
            dxUnconsumed,
            dyUnconsumed - myDyConsumed,
            null,
            type,
            consumed
        )
    }

}

interface OnRefreshListener {
    fun onRefresh(refreshLayout: PullRefreshLayout)
}

interface OnLoadMoreListener {
    fun onLoadMore(refreshLayout: PullRefreshLayout)
}