package com.example.fragment.library.base.component.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.Transformation
import android.widget.ImageView
import android.widget.ListView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.*
import androidx.core.widget.ListViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fragment.library.base.R
import kotlin.math.abs
import kotlin.math.pow

class SimplePullRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr),
    NestedScrollingParent3, NestedScrollingParent2, NestedScrollingParent,
    NestedScrollingChild3, NestedScrollingChild2, NestedScrollingChild {

    companion object {
        private const val ANIMATE_TO_START_DURATION = 250L
        private const val ANIMATE_TO_END_DURATION = 250L
        private const val ANIMATE_TO_TRIGGER_DURATION = 250L
        private const val DECELERATE_INTERPOLATION_FACTOR = 2f
        private const val DEFAULT_DRAG_MAX_DISTANCE = 100
        private const val DRAG_RATE = .5f
        private const val PRELOADING_NUMBER = 5
    }

    private var touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val decelerateInterpolator = DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR)

    private var refreshViewIndex = -1
    private var refreshView = ImageView(context)
    private lateinit var targetView: View

    private var density = resources.displayMetrics.density
    private var refreshViewHeight = (DEFAULT_DRAG_MAX_DISTANCE * density).toInt()
    private var currentTargetOffsetTop = 0
    private var from = 0

    private var refreshing = false
    private var loadMore = false
    private var loadMoreText = true

    private val nestedScrollingParentHelper = NestedScrollingParentHelper(this)
    private val nestedScrollingChildHelper = NestedScrollingChildHelper(this)
    private val parentScrollConsumed = IntArray(2)
    private val parentOffsetInWindow = IntArray(2)
    private val nestedScrollingV2ConsumedCompat = IntArray(2)
    private var nestedScrollInProgress = false
    private var totalUnconsumed = 0f

    private var refreshListener: OnRefreshListener? = null
    private var loadMoreListener: OnLoadMoreListener? = null
    private var loadMoreAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null

    private val refreshDrawable = object : BaseRefreshView(this) {

        private val ANIMATION_DURATION = 750L
        private val INITIAL_ROTATE_GROWTH = 7.5

        private var mScreenWidth = 0f
        private var mDx = 0f
        private var mDy = 0f

        private var mBitmapWidth = 0
        private var mBitmapHeight = 0

        private var mPercent = 0.0f

        private var mAnimation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                setPercent(interpolatedTime, true)
            }
        }

        private var m1 = BitmapFactory.decodeResource(resources, R.drawable.loading_big_1)
        private var m4 = BitmapFactory.decodeResource(resources, R.drawable.loading_big_4)
        private var m7 = BitmapFactory.decodeResource(resources, R.drawable.loading_big_7)
        private var m10 = BitmapFactory.decodeResource(resources, R.drawable.loading_big_10)
        private var m13 = BitmapFactory.decodeResource(resources, R.drawable.loading_big_13)
        private var m16 = BitmapFactory.decodeResource(resources, R.drawable.loading_big_16)
        private var m19 = BitmapFactory.decodeResource(resources, R.drawable.loading_big_19)

        private var mLoadings = listOf<Bitmap>(m1, m4, m7, m10, m13, m16, m19)

        init {
            mBitmapWidth = mLoadings[0].width
            mBitmapHeight = mLoadings[0].height

            mScreenWidth = resources.displayMetrics.widthPixels.toFloat()
            mDx = (mScreenWidth - mBitmapWidth) / 2f
            mDy = parent.getTotalDragDistance() * 1f - (mBitmapHeight / 2)

            mAnimation.repeatCount = Animation.INFINITE
            mAnimation.repeatMode = Animation.RESTART
            mAnimation.interpolator = LinearInterpolator()
            mAnimation.duration = ANIMATION_DURATION
        }

        fun setPercent(percent: Float) {
            mPercent = percent
        }

        override fun setPercent(percent: Float, invalidate: Boolean) {
            setPercent(percent)
            if (invalidate) invalidateSelf()
        }

        override fun offsetTopAndBottom(offset: Int) {
            mDy -= offset / 2f
            invalidateSelf()
        }

        override fun draw(canvas: Canvas) {
            val percent = mPercent * INITIAL_ROTATE_GROWTH
            val size = mLoadings.size - 1
            val index = (percent % size).toInt()
            canvas.drawBitmap(mLoadings[index], mDx, mDy, null)
        }

        override fun start() {
            mAnimation.reset()
            parent.startAnimation(mAnimation)
        }

        override fun stop() {
            parent.clearAnimation()
        }

        override fun isRunning(): Boolean {
            return false
        }

    }

    interface OnRefreshListener {
        fun onRefresh(refreshLayout: SimplePullRefreshLayout)
    }

    interface OnLoadMoreListener {
        fun onLoadMore(refreshLayout: SimplePullRefreshLayout)
    }

    fun isRefresh(): Boolean {
        return refreshing
    }

    fun setRefreshing() {
        if (!refreshing) {
            animateOffsetToEndPosition(refreshViewHeight)
        }
    }

    fun finishRefresh() {
        if (refreshing) {
            refreshing = false
            setLoadMore(true)
            refreshDrawable.stop()
            animateOffsetToCorrectPosition(currentTargetOffsetTop)
        }
    }

    fun setLoadMore(isLoadMore: Boolean) {
        loadMore = isLoadMore
        loadMoreText = isLoadMore
        loadMoreAdapter?.apply {
            notifyItemRangeChanged(itemCount - 1, 1)
        }
    }

    fun setOnRefreshListener(listener: OnRefreshListener) {
        refreshListener = listener
    }

    fun setOnLoadMoreListener(
        recyclerView: RecyclerView,
        listener: OnLoadMoreListener
    ) {
        recyclerView.adapter?.let { adapter ->
            loadMoreListener = listener
            loadMoreAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

                private val TYPE_LOAD_MORE = 201225

                init {
                    adapter.registerAdapterDataObserver(object :
                        RecyclerView.AdapterDataObserver() {

                        override fun onChanged() {
                            super.onChanged()
                            notifyDataSetChanged()
                        }

                        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                            super.onItemRangeChanged(positionStart, itemCount)
                            notifyItemRangeChanged(positionStart, itemCount)
                        }

                        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                            super.onItemRangeInserted(positionStart, itemCount)
                            notifyItemRangeInserted(positionStart, itemCount)
                        }

                        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                            super.onItemRangeRemoved(positionStart, itemCount)
                            notifyItemRangeRemoved(positionStart, itemCount)
                        }

                    })
                }

                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int
                ): RecyclerView.ViewHolder {
                    return if (viewType == TYPE_LOAD_MORE) {
                        val loadMoreView = LoadMoreView(parent.context)
                        loadMoreView.id = TYPE_LOAD_MORE
                        loadMoreView.width = parent.width
                        LoadMoreViewHolder(loadMoreView)
                    } else
                        adapter.onCreateViewHolder(parent, viewType)
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    if (getItemViewType(position) == TYPE_LOAD_MORE) {
                        val loadView = holder.itemView.findViewById<LoadMoreView>(TYPE_LOAD_MORE)
                        if (itemCount > 1) {
                            loadView.setLoadMore(loadMoreText)
                        }
                    } else {
                        adapter.onBindViewHolder(holder, position)
                    }
                    if (itemCount > 1 && position >= itemCount - PRELOADING_NUMBER && loadMore) {
                        loadMore = false
                        recyclerView.post {
                            loadMoreListener?.onLoadMore(this@SimplePullRefreshLayout)
                        }
                    }
                }

                override fun getItemCount(): Int {
                    return adapter.itemCount + 1
                }

                override fun getItemViewType(position: Int): Int {
                    return if (position == itemCount - 1)
                        TYPE_LOAD_MORE
                    else
                        adapter.getItemViewType(position)
                }

            }
        }
        recyclerView.adapter = loadMoreAdapter
        setLoadMore(true)
    }

    class LoadMoreViewHolder(view: View) : RecyclerView.ViewHolder(view)

    init {
        setWillNotDraw(false)
        isChildrenDrawingOrderEnabled = true
        isNestedScrollingEnabled = true
        isEnabled = true
    }

    fun getTotalDragDistance(): Int {
        return refreshViewHeight
    }

    private fun animateOffsetToCorrectPosition(f: Int) {
        from = f
        mAnimateToCorrectPosition.reset()
        mAnimateToCorrectPosition.duration = ANIMATE_TO_TRIGGER_DURATION
        mAnimateToCorrectPosition.interpolator = decelerateInterpolator
        refreshView.clearAnimation()
        refreshView.startAnimation(mAnimateToCorrectPosition)
    }

    private fun animateOffsetToStartPosition(f: Int) {
        from = f
        mAnimateToStartPosition.reset()
        mAnimateToStartPosition.duration = ANIMATE_TO_START_DURATION
        mAnimateToStartPosition.interpolator = decelerateInterpolator
        refreshView.clearAnimation()
        refreshView.startAnimation(mAnimateToStartPosition)
    }

    private fun animateOffsetToEndPosition(f: Int) {
        from = f
        mAnimateToEndPosition.reset()
        mAnimateToEndPosition.duration = ANIMATE_TO_END_DURATION
        mAnimateToEndPosition.interpolator = decelerateInterpolator
        refreshView.clearAnimation()
        refreshView.startAnimation(mAnimateToEndPosition)
    }

    private val mAnimateToCorrectPosition: Animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            refreshDrawable.setPercent(interpolatedTime, true)
            val targetTop = from - (from * interpolatedTime).toInt()
            val offset = targetTop - targetView.top
            setTargetOffsetTopAndBottom(offset)
        }
    }

    private val mAnimateToStartPosition: Animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            moveToStart(interpolatedTime)
        }
    }

    private val mAnimateToEndPosition: Animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            moveToEnd(interpolatedTime)
            if (interpolatedTime >= DRAG_RATE && !refreshing) {
                refreshing = true
                refreshDrawable.start()
                refreshListener?.let {
                    it.onRefresh(this@SimplePullRefreshLayout)
                    loadMore = true
                }
            }
        }
    }

    private fun moveToStart(interpolatedTime: Float) {
        refreshDrawable.setPercent(interpolatedTime, true)
        val targetTop = from - (from * interpolatedTime).toInt()
        val offset = targetTop - targetView.top
        setTargetOffsetTopAndBottom(offset)
    }

    private fun moveToEnd(interpolatedTime: Float) {
        val targetTop = (from * interpolatedTime).toInt()
        val offset = targetTop - targetView.top
        setTargetOffsetTopAndBottom(offset)
    }

    private fun setTargetOffsetTopAndBottom(offset: Int) {
        refreshDrawable.offsetTopAndBottom(offset)
        ViewCompat.offsetTopAndBottom(refreshView, offset)
        ViewCompat.offsetTopAndBottom(targetView, offset)
        currentTargetOffsetTop = targetView.top
    }

    private fun canChildScrollUp(): Boolean {
        return if (targetView is ListView) {
            ListViewCompat.canScrollList((targetView as ListView), -1)
        } else targetView.canScrollVertically(-1)
    }

    override fun getChildDrawingOrder(childCount: Int, i: Int): Int {
        return when {
            refreshViewIndex < 0 -> {
                i
            }
            i == childCount - 1 -> {
                refreshViewIndex
            }
            i >= refreshViewIndex -> {
                i + 1
            }
            else -> {
                i
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child != refreshView) {
                targetView = child
                break
            }
        }
        addView(refreshView)
        refreshView.setBackgroundColor(Color.parseColor("#F0F0F0"))
        refreshView.setImageDrawable(refreshDrawable)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = measuredWidth
        val height = measuredHeight
        val childLeft = paddingLeft
        val childTop = paddingTop
        val childWidth = width - paddingLeft - paddingRight
        val childHeight = height - paddingTop - paddingBottom
        targetView.layout(
            childLeft,
            childTop + currentTargetOffsetTop,
            childLeft + childWidth,
            childTop + childHeight + currentTargetOffsetTop
        )
        refreshView.layout(
            childLeft,
            childTop - refreshViewHeight + currentTargetOffsetTop,
            childLeft + childWidth,
            childTop + currentTargetOffsetTop
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
            MeasureSpec.makeMeasureSpec(refreshViewHeight, MeasureSpec.EXACTLY)
        )
        refreshViewIndex = -1
        for (index in 0 until childCount) {
            if (getChildAt(index) == refreshView) {
                refreshViewIndex = index
                break
            }
        }
    }

    private fun moveSpinner(overScrollTop: Float) {
        val originalDragPercent = overScrollTop / refreshViewHeight
        val dragPercent = 1f.coerceAtMost(abs(originalDragPercent))
        refreshDrawable.setPercent(originalDragPercent, true)
        val extraOS = abs(overScrollTop) - refreshViewHeight
        val slingshotDist = refreshViewHeight
        val tensionSlingshotPercent =
            0f.coerceAtLeast(extraOS.coerceAtMost(slingshotDist * 2f) / slingshotDist)
        val tensionPercent =
            (tensionSlingshotPercent / 4 - (tensionSlingshotPercent / 4).pow(2)) * 2
        val extraMove = slingshotDist * tensionPercent * 2
        val targetY = (slingshotDist * dragPercent + extraMove).toInt() / 2
        setTargetOffsetTopAndBottom(targetY - currentTargetOffsetTop)
    }

    private fun finishSpinner(overScrollTop: Float) {
        if (overScrollTop > refreshViewHeight * 2 && !refreshing) {
            refreshing = true
            refreshDrawable.start()
            refreshListener?.let {
                it.onRefresh(this)
                loadMore = true
            }
        } else {
            refreshing = false
            animateOffsetToStartPosition(currentTargetOffsetTop)
        }
    }

    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
        return (isEnabled && nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL != 0)
    }

    /**
     * 开始NestedScroll时调用，判断父View是否接受嵌套滑动
     *
     * @param child            嵌套滑动对应的父类的子类(因为嵌套滑动对于的父View不一定是一级就能找到的，可能挑了两级父View的父View，child的辈分>=target)
     * @param target           具体嵌套滑动的那个子类
     * @param axes             支持嵌套滚动轴。水平方向，垂直方向，或者不指定
     * @param type             滑动类型，ViewCompat.TYPE_NON_TOUCH fling 效果ViewCompat.TYPE_TOUCH 手势滑动
     */
    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return if (type == ViewCompat.TYPE_TOUCH) {
            onStartNestedScroll(child, target, axes)
        } else {
            false
        }
    }

    /**
     * 当onStartNestedScroll返回为true时，也就是父控件接受嵌套滑动时，该方法才会调用
     */
    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        nestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes)
        startNestedScroll(axes and ViewCompat.SCROLL_AXIS_VERTICAL)
        totalUnconsumed = 0f
        nestedScrollInProgress = true
    }

    /**
     * 在onStartNestedScroll之后调用，参数意义同上
     */
    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        if (type == ViewCompat.TYPE_TOUCH) {
            onNestedScrollAccepted(child, target, axes)
        }
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        if (dy > 0) {
            if (totalUnconsumed > 0) {
                if (dy > totalUnconsumed) {
                    consumed[1] = totalUnconsumed.toInt()
                    totalUnconsumed = 0f
                } else {
                    totalUnconsumed -= dy.toFloat()
                    consumed[1] = dy
                }
            } else {
                if (currentTargetOffsetTop > 0) {
                    totalUnconsumed = (currentTargetOffsetTop * 2).toFloat()
                    totalUnconsumed -= dy.toFloat()
                    consumed[1] = -dy
                }
            }
            moveSpinner(totalUnconsumed)
        }
        val parentConsumed = parentScrollConsumed
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0]
            consumed[1] += parentConsumed[1]
        }
    }

    /**
     * 在嵌套滑动的子View未滑动之前，判断父view是否优先与子view处理(也就是父view可以先消耗，然后给子view消耗）
     *
     * @param target   具体嵌套滑动的那个子类
     * @param dx       水平方向嵌套滑动的子View想要变化的距离
     * @param dy       垂直方向嵌套滑动的子View想要变化的距离 dy<0向下滑动 dy>0 向上滑动
     * @param consumed 这个参数要我们在实现这个函数的时候指定，回头告诉子View当前父View消耗的距离
     *                 consumed[0] 水平消耗的距离，consumed[1] 垂直消耗的距离 好让子view做出相应的调整
     * @param type     滑动类型，ViewCompat.TYPE_NON_TOUCH fling 效果ViewCompat.TYPE_TOUCH 手势滑动
     */
    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (type == ViewCompat.TYPE_TOUCH) {
            onNestedPreScroll(target, dx, dy, consumed)
        }
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int
    ) {
        onNestedScroll(
            target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
            ViewCompat.TYPE_TOUCH, nestedScrollingV2ConsumedCompat
        )
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, type: Int
    ) {
        onNestedScroll(
            target,
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            type,
            nestedScrollingV2ConsumedCompat
        )
    }

    /**
     * 嵌套滑动的子View在滑动之后，判断父view是否继续处理（也就是父消耗一定距离后，子再消耗，最后判断父消耗不）
     *
     * @param target       具体嵌套滑动的那个子类
     * @param dxConsumed   水平方向嵌套滑动的子View滑动的距离(消耗的距离)
     * @param dyConsumed   垂直方向嵌套滑动的子View滑动的距离(消耗的距离)
     * @param dxUnconsumed 水平方向嵌套滑动的子View未滑动的距离(未消耗的距离)
     * @param dyUnconsumed 垂直方向嵌套滑动的子View未滑动的距离(未消耗的距离)
     * @param type         滑动类型，ViewCompat.TYPE_NON_TOUCH fling 效果ViewCompat.TYPE_TOUCH 手势滑动
     * @param consumed     这个参数要我们在实现这个函数的时候指定，回头告诉子View当前父View消耗的距离
     *                     consumed[0] 水平消耗的距离，consumed[1] 垂直消耗的距离 好让子view做出相应的调整
     */
    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int, @ViewCompat.NestedScrollType type: Int,
        consumed: IntArray
    ) {
        if (type != ViewCompat.TYPE_TOUCH) {
            return
        }
        val consumedBeforeParents = consumed[1]
        dispatchNestedScroll(
            dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
            parentOffsetInWindow, type, consumed
        )
        val consumedByParents = consumed[1] - consumedBeforeParents
        val unconsumedAfterParents = dyUnconsumed - consumedByParents
        val remainingDistanceToScroll = if (unconsumedAfterParents == 0) {
            dyUnconsumed + parentOffsetInWindow[1]
        } else {
            unconsumedAfterParents
        }
        if (remainingDistanceToScroll < 0 && !canChildScrollUp()) {
            totalUnconsumed += abs(remainingDistanceToScroll).toFloat()
            moveSpinner(totalUnconsumed)
            consumed[1] += unconsumedAfterParents
        }
    }

    override fun onStopNestedScroll(target: View) {
        nestedScrollingParentHelper.onStopNestedScroll(target)
        nestedScrollInProgress = false
        if (totalUnconsumed > 0) {
            finishSpinner(totalUnconsumed)
            totalUnconsumed = 0f
        }
        stopNestedScroll()
    }

    /**
     * 嵌套滑动结束
     */
    override fun onStopNestedScroll(target: View, type: Int) {
        if (type == ViewCompat.TYPE_TOUCH) {
            onStopNestedScroll(target)
        }
    }

    override fun onNestedPreFling(
        target: View, velocityX: Float,
        velocityY: Float
    ): Boolean {
        return dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun onNestedFling(
        target: View, velocityX: Float, velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return nestedScrollingChildHelper.startNestedScroll(axes)
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return type == ViewCompat.TYPE_TOUCH && startNestedScroll(axes)
    }

    override fun stopNestedScroll() {
        nestedScrollingChildHelper.stopNestedScroll()
    }

    override fun stopNestedScroll(type: Int) {
        if (type == ViewCompat.TYPE_TOUCH) {
            stopNestedScroll()
        }
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean {
        return nestedScrollingChildHelper.dispatchNestedPreScroll(
            dx, dy, consumed, offsetInWindow
        )
    }

    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return type == ViewCompat.TYPE_TOUCH && dispatchNestedPreScroll(
            dx, dy, consumed,
            offsetInWindow
        )
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, offsetInWindow: IntArray?
    ): Boolean {
        return nestedScrollingChildHelper.dispatchNestedScroll(
            dxConsumed, dyConsumed,
            dxUnconsumed, dyUnconsumed, offsetInWindow
        )
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, offsetInWindow: IntArray?, type: Int
    ): Boolean {
        return type == ViewCompat.TYPE_TOUCH && nestedScrollingChildHelper.dispatchNestedScroll(
            dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type
        )
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, offsetInWindow: IntArray?, @ViewCompat.NestedScrollType type: Int,
        consumed: IntArray
    ) {
        if (type == ViewCompat.TYPE_TOUCH) {
            nestedScrollingChildHelper.dispatchNestedScroll(
                dxConsumed, dyConsumed, dxUnconsumed,
                dyUnconsumed, offsetInWindow, type, consumed
            )
        }
    }

    override fun hasNestedScrollingParent(): Boolean {
        return nestedScrollingChildHelper.hasNestedScrollingParent()
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        return type == ViewCompat.TYPE_TOUCH && hasNestedScrollingParent()
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        nestedScrollingChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return nestedScrollingChildHelper.isNestedScrollingEnabled
    }

    override fun getNestedScrollAxes(): Int {
        return nestedScrollingParentHelper.nestedScrollAxes
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return nestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return nestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }
}

abstract class BaseRefreshView(val parent: SimplePullRefreshLayout) : Drawable(), Animatable {

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    abstract fun setPercent(percent: Float, invalidate: Boolean)

    abstract fun offsetTopAndBottom(offset: Int)
}

class LoadMoreView(context: Context) : AppCompatTextView(context) {

    init {
        setPadding(20, 20, 20, 20)
        gravity = Gravity.CENTER
    }

    fun setLoadMore(isLoadMore: Boolean) {
        text = if (isLoadMore) "正在加载..." else "没有更多了。"
    }
}

