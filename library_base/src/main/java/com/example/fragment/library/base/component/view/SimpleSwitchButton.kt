package com.example.fragment.library.base.component.view

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

class SimpleSwitchButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val buttonColor = ContextCompat.getColor(context, R.color.switch_bt)
    private val shadowColor = ContextCompat.getColor(context, R.color.switch_bt_shadow)
    private val borderColor = ContextCompat.getColor(context, R.color.switch_bt_border)
    private val checkColor = ContextCompat.getColor(context, R.color.switch_bt_check)
    private val uncheckColor = ContextCompat.getColor(context, R.color.switch_bt_uncheck)
    private var bgColor = uncheckColor

    private val argbEvaluator = ArgbEvaluator()

    /**
     * 按钮画笔
     */
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)

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
     * 边框画笔
     */
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 边框宽度px
     */
    private val borderWidth = MetricsUtils.dp2px(1f)

    /**
     * 背景画笔
     */
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 阴影半径
     */
    private val shadowRadius = MetricsUtils.dp2px(2.5f)

    /**
     * 阴影Y偏移px
     */
    private val shadowOffset = MetricsUtils.dp2px(1.5f)

    private var isOpen = false

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
    private val moveAnimator = ValueAnimator.ofFloat(0f, 1f)

    private fun downAnimator(isDown: Boolean) {
        if (buttonOffset != 0f) {
            return
        }
        if (downAnimator.isRunning) {
            downAnimator.cancel()
        }
        downAnimator.duration = 300
        downAnimator.addUpdateListener { animation ->
            viewRadiusFraction = if (isDown) {
                animation.animatedValue as Float
            } else {
                1f - (animation.animatedValue as Float)
            }
            postInvalidate()
        }
        downAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                bgColor = when {
                    isOpen -> checkColor
                    isDown -> borderColor
                    else -> uncheckColor
                }
                viewRadiusFraction = 0f
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }

        })
        downAnimator.start()
    }

    private fun moveAnimator() {
        if (moveAnimator.isRunning) {
            moveAnimator.cancel()
        }
        if (buttonOffset >= buttonMaxOffset * .5f) {
            isOpen = true
            moveAnimator.setFloatValues(buttonOffset, buttonMaxOffset)
        } else {
            isOpen = false
            moveAnimator.setFloatValues(buttonOffset, 0f)
        }
        moveAnimator.duration = 300
        moveAnimator.addUpdateListener { animation ->
            buttonOffset = animation.animatedValue as Float
            postInvalidate()
        }
        moveAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                if (buttonOffset == buttonMaxOffset) {
                    isOpen = true
                } else if (buttonOffset == 0f) {
                    isOpen = false
                }
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }

        })
        moveAnimator.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downAnimator(true)
                moveX = event.x
            }
            MotionEvent.ACTION_MOVE -> {
                if (downAnimator.isRunning) {
                    downAnimator.cancel()
                }
                val x = event.x
                buttonOffset += x - moveX
                moveX = x
                when {
                    buttonOffset <= 0f -> {
                        isOpen = false
                        buttonOffset = 0f
                        downAnimator(false)
                    }
                    buttonOffset >= buttonMaxOffset -> {
                        isOpen = true
                        buttonOffset = buttonMaxOffset
                    }
                    else -> {
                        val value = buttonOffset / buttonMaxOffset
                        val fraction = 0f.coerceAtLeast(1f.coerceAtMost(value))
                        bgColor = argbEvaluator.evaluate(fraction, borderColor, checkColor) as Int
                        postInvalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                moveAnimator()
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
        drawBackground(canvas)
        drawBorder(canvas)
        drawButton(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        bgPaint.style = Paint.Style.FILL_AND_STROKE
        bgPaint.color = borderColor
        canvas.drawRoundRect(
            viewLeft, viewTop, viewRight, viewBottom,
            viewRadius, viewRadius, bgPaint
        )
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

    private fun drawBorder(canvas: Canvas) {
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = borderWidth
        borderPaint.color = borderColor
        canvas.drawRoundRect(
            viewLeft, viewTop, viewRight, viewBottom,
            viewRadius, viewRadius, borderPaint
        )
    }

    private fun drawButton(canvas: Canvas) {
        buttonPaint.style = Paint.Style.FILL_AND_STROKE
        buttonPaint.color = buttonColor
        buttonPaint.setShadowLayer(shadowRadius, 0f, shadowOffset, shadowColor)
        canvas.drawCircle(buttonX + buttonOffset, buttonY, buttonRadius, buttonPaint)
    }

}