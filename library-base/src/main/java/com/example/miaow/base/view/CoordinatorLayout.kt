package com.example.miaow.base.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.*

/**
 * 一般情况下，事件是从child的触摸事件开始的，
 * 1. 首先调用child.startNestedScroll()方法，此方法内部通过 NestedScrollingChildHelper 调用并返回parent.onStartNestedScroll()方法的结果，
 *    为true，说明parent接受了嵌套滑动，同时调用了parent.onNestedScrollAccepted()方法，此时开始嵌套滑动；
 * 2. 在滑动事件中，child通过child.dispatchNestedPreScroll()方法分配滑动的距离，
 *    child.dispatchNestedPreScroll()内部会先调用parent.onNestedPreScroll()方法，由parent先处理滑动距离。
 * 3. parent消耗完成之后，再将剩余的距离传递给child，child拿到parent使用完成之后的距离之后，自己再处理剩余的距离。
 * 4. 如果此时子控件还有未处理的距离，则将剩余的距离再次通过 child.dispatchNestedScroll()方法调用parent.onNestedScroll()方法，将剩余的距离交个parent来进行处理
 * 5. 滑动结束之后，调用 child.stopNestedScroll()通知parent滑动结束，至此，触摸滑动结束
 * 6. 触摸滑动结束之后，child会继续进行惯性滑动，惯性滑动可以通过 Scroller 实现，
 *    具体滑动可以自己来处理，在fling过程中，和触摸滑动调用流程一样，需要注意type参数的区分，用来通知parent两种不同的滑动流程
 */
class CoordinatorLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), NestedScrollingParent3, NestedScrollingChild3 {

    private val parentHelper = NestedScrollingParentHelper(this)
    private val childHelper = NestedScrollingChildHelper(this)
    private val scrollConsumed = IntArray(2)
    private var maxScrollY = 0

    init {
        isNestedScrollingEnabled = true
    }

    /**
     * 设置最大滑动距离
     *
     * @param maxScrollY 最大滑动距离
     */
    fun setMaxScrollY(maxScrollY: Int) {
        this.maxScrollY = maxScrollY
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        childHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return childHelper.isNestedScrollingEnabled
    }

    /**
     * 开始滑动前调用，在惯性滑动和触摸滑动前都会进行调用，此方法一般在 onInterceptTouchEvent或者onTouch中，通知父类方法开始滑动
     * 会调用父类方法的 onStartNestedScroll onNestedScrollAccepted 两个方法
     *
     * @param axes 滑动方向
     * @param type 开始滑动的类型 the type of input which cause this scroll event
     * @return 有父视图并且开始滑动，则返回true 实际上就是看parent的 onStartNestedScroll 方法
     */
    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return childHelper.startNestedScroll(axes, type)
    }

    /**
     * 子控件停止滑动，例如手指抬起，惯性滑动结束
     *
     * @param type 停止滑动的类型 TYPE_TOUCH，TYPE_NON_TOUCH
     */
    override fun stopNestedScroll(type: Int) {
        childHelper.stopNestedScroll(type)
    }

    /**
     * 判断是否有父View 支持嵌套滑动
     */
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

    /**
     * 在dispatchNestedPreScroll 之后进行调用
     * 当滑动的距离父控件消耗后，父控件将剩余的距离再次交个子控件，
     * 子控件再次消耗部分距离后，又继续将剩余的距离分发给父控件,由父控件判断是否消耗剩下的距离。
     * 如果四个消耗的距离都是0，则表示没有神可以消耗的了，会直接返回false，否则会调用父控件的
     * onNestedScroll 方法，父控件继续消耗剩余的距离
     * 会调用父控件的
     *
     * @param dxConsumed     水平方向嵌套滑动的子控件滑动的距离(消耗的距离)    dx<0 向右滑动 dx>0 向左滑动 （保持和 RecycleView 一致）
     * @param dyConsumed     垂直方向嵌套滑动的子控件滑动的距离(消耗的距离)    dy<0 向下滑动 dy>0 向上滑动 （保持和 RecycleView 一致）
     * @param dxUnconsumed   水平方向嵌套滑动的子控件未滑动的距离(未消耗的距离)dx<0 向右滑动 dx>0 向左滑动 （保持和 RecycleView 一致）
     * @param dyUnconsumed   垂直方向嵌套滑动的子控件未滑动的距离(未消耗的距离)dy<0 向下滑动 dy>0 向上滑动 （保持和 RecycleView 一致）
     * @param offsetInWindow 子控件在当前window的偏移量
     * @return 如果返回true, 表示父控件又继续消耗了
     */
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

    /**
     * 子控件在开始滑动前，通知父控件开始滑动，同时由父控件先消耗滑动时间
     * 在子View的onInterceptTouchEvent或者onTouch中，调用该方法通知父View滑动的距离
     * 最终会调用父view的 onNestedPreScroll 方法
     *
     * @param dx             水平方向嵌套滑动的子控件想要变化的距离 dx<0 向右滑动 dx>0 向左滑动 （保持和 RecycleView 一致）
     * @param dy             垂直方向嵌套滑动的子控件想要变化的距离 dy<0 向下滑动 dy>0 向上滑动 （保持和 RecycleView 一致）
     * @param consumed       父控件消耗的距离，父控件消耗完成之后，剩余的才会给子控件，子控件需要使用consumed来进行实际滑动距离的处理
     * @param offsetInWindow 子控件在当前window的偏移量
     * @param type           滑动类型，ViewCompat.TYPE_NON_TOUCH fling效果,ViewCompat.TYPE_TOUCH 手势滑动
     * @return true表示父控件进行了滑动消耗，需要处理 consumed 的值，false表示父控件不对滑动距离进行消耗，可以不考虑consumed数据的处理，此时consumed中两个数据都应该为0
     */
    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }

    /**
     * 开始NestedScroll时调用，判断父View是否接受嵌套滑动
     *
     * @param child  嵌套滑动对应的父类的子类(因为嵌套滑动对于的父View不一定是一级就能找到的，可能挑了两级父View的父View，child的辈分>=target)
     * @param target 具体嵌套滑动的那个子类
     * @param axes   支持嵌套滚动轴。水平方向，垂直方向，或者不指定
     * @param type   滑动类型，ViewCompat.TYPE_NON_TOUCH fling 效果ViewCompat.TYPE_TOUCH 手势滑动
     */
    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
    }

    /**
     * 当onStartNestedScroll返回为true时，也就是父控件接受嵌套滑动时，该方法才会调用
     */
    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        parentHelper.onNestedScrollAccepted(child, target, axes, type)
        startNestedScroll(axes, type)
    }

    /**
     * 嵌套滑动结束
     */
    override fun onStopNestedScroll(target: View, type: Int) {
        parentHelper.onStopNestedScroll(target, type)
        stopNestedScroll(type)
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
        //由parent先处理滑动距离
        if (!dispatchNestedPreScroll(dx, dy, consumed, null, type)) {
            if (dy > 0 && scrollY < maxScrollY) {
                val sy = if (scrollY + dy > maxScrollY) {
                    maxScrollY - scrollY
                } else dy
                scrollBy(0, sy)
                consumed[1] = sy
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
        if (dyUnconsumed < 0 && scrollY > 0) {
            myDyConsumed = if (scrollY + dyUnconsumed < 0) {
                -scrollY
            } else {
                dyUnconsumed
            }
            scrollBy(0, myDyConsumed)
            consumed[1] += myDyConsumed
        }
        //将剩余的距离交给parent来进行处理
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