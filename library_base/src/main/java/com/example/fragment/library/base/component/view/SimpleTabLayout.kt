package com.example.fragment.library.base.component.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.database.DataSetObserver
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.animation.AnimationUtils
import kotlin.math.roundToInt

class SimpleTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    companion object {
        private const val INVALID_POSITION = -1
        private const val INDICATOR_HEIGHT = 0
        private const val MIN_INDICATOR_WIDTH = 24
        private const val ANIMATION_DURATION = 300L
    }

    enum class MODE {
        FIXED, AUTO
    }

    private var onTabSelectedListener: OnTabSelectedListener? = null
    private var viewPager: ViewPager? = null

    private var currentPosition = INVALID_POSITION

    private var slidingTabIndicator = SlidingTabIndicator(context)
    private var selectedIndicatorHeight = INDICATOR_HEIGHT
    private var selectedIndicatorWidth = 0
    private var textColor: ColorStateList? = null
    private var textSize = -1f

    private var tabIndicatorFullWidth = true
    private var tabViewContentBounds = RectF()

    private var scrollAnimator: ValueAnimator

    private var pageChangeListener: CustomOnPageChangeListener? = null
    private var adapterChangeListener: CustomAdapterChangeListener? = null
    private var pagerAdapterObserver: DataSetObserver? = null
    private var pagerAdapter: PagerAdapter? = null

    private var mod = MODE.FIXED

    init {
        isHorizontalScrollBarEnabled = false
        isFillViewport = true  //使子布局match_parent属性生效

        scrollAnimator = ValueAnimator()
        scrollAnimator.interpolator = AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR
        scrollAnimator.duration = ANIMATION_DURATION
        scrollAnimator.addUpdateListener { animator ->
            scrollTo(animator.animatedValue as Int, 0)
        }
        super.addView(slidingTabIndicator)
    }

    fun addTab(tabView: View) {
        tabView.isClickable = false
        slidingTabIndicator.addView(tabView, createLayoutParamsForTabs())
        tabView.tag = slidingTabIndicator.childCount - 1
        tabView.setOnClickListener {
            val position = tabView.tag as Int
            selectTab(position)
        }
    }

    fun removeAllTabs(){
        slidingTabIndicator.removeAllViews()
    }

    fun getCurrentPosition(): Int {
        return currentPosition
    }

    fun setSelectedIndicatorColor(color: Int) {
        slidingTabIndicator.setSelectedIndicatorColor(color)
    }

    fun setSelectedIndicatorHeight(height: Int) {
        slidingTabIndicator.setSelectedIndicatorHeight(height)
    }

    fun setSelectedIndicatorWidth(width: Int) {
        this.selectedIndicatorWidth = width
    }

    fun setTextColor(color: ColorStateList) {
        textColor = color
    }

    fun setTextSize(size: Float) {
        textSize = size
    }

    fun setTabMod(tabMod: MODE) {
        mod = tabMod
    }

    fun setOnTabSelectedListener(listener: OnTabSelectedListener) {
        this.onTabSelectedListener = listener
    }

    fun setupWithViewPager(viewPager: ViewPager) {
        this.viewPager = viewPager
        adapterChangeListener?.let { listener ->
            viewPager.removeOnAdapterChangeListener(listener)
        }
        if (adapterChangeListener == null) {
            adapterChangeListener = CustomAdapterChangeListener(this)
        }
        adapterChangeListener?.let { listener ->
            viewPager.addOnAdapterChangeListener(listener)
        }
        pageChangeListener?.let { listener ->
            viewPager.removeOnPageChangeListener(listener)
        }
        if (pageChangeListener == null) {
            pageChangeListener = CustomOnPageChangeListener(this)
        }
        pageChangeListener?.let { listener ->
            viewPager.addOnPageChangeListener(listener)
            listener.reset()
        }
    }

    private fun populateFromPagerAdapter() {
        viewPager?.let { vp ->
            if (slidingTabIndicator.childCount == 0) {
                vp.adapter?.let { adapter ->
                    val adapterCount = adapter.count
                    for (i in 0 until adapterCount) {
                        val textView = TextView(context)
                        textView.gravity = Gravity.CENTER
                        textView.setPadding(10, 10, 10, 10)
                        textView.text = adapter.getPageTitle(i)
                        if (textColor != null) {
                            textView.setTextColor(textColor)
                        }
                        if (textSize != -1f) {
                            textView.textSize = textSize
                        }
                        addTab(textView)
                    }
                    if (adapterCount > 0) {
                        selectTab(vp.currentItem)
                    }
                }
            }
        }
    }

    fun setPagerAdapter(adapter: PagerAdapter?) {

        pagerAdapter?.let { pagerAdapter ->
            pagerAdapterObserver?.let { observer ->
                pagerAdapter.unregisterDataSetObserver(observer)
            }
        }

        pagerAdapter = adapter

        if (pagerAdapterObserver == null) {
            pagerAdapterObserver = CustomPagerAdapterObserver(this)
        }
        pagerAdapterObserver?.let { observer ->
            adapter?.registerDataSetObserver(observer)
        }
        populateFromPagerAdapter()
    }

    private class CustomPagerAdapterObserver(val tabLayout: SimpleTabLayout) : DataSetObserver() {
        override fun onChanged() {
            tabLayout.populateFromPagerAdapter()
        }

        override fun onInvalidated() {
            tabLayout.populateFromPagerAdapter()
        }
    }

    private class CustomAdapterChangeListener(val tabLayout: SimpleTabLayout) :
            ViewPager.OnAdapterChangeListener {
        override fun onAdapterChanged(
            viewPager: ViewPager,
            oldAdapter: PagerAdapter?,
            newAdapter: PagerAdapter?
        ) {
            if (tabLayout.viewPager == viewPager) {
                tabLayout.setPagerAdapter(newAdapter)
            }
        }
    }

    private class CustomOnPageChangeListener(val tabLayout: SimpleTabLayout) :
            ViewPager.OnPageChangeListener {

        private var previousScrollState = 0
        private var scrollState = 0

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            val updateIndicator =
                    !(scrollState == ViewPager.SCROLL_STATE_SETTLING && previousScrollState == ViewPager.SCROLL_STATE_IDLE)
            tabLayout.setScrollPosition(position, positionOffset, updateIndicator)
        }

        override fun onPageSelected(position: Int) {
            if (tabLayout.currentPosition != position && position < tabLayout.slidingTabIndicator.childCount) {
                val updateIndicator = (scrollState == ViewPager.SCROLL_STATE_IDLE
                        || (scrollState == ViewPager.SCROLL_STATE_SETTLING
                        && previousScrollState == ViewPager.SCROLL_STATE_IDLE))
                tabLayout.selectTab(position, updateIndicator)
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            previousScrollState = scrollState
            scrollState = state
        }

        fun reset() {
            scrollState = ViewPager.SCROLL_STATE_IDLE
            previousScrollState = scrollState
        }
    }

    fun getTab(position: Int): View? {
        return if (position < slidingTabIndicator.childCount) {
            slidingTabIndicator.getChildAt(position)
        } else null
    }

    fun selectTab(position: Int) {
        selectTab(position, true)
    }

    private fun selectTab(position: Int, updateIndicator: Boolean) {
        if (position != INVALID_POSITION) {
            slidingTabIndicator.getChildAt(position)?.let { tabView ->
                tabView.isSelected = true
                onTabSelectedListener?.onTabSelected(tabView, position, position == currentPosition)
            }
            viewPager?.apply {
                if (position != currentItem) {
                    currentItem = position
                }
            }
        }
        if (currentPosition != INVALID_POSITION && currentPosition != position) {
            slidingTabIndicator.getChildAt(currentPosition)?.let { currentTabView ->
                currentTabView.isSelected = false
                onTabSelectedListener?.onTabUnselected(currentTabView, currentPosition)
            }
        }
        if (currentPosition == position) {
            animateToTab(position)
        } else {
            if (updateIndicator) {
                if (currentPosition == INVALID_POSITION && position != INVALID_POSITION) {
                    setScrollPosition(position, 0f, true)
                } else {
                    animateToTab(position)
                }
            }
            currentPosition = position
        }
    }

    private fun createLayoutParamsForTabs(): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        if (mod == MODE.AUTO) {
            lp.width = 0
            lp.weight = 1f
        } else if (mod == MODE.FIXED) {
            lp.width = LinearLayout.LayoutParams.WRAP_CONTENT
            lp.weight = 0f
        }
        lp.bottomMargin = selectedIndicatorHeight
        return lp
    }

    private fun animateToTab(newPosition: Int) {
        if (newPosition == INVALID_POSITION) {
            return
        }
        if (windowToken == null
                || !ViewCompat.isLaidOut(this)
                || slidingTabIndicator.childrenNeedLayout()
        ) {
            setScrollPosition(newPosition, 0f, true)
            return
        }
        val startScrollX = scrollX
        val targetScrollX = calculateScrollXForTab(newPosition, 0f)
        if (startScrollX != targetScrollX) {
            scrollAnimator.setIntValues(startScrollX, targetScrollX)
            scrollAnimator.start()
        }
        slidingTabIndicator.animateIndicatorToPosition(newPosition, ANIMATION_DURATION)
    }

    private fun setScrollPosition(
        position: Int,
        positionOffset: Float,
        updateIndicatorPosition: Boolean
    ) {
        val roundedPosition = (position.toFloat() + positionOffset).roundToInt()
        if (roundedPosition < 0 || roundedPosition >= slidingTabIndicator.childCount) {
            return
        }
        if (updateIndicatorPosition) {
            slidingTabIndicator.setIndicatorPositionFromTabPosition(position, positionOffset)
        }
        if (scrollAnimator.isRunning) {
            scrollAnimator.cancel()
        }
        scrollTo(calculateScrollXForTab(position, positionOffset), 0)
    }

    private fun calculateScrollXForTab(position: Int, positionOffset: Float): Int {
        val selectedChild = slidingTabIndicator.getChildAt(position)
        selectedChild?.let {
            val nextChild =
                    if (position + 1 < slidingTabIndicator.childCount) slidingTabIndicator.getChildAt(
                        position + 1
                    ) else null
            val selectedWidth = selectedChild.width
            val nextWidth = nextChild?.width ?: 0

            val scrollBase = selectedChild.left + selectedWidth / 2 - width / 2
            val scrollOffset = ((selectedWidth + nextWidth) * 0.5f * positionOffset).toInt()
            return if (ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_LTR) scrollBase + scrollOffset else scrollBase - scrollOffset
        }
        return 0
    }

    private inner class SlidingTabIndicator @JvmOverloads constructor(context: Context) :
            LinearLayout(context) {

        private var selectedIndicatorPaint: Paint
        private var defaultSelectionIndicator: GradientDrawable

        private var selectedPosition = INVALID_POSITION
        private var selectionOffset = 0f

        private var indicatorLeft = -1
        private var indicatorRight = -1
        private var animationStartLeft = -1
        private var animationStartRight = -1
        private var indicatorAnimator = ValueAnimator()

        init {
            setWillNotDraw(false)
            selectedIndicatorPaint = Paint()
            defaultSelectionIndicator = GradientDrawable()
        }

        fun setSelectedIndicatorColor(color: Int) {
            if (selectedIndicatorPaint.color != color) {
                selectedIndicatorPaint.color = color
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }

        fun setSelectedIndicatorHeight(height: Int) {
            if (selectedIndicatorHeight != height) {
                selectedIndicatorHeight = height
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }

        fun childrenNeedLayout(): Boolean {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.width <= 0) {
                    return true
                }
            }
            return false
        }

        fun setIndicatorPositionFromTabPosition(position: Int, positionOffset: Float) {
            if (indicatorAnimator.isRunning) {
                indicatorAnimator.cancel()
            }
            selectedPosition = position
            selectionOffset = positionOffset
            updateIndicatorPosition()
        }

        private fun updateIndicatorPosition() {
            val selectedView = getChildAt(selectedPosition)
            var left: Int
            var right: Int
            if (selectedView != null && selectedView.width > 0) {
                left = selectedView.left
                right = selectedView.right
                if (!tabIndicatorFullWidth) {
                    calculateTabViewContentBounds(selectedView, tabViewContentBounds)
                    left = tabViewContentBounds.left.toInt()
                    right = tabViewContentBounds.right.toInt()
                }
                if (selectionOffset > 0f && selectedPosition < childCount - 1) {
                    val nextView = getChildAt(selectedPosition + 1)
                    var nextViewLeft = nextView.left
                    var nextViewRight = nextView.right
                    if (!tabIndicatorFullWidth) {
                        calculateTabViewContentBounds(nextView, tabViewContentBounds)
                        nextViewLeft = tabViewContentBounds.left.toInt()
                        nextViewRight = tabViewContentBounds.right.toInt()
                    }
                    left =
                            (selectionOffset * nextViewLeft + (1.0f - selectionOffset) * left).toInt()
                    right =
                            (selectionOffset * nextViewRight + (1.0f - selectionOffset) * right).toInt()
                }
            } else {
                left = -1
                right = -1
            }
            setIndicatorPosition(left, right)
        }

        private fun setIndicatorPosition(left: Int, right: Int) {
            if (left != indicatorLeft || right != indicatorRight) {
                indicatorLeft = left
                indicatorRight = right
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }

        fun animateIndicatorToPosition(position: Int, duration: Long) {
            if (indicatorAnimator.isRunning) {
                indicatorAnimator.cancel()
            }
            updateOrRecreateIndicatorAnimation( /* recreateAnimation= */true, position, duration)
        }

        @SuppressLint("RestrictedApi")
        private fun updateOrRecreateIndicatorAnimation(
            recreateAnimation: Boolean,
            position: Int,
            duration: Long
        ) {
            val targetView = getChildAt(position)
            if (targetView == null) {
                updateIndicatorPosition()
                return
            }
            var targetLeft = targetView.left
            var targetRight = targetView.right
            if (!tabIndicatorFullWidth) {
                calculateTabViewContentBounds(targetView, tabViewContentBounds)
                targetLeft = tabViewContentBounds.left.toInt()
                targetRight = tabViewContentBounds.right.toInt()
            }

            val finalTargetLeft = targetLeft
            val finalTargetRight = targetRight

            val startLeft = indicatorLeft
            val startRight = indicatorRight

            if (startLeft == finalTargetLeft && startRight == finalTargetRight) {
                return
            }

            if (recreateAnimation) {
                animationStartLeft = startLeft
                animationStartRight = startRight
            }

            val updateListener =
                    ValueAnimator.AnimatorUpdateListener { valueAnimator ->
                        val fraction = valueAnimator.animatedFraction
                        setIndicatorPosition(
                            AnimationUtils.lerp(animationStartLeft, finalTargetLeft, fraction),
                            AnimationUtils.lerp(animationStartRight, finalTargetRight, fraction)
                        )
                    }
            if (recreateAnimation) {
                indicatorAnimator.interpolator = AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR
                indicatorAnimator.duration = duration
                indicatorAnimator.setFloatValues(0f, 1f)
                indicatorAnimator.addUpdateListener(updateListener)
                indicatorAnimator.addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animator: Animator) {
                            selectedPosition = position
                        }

                        override fun onAnimationEnd(animator: Animator) {
                            selectedPosition = position
                            selectionOffset = 0f
                        }
                    })
                indicatorAnimator.start()
            } else {
                indicatorAnimator.removeAllUpdateListeners()
                indicatorAnimator.addUpdateListener(updateListener)
            }
        }

        private fun calculateTabViewContentBounds(tabView: View, contentBounds: RectF) {
            var tabViewContentWidth = tabView.width
            val minIndicatorWidth = MIN_INDICATOR_WIDTH
            if (tabViewContentWidth < minIndicatorWidth) {
                tabViewContentWidth = minIndicatorWidth
            }
            val tabViewCenter = (tabView.left + tabView.right) / 2
            val contentLeftBounds = tabViewCenter - tabViewContentWidth / 2
            val contentRightBounds = tabViewCenter + tabViewContentWidth / 2
            contentBounds.set(contentLeftBounds.toFloat(), 0f, contentRightBounds.toFloat(), 0f)
        }

        @Override
        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            super.onLayout(changed, l, t, r, b)
            if (indicatorAnimator.isRunning) {
                updateOrRecreateIndicatorAnimation(false, selectedPosition, -1)
            } else {
                updateIndicatorPosition()
            }
        }

        @Override
        override fun onDraw(canvas: Canvas) {
            var indicatorHeight = 0
            if (selectedIndicatorHeight >= 0) {
                indicatorHeight = selectedIndicatorHeight
            }
            val indicatorTop = height - indicatorHeight
            val indicatorBottom = height

            if (indicatorLeft in 0 until indicatorRight) {
                val selectedIndicator = DrawableCompat.wrap(defaultSelectionIndicator).mutate()
                if (selectedIndicatorWidth > 0) {
                    val indicatorCenter = (indicatorLeft + indicatorRight) / 2
                    indicatorLeft = indicatorCenter - (selectedIndicatorWidth / 2)
                    indicatorRight = indicatorCenter + (selectedIndicatorWidth / 2)
                }
                selectedIndicator.setBounds(
                    indicatorLeft,
                    indicatorTop,
                    indicatorRight,
                    indicatorBottom
                )
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                    selectedIndicator.setColorFilter(
                        selectedIndicatorPaint.color,
                        PorterDuff.Mode.SRC_IN
                    )
                } else {
                    DrawableCompat.setTint(selectedIndicator, selectedIndicatorPaint.color)
                }
                selectedIndicator.draw(canvas)
            }
            super.onDraw(canvas)
        }
    }

    interface OnTabSelectedListener {
        fun onTabSelected(tabView: View, position: Int, isRefresh: Boolean)
        fun onTabUnselected(tabView: View, position: Int)
    }

}