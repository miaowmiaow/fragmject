package com.example.fragment.library.base.component.view

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.example.fragment.library.base.R

class SimpleEditText @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr), TextWatcher {

    private var mClearDrawable = ContextCompat.getDrawable(context, R.drawable.selector_edit_text_delete)
    private var mOnTextChangedListener: OnTextChangedListener? = null

    init {
        mClearDrawable?.apply {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
        addTextChangedListener(this)
        setClearIconVisible(false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            if (compoundDrawables[2] != null) {
                val touchable = event.x > width - totalPaddingRight && event.x < width - paddingRight
                if (touchable) {
                    text?.apply {
                        clear()
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        setClearIconVisible(s.isNotEmpty())
        mOnTextChangedListener?.apply {
            onTextChanged(s, start, before, count)
        }
    }

    override fun afterTextChanged(s: Editable) {
    }

    private fun setClearIconVisible(visible: Boolean) {
        val right = if (visible) mClearDrawable else null
        setCompoundDrawables(compoundDrawables[0], compoundDrawables[1], right, compoundDrawables[3])
    }

    fun setText(text: String) {
        setText(text, BufferType.NORMAL)
        setSelection(text.length)
    }

    fun setOnTextChangedListener(listener: OnTextChangedListener) {
        mOnTextChangedListener = listener
    }

    interface OnTextChangedListener {
        fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int)
    }
}