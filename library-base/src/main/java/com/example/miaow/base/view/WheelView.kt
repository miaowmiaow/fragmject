package com.example.miaow.base.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.graphics.toColorInt

class WheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    companion object {

        const val RIGHT_ANGLE = 90f

        /**
         * 无效的位置
         */
        const val IDLE_POSITION = -1
    }

    /**
     * 3D旋转
     */
    private val mCamera = Camera()
    private val mMatrix = Matrix()

    /**
     * 自身中心点
     */
    private var mCenterX = 0f
    private var mCenterY = 0f

    /**
     * 显示的item数量
     */
    private var mVisibleNum = 0

    /**
     * 每个item大小,  垂直布局时为item的高度, 水平布局时为item的宽度
     */
    private var mItemSize = 0

    /**
     * 每个item平均下来后对应的旋转角度
     * 根据中间分割线上下item和中间总数量计算每个item对应的旋转角度
     */
    private var mItemDegree = 0f

    /**
     * 滑动轴的半径
     */
    private var mWheelRadius = 0.0

    /**
     * 分割线画笔
     */
    private val mDividerPaint = Paint()
    private val mShadePaint = Paint()

    private var mLastSelectedPosition = IDLE_POSITION
    private var mSelectedPosition = IDLE_POSITION

    private var mSelectedMask = true

    init {
        overScrollMode = OVER_SCROLL_NEVER
        mCamera.setLocation(0f, 0f, -24f)
        LinearSnapHelper().attachToRecyclerView(this)
        mDividerPaint.isAntiAlias = true
        mDividerPaint.color = "#1b000000".toColorInt()
        mShadePaint.isAntiAlias = true
        mShadePaint.color = "#bbffffff".toColorInt()
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                onItemSelectedListener?.let { listener ->
                    if (newState != SCROLL_STATE_IDLE) return
                    val centerItemPosition = findCenterItemPosition()
                    if (centerItemPosition == IDLE_POSITION) return
                    mSelectedPosition = centerItemPosition
                    if (mSelectedPosition != mLastSelectedPosition) {
                        listener.onItemSelected(centerItemPosition)
                        mLastSelectedPosition = mSelectedPosition
                    }
                }
            }
        })
    }

    fun setVisibleNum(visibleNum: Int) {
        mVisibleNum = visibleNum
        mItemDegree = 90f / (mVisibleNum * 2 + 1)
    }

    fun setSelectMask(b: Boolean) {
        mSelectedMask = b
    }

    fun setSelectPosition(position: Int) {
        mSelectedPosition = position
    }

    fun setWheelAdapter(adapter: Adapter<ViewHolder>) {
        post {
            mItemSize = measuredHeight / (mVisibleNum * 2 + 1)
            mWheelRadius = radianToRadius(mItemSize, mItemDegree)
            this.adapter = object : RecyclerView.Adapter<ViewHolder>() {

                private val EMPTY_TYPE = 210206

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, mItemSize)
                    return if (viewType == EMPTY_TYPE) {
                        EmptyViewHolder(View(parent.context).apply {
                            this.layoutParams = layoutParams
                        })
                    } else {
                        adapter.onCreateViewHolder(parent, viewType).apply {
                            itemView.layoutParams = layoutParams
                        }
                    }
                }

                override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                    if (holder.itemViewType != EMPTY_TYPE) {
                        adapter.onBindViewHolder(holder, actualPosition(position))
                    }
                }

                override fun getItemCount(): Int {
                    return adapter.itemCount + mVisibleNum * 2
                }

                override fun getItemViewType(position: Int): Int {
                    return if (position < mVisibleNum || position >= adapter.itemCount + mVisibleNum) {
                        EMPTY_TYPE
                    } else {
                        adapter.getItemViewType(actualPosition(position))
                    }
                }

                private fun actualPosition(position: Int): Int {
                    return position - mVisibleNum
                }

            }
            this.scrollToPosition(mSelectedPosition)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCenterX = w * 0.5f
        mCenterY = h * 0.5f
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        canvas.save()
        (layoutManager as LinearLayoutManager).apply {
            if (orientation == LinearLayoutManager.VERTICAL) {
                verticalCanvasForDrawChild(canvas, child, mCenterX)
            } else {
                horizontalCanvasForDrawChild(canvas, child)
            }
        }
        val drawChild = super.drawChild(canvas, child, drawingTime)
        canvas.restore()
        return drawChild
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        (layoutManager as LinearLayoutManager).apply {
            if (mSelectedMask) {
                if (orientation == LinearLayoutManager.VERTICAL) {
                    val firstY = (measuredHeight - mItemSize) / 2.0f
                    val secondY = (measuredHeight + mItemSize) / 2.0f
                    canvas.drawRect(RectF(0f, 0f, measuredWidth.toFloat(), firstY), mShadePaint)
                    canvas.drawRoundRect(
                        RectF(0f, firstY, measuredWidth.toFloat(), secondY),
                        20f,
                        20f,
                        mDividerPaint
                    )
                    canvas.drawRect(
                        RectF(
                            0f,
                            secondY,
                            measuredWidth.toFloat(),
                            measuredHeight.toFloat()
                        ), mShadePaint
                    )
                } else {
                    val firstX = (measuredWidth - mItemSize) / 2.0f
                    val secondX = (measuredWidth + mItemSize) / 2.0f
                    canvas.drawRect(RectF(0f, 0f, firstX, measuredHeight.toFloat()), mShadePaint)
                    canvas.drawRoundRect(
                        RectF(firstX, 0f, secondX, measuredHeight.toFloat()),
                        20f,
                        20f,
                        mDividerPaint
                    )
                    canvas.drawRect(
                        RectF(
                            secondX,
                            0f,
                            measuredWidth.toFloat(),
                            measuredHeight.toFloat()
                        ), mShadePaint
                    )
                }
            }
        }
    }

    /**
     * 垂直方向旋转canvas
     * @param canvas
     * @param child
     * @param translateX
     * @return
     */
    private fun verticalCanvasForDrawChild(canvas: Canvas, child: View, translateX: Float) {
        val itemCenterY = (child.top + child.bottom) * 0.5f
        val scrollOffY = itemCenterY - mCenterY
        val rotateDegreeX =
            rotateLimitRightAngle(scrollOffY * mItemDegree / mItemSize) //垂直布局时要以X轴为中心旋转
        val rotateSinX = sin(Math.toRadians(rotateDegreeX.toDouble()))
        val rotateOffY = scrollOffY - mWheelRadius * rotateSinX //因旋转导致界面视角的偏移
        canvas.translate(0f, -rotateOffY.toFloat()) //因旋转导致界面视角的偏移
        mCamera.save()
        //旋转时离视角的z轴方向也会变化,先移动Z轴再旋转
        val z = mWheelRadius * (1 - abs(cos(Math.toRadians(rotateDegreeX.toDouble()))))
        mCamera.translate(0f, 0f, z.toFloat())
        mCamera.rotateX(-rotateDegreeX)
        mCamera.getMatrix(mMatrix)
        mCamera.restore()
        mMatrix.preTranslate(-translateX, -itemCenterY)
        mMatrix.postTranslate(translateX, itemCenterY)
        canvas.concat(mMatrix)
    }

    /**
     * 水平方向旋转canvas
     * @param canvas
     * @param child
     * @return
     */
    private fun horizontalCanvasForDrawChild(canvas: Canvas, child: View) {
        val itemCenterX = (child.left + child.right) * 0.5f
        val scrollOffX = itemCenterX - mCenterX
        val rotateDegreeY =
            rotateLimitRightAngle(scrollOffX * mItemDegree / mItemSize)
        val rotateSinY =
            sin(Math.toRadians(rotateDegreeY.toDouble())).toFloat()
        val rotateOffX = (scrollOffX - mWheelRadius * rotateSinY).toFloat()
        canvas.translate(-rotateOffX, 0f)
        mCamera.save()
        val z = mWheelRadius * (1 - abs(cos(Math.toRadians(rotateDegreeY.toDouble()))))
        mCamera.translate(0f, 0f, z.toFloat())
        mCamera.rotateY(rotateDegreeY)
        mCamera.getMatrix(mMatrix)
        mCamera.restore()
        mMatrix.preTranslate(-itemCenterX, -mCenterY)
        mMatrix.postTranslate(itemCenterX, mCenterY)
        canvas.concat(mMatrix)
    }

    /**
     * 旋转的角度绝对值不能大于90度
     * @param degree
     * @return
     */
    private fun rotateLimitRightAngle(degree: Float): Float {
        if (degree >= RIGHT_ANGLE) return RIGHT_ANGLE
        return if (degree <= -RIGHT_ANGLE) -RIGHT_ANGLE else degree
    }

    /**
     * 获取中心点位置
     * @return
     */
    fun findCenterItemPosition(): Int {
        if (adapter == null || mCenterY == 0f || mCenterX == 0f) return -1
        val centerView = findChildViewUnder(mCenterX, mCenterY)
        if (centerView != null) {
            val adapterPosition = getChildAdapterPosition(centerView) - mVisibleNum
            if (adapterPosition >= 0) {
                return adapterPosition
            }
        }
        return -1
    }

    /**
     * 根据item的大小(弧的长度),和item对应的旋转角度,计算出滑轮轴的半径
     * @param radian
     * @param degree
     * @return
     */
    private fun radianToRadius(radian: Int, degree: Float): Double {
        return radian * 180.0 / (degree * Math.PI)
    }

    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private var onItemSelectedListener: OnItemSelectedListener? = null

    fun setOnItemSelectedListener(listener: OnItemSelectedListener?) {
        this.onItemSelectedListener = listener
    }

    interface OnItemSelectedListener {
        fun onItemSelected(index: Int)
    }
}