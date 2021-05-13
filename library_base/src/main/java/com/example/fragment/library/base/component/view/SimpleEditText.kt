package com.example.fragment.library.base.component.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.example.fragment.library.base.R

class SimpleEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr), View.OnFocusChangeListener, TextWatcher {

    private val clearDrawable =
        ContextCompat.getDrawable(context, R.drawable.selector_edit_text_delete)

    private var hasFocus = false
    private var root: View? = null
    private var onGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    init {
        clearDrawable?.apply {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
        setClearIconVisible(false)
        addTextChangedListener(this)
        onFocusChangeListener = this
    }

    fun addKeyboardListener(root: View) {
        onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (hasFocus) {
                val rect = Rect()
                root.getWindowVisibleDisplayFrame(rect)
                //获取窗体的不可视区域高度，在键盘没有弹起时，root.getHeight()调节度应该和rect.bottom高度一样
                val invisibleHeight = root.height - rect.bottom
                //屏幕高度
                val screenHeight = root.height
                //不可见区域大于屏幕本身高度的1/4说明键盘弹起了
                if (invisibleHeight > screenHeight / 4) {
                    val location = IntArray(2)
                    // 4､获取控件自身的窗体坐标，算出需要滚动的高度
                    this.getLocationInWindow(location)
                    val scrollHeight = location[1] + this.height - rect.bottom
                    if (scrollHeight > 0) {
                        AnimatorSet().also { set ->
                            set.playTogether(
                                ObjectAnimator.ofFloat(
                                    root,
                                    "translationY",
                                    -scrollHeight.toFloat()
                                )
                            )
                        }.start()
                    }
                } else {
                    //不可见区域小于屏幕高度1/4时,说明键盘隐藏了，把界面下移，移回到原有高度
                    AnimatorSet().also { set ->
                        set.playTogether(
                            ObjectAnimator.ofFloat(root, "translationY", 0F)
                        )
                    }.start()
                }
            }
        }
        root.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
        this.root = root
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        root?.viewTreeObserver?.removeOnGlobalLayoutListener(onGlobalLayoutListener)
    }

    override fun onFocusChange(view: View, hasFocus: Boolean) {
        this.hasFocus = hasFocus
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            clearDrawable?.let {
                val xTouchable =
                    (event.x > width - paddingRight - it.intrinsicWidth
                            && event.x < width - paddingRight)
                val yTouchable = (event.y > (height - it.intrinsicHeight) / 2
                        && event.y < (height + it.intrinsicHeight) / 2)
                if (xTouchable && yTouchable) {
                    setText("")
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        setClearIconVisible(s.isNotEmpty())
    }

    override fun afterTextChanged(s: Editable) {
    }

    private fun setClearIconVisible(visible: Boolean) {
        val right = if (visible) clearDrawable else null
        setCompoundDrawables(
            compoundDrawables[0],
            compoundDrawables[1],
            right,
            compoundDrawables[3]
        )
    }

    fun setText(text: String) {
        setText(text, BufferType.NORMAL)
        setSelection(text.length)
    }

}