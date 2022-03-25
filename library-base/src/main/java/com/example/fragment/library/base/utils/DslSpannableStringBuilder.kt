package com.example.fragment.library.base.utils

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.text.style.DynamicDrawableSpan.ALIGN_BOTTOM
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import java.util.*

//为 TextView 创建扩展函数，其参数为接口的扩展函数
fun TextView.buildSpannableString(init: DslSpannableStringBuilder.() -> Unit) {
    //具体实现类
    val spanStringBuilderImpl = DslSpannableStringBuilderImpl()
    spanStringBuilderImpl.init()
    movementMethod = LinkMovementMethod.getInstance()
    //通过实现类返回SpannableStringBuilder
    text = spanStringBuilderImpl.build()
}

class DslSpannableStringBuilderImpl : DslSpannableStringBuilder {

    private val stringBuilder = SpannableStringBuilder()

    override fun append(text: String, method: (DslSpan.() -> Unit)?) {
        val start = stringBuilder.length
        stringBuilder.append(text)
        val end = stringBuilder.length
        val dslSpan = DslSpanImpl()
        method?.apply { dslSpan.this() }
        dslSpan.apply {
            spans.forEach {
                stringBuilder.setSpan(it, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    fun build(): SpannableStringBuilder {
        return stringBuilder
    }

}

class DslSpanImpl : DslSpan {
    var spans: MutableList<CharacterStyle> = ArrayList()

    override fun setBackgroundColor(color: Int) {
        spans.add(BackgroundColorSpan(color))
    }

    override fun setClick(color: Int, underlineText: Boolean, onClick: (View) -> Unit) {
        spans.add(object : ClickableSpan() {
            override fun updateDrawState(ds: TextPaint) {
                ds.color = color
                ds.isUnderlineText = underlineText
            }

            override fun onClick(widget: View) {
                onClick(widget)
            }
        })
    }

    override fun setColor(color: Int) {
        spans.add(ForegroundColorSpan(color))
    }

    override fun setImage(drawable: Drawable, verticalAlignment: Int) {
        drawable.setBounds(0,0,drawable.intrinsicWidth, drawable.intrinsicHeight)
        spans.add(ImageSpan(drawable, verticalAlignment))
    }

    override fun setSize(size: Int) {
        spans.add(AbsoluteSizeSpan(size))
    }

    override fun setStrikethrough() {
        spans.add(StrikethroughSpan())
    }

    override fun setStyle(style: Int) {
        spans.add(StyleSpan(style))
    }

    override fun setUnderLine(use: Boolean) {
        if (use) spans.add(UnderlineSpan())
    }
}

interface DslSpannableStringBuilder {

    fun append(text: String, method: (DslSpan.() -> Unit)? = null)

}

interface DslSpan {

    //设置背景颜色
    fun setBackgroundColor(@ColorInt color: Int)

    //设置点击事件
    fun setClick(@ColorInt color: Int, underlineText: Boolean, onClick: (View) -> Unit)

    //设置文字颜色
    fun setColor(@ColorInt color: Int)

    //设置图片
    fun setImage(drawable: Drawable, verticalAlignment: Int = ALIGN_BOTTOM)

    //设置字体大小
    fun setSize(size: Int)

    fun setStrikethrough()

    //设置字体样式
    fun setStyle(style: Int = Typeface.NORMAL)

    //设置下划线
    fun setUnderLine(use: Boolean)

}