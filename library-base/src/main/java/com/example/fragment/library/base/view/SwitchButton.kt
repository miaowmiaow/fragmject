package com.example.fragment.library.base.view

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.fragment.library.base.R
import com.example.fragment.library.base.utils.MetricsUtils
import kotlin.math.abs

class SwitchButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class STATE {
        OPEN, DRAG, CLOSE
    }

    private val buttonColor = ContextCompat.getColor(context, R.color.switch_bt)
    private val buttonShadowColor = ContextCompat.getColor(context, R.color.switch_bt_shadow)
    private val bgColor = ContextCompat.getColor(context, R.color.switch_bt_bg)
    private val checkColor = ContextCompat.getColor(context, R.color.switch_bt_check)
    private val uncheckColor = ContextCompat.getColor(context, R.color.switch_bt_uncheck)
    private var stateColor = uncheckColor

    private val argbEvaluator = ArgbEvaluator()

    /**
     * 按钮画笔
     */
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 背景画笔
     */
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 按钮半径
     */
    private var buttonRadius = 0f

    /**
     * 按钮X
     */
    private var buttonX = 0f

    /**
     * 按钮Y
     */
    private var buttonY = 0f

    /**
     * 边框宽度px
     */
    private val borderWidth = MetricsUtils.dp2px(1f)

    /**
     * 阴影半径
     */
    private val shadowRadius = MetricsUtils.dp2px(2.5f)

    /**
     * 阴影Y偏移px
     */
    private val shadowOffset = MetricsUtils.dp2px(1.5f)

    private var state = STATE.CLOSE

    private var moveX = 0f
    private var buttonOffset = 0f
    private var buttonMaxOffset = 0f

    private var viewRadius = 0f
    private var viewRadiusFraction = 0f

    private var viewLeft = 0f
    private var viewTop = 0f
    private var viewRight = 0f
    private var viewBottom = 0f

    private val downAnimator = ValueAnimator.ofFloat(0f, 1f)
    private val originAnimator = ValueAnimator.ofFloat(0f, 1f)
    private val toggleAnimator = ValueAnimator.ofFloat(0f, 1f)

    private var dragDistance = 0f
    private var onChangeListener: OnChangeListener? = null

    init {
        setPadding(
            borderWidth.toInt(),
            borderWidth.toInt(),
            borderWidth.toInt(),
            borderWidth.toInt()
        )
    }

    fun setChecked(checked: Boolean) {
        if (checked) {
            state = STATE.OPEN
            stateColor = checkColor
            buttonOffset = buttonMaxOffset
            viewRadiusFraction = 1f
        } else {
            state = STATE.CLOSE
            stateColor = uncheckColor
            buttonOffset = 0f
            viewRadiusFraction = 0f
        }
        postInvalidate()
    }

    private fun toggle() {
        if (toggleAnimator.isRunning) {
            toggleAnimator.cancel()
        }
        toggleAnimator.duration = 250
        toggleAnimator.addUpdateListener { animation ->
            buttonOffset = if (state == STATE.OPEN) {
                buttonMaxOffset * (1 - (animation.animatedValue as Float))
            } else {
                buttonMaxOffset * animation.animatedValue as Float
            }
            val value = buttonOffset / buttonMaxOffset
            val fraction = 0f.coerceAtLeast(1f.coerceAtMost(value))
            stateColor = argbEvaluator.evaluate(fraction, uncheckColor, checkColor) as Int
            postInvalidate()
        }
        toggleAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                if (buttonOffset == 0f) {
                    state = STATE.CLOSE
                    downAnimator(false)
                    onChangeListener?.onClose(this@SwitchButton)
                } else if (buttonOffset == buttonMaxOffset) {
                    state = STATE.OPEN
                    onChangeListener?.onOpen(this@SwitchButton)
                }
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }
        })
        toggleAnimator.start()
    }

    private fun downAnimator(isDown: Boolean) {
        if (buttonOffset != 0f) {
            return
        }
        if (downAnimator.isRunning) {
            downAnimator.cancel()
        }
        downAnimator.duration = 250
        downAnimator.addUpdateListener { animation ->
            viewRadiusFraction = if (isDown) {
                animation.animatedValue as Float
            } else {
                1f - (animation.animatedValue as Float)
            }
            postInvalidate()
        }
        downAnimator.start()
    }

    private fun originAnimator() {
        if (originAnimator.isRunning) {
            originAnimator.cancel()
        }
        if (buttonOffset >= buttonMaxOffset * .5f) {
            originAnimator.setFloatValues(buttonOffset, buttonMaxOffset)
        } else {
            originAnimator.setFloatValues(buttonOffset, 0f)
        }
        originAnimator.duration = 250
        originAnimator.addUpdateListener { animation ->
            buttonOffset = animation.animatedValue as Float
            val value = buttonOffset / buttonMaxOffset
            val fraction = 0f.coerceAtLeast(1f.coerceAtMost(value))
            stateColor = argbEvaluator.evaluate(fraction, uncheckColor, checkColor) as Int
            postInvalidate()
        }
        originAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                if (buttonOffset == 0f) {
                    state = STATE.CLOSE
                    downAnimator(false)
                    onChangeListener?.onClose(this@SwitchButton)
                } else if (buttonOffset == buttonMaxOffset) {
                    state = STATE.OPEN
                    onChangeListener?.onOpen(this@SwitchButton)
                }
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }
        })
        originAnimator.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downAnimator(true)
                dragDistance = event.x
                moveX = event.x
            }
            MotionEvent.ACTION_MOVE -> {
                state = STATE.DRAG
                val x = event.x
                buttonOffset += x - moveX
                moveX = x
                when {
                    buttonOffset <= 0f -> {
                        state = STATE.CLOSE
                        buttonOffset = 0f
                    }
                    buttonOffset >= buttonMaxOffset -> {
                        state = STATE.OPEN
                        buttonOffset = buttonMaxOffset
                    }
                    else -> {
                        if (downAnimator.isRunning) {
                            downAnimator.cancel()
                        }
                    }
                }
                val value = buttonOffset / buttonMaxOffset
                val fraction = 0f.coerceAtLeast(1f.coerceAtMost(value))
                stateColor = argbEvaluator.evaluate(fraction, uncheckColor, checkColor) as Int
                postInvalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (abs(event.x - dragDistance) < buttonMaxOffset) {
                    toggle()
                } else {
                    if (state == STATE.DRAG) {
                        originAnimator()
                    } else {
                        if (buttonOffset == 0f) {
                            downAnimator(false)
                        }
                        if (state == STATE.OPEN) {
                            onChangeListener?.onOpen(this@SwitchButton)
                        } else {
                            onChangeListener?.onClose(this@SwitchButton)
                        }
                    }
                }
            }
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewLeft = 0f + paddingStart
        viewTop = 0f + paddingTop
        viewRight = width.toFloat() - paddingEnd
        viewBottom = height.toFloat() - paddingBottom
        viewRadius = (viewBottom - viewTop) * .5f
        buttonMaxOffset = viewRight - viewRadius * 2 - borderWidth
        buttonX = viewRadius + paddingStart
        buttonY = viewRadius + paddingTop
        buttonRadius = viewRadius - borderWidth
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCheckState(canvas)
        drawBackground(canvas)
        drawButton(canvas)
    }

    private fun drawCheckState(canvas: Canvas) {
        bgPaint.style = Paint.Style.FILL_AND_STROKE
        bgPaint.color = stateColor
        canvas.drawRoundRect(
            viewLeft, viewTop, viewRight, viewBottom,
            viewRadius, viewRadius, bgPaint
        )
        bgPaint.style = Paint.Style.STROKE
        bgPaint.strokeWidth = borderWidth
        bgPaint.color = stateColor
        canvas.drawRoundRect(
            viewLeft, viewTop, viewRight, viewBottom,
            viewRadius, viewRadius, bgPaint
        )
    }

    private fun drawBackground(canvas: Canvas) {
        if (state == STATE.DRAG) {
            viewRadiusFraction = 1f
        }
        bgPaint.style = Paint.Style.FILL
        bgPaint.color = bgColor
        canvas.drawRoundRect(
            viewLeft + (viewRight * viewRadiusFraction * .5f),
            viewTop + (viewBottom * viewRadiusFraction * .5f),
            viewRight - (viewRight * viewRadiusFraction * .5f),
            viewBottom - (viewBottom * viewRadiusFraction * .5f),
            viewRadius,
            viewRadius,
            bgPaint
        )
    }

    private fun drawButton(canvas: Canvas) {
        buttonPaint.style = Paint.Style.FILL_AND_STROKE
        buttonPaint.color = buttonColor
        buttonPaint.setShadowLayer(shadowRadius, 0f, shadowOffset, buttonShadowColor)
        canvas.drawCircle(buttonX + buttonOffset, buttonY, buttonRadius, buttonPaint)
    }

    fun setOnChangeListener(l: OnChangeListener) {
        onChangeListener = l
    }

    interface OnChangeListener {
        fun onOpen(view: SwitchButton)
        fun onClose(view: SwitchButton)
    }

}