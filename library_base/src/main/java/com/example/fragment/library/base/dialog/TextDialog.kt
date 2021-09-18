package com.example.fragment.library.base.dialog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import com.example.fragment.library.base.R

class TextDialog : BaseDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): TextDialog {
            return TextDialog()
        }
    }

    private lateinit var textBack: TextView
    private lateinit var textFinish: TextView
    private lateinit var editText: EditText
    private lateinit var textWhite: RelativeLayout
    private lateinit var textBlack: RelativeLayout
    private lateinit var textRed: RelativeLayout
    private lateinit var textYellow: RelativeLayout
    private lateinit var textGreen: RelativeLayout
    private lateinit var textBlue: RelativeLayout
    private lateinit var textPurple: RelativeLayout

    private val colors = arrayListOf(
        Color.parseColor("#ffffff"),
        Color.parseColor("#000000"),
        Color.parseColor("#ff0000"),
        Color.parseColor("#ffb636"),
        Color.parseColor("#00FF00"),
        Color.parseColor("#508cee"),
        Color.parseColor("#7B68EE"),
    )

    private val textColors: MutableList<RelativeLayout> = arrayListOf()
    private var onTextFinishListener: OnTextFinishListener? = null
    private var text: String? = null

    fun setText(text: String?): TextDialog {
        this.text = text
        return this
    }

    fun setOnTextFinishListener(onTextFinishListener: OnTextFinishListener): TextDialog {
        this.onTextFinishListener = onTextFinishListener
        return this
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        dialog?.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
        super.onActivityCreated(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullDialog)
        dialog?.window?.apply {
            attributes.gravity = Gravity.BOTTOM
            decorView.setPadding(0, 0, 0, 0)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_text, container, false)

        textBack = view.findViewById(R.id.text_back)
        textFinish = view.findViewById(R.id.text_finish)
        editText = view.findViewById(R.id.edit_text)
        textWhite = view.findViewById(R.id.text_white)
        textBlack = view.findViewById(R.id.text_black)
        textRed = view.findViewById(R.id.text_red)
        textYellow = view.findViewById(R.id.text_yellow)
        textGreen = view.findViewById(R.id.text_green)
        textBlue = view.findViewById(R.id.text_blue)
        textPurple = view.findViewById(R.id.text_purple)

        textColors.add(textWhite)
        textColors.add(textBlack)
        textColors.add(textRed)
        textColors.add(textYellow)
        textColors.add(textGreen)
        textColors.add(textBlue)
        textColors.add(textPurple)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        text?.let {
            editText.setText(text)
        }
        editText.postDelayed({
            showSoftInput(editText)
        }, 250)
        textBack.setOnClickListener {
            text?.let {
                if(it.isNotBlank()){
                    editText.setText(text)
                    editText.isFocusable = false
                    onTextFinishListener?.onFinish(conversionBitmap(), editText.text.toString())
                }
            }
            hideSoftInput(editText)
            dismiss()
        }
        textFinish.setOnClickListener {
            hideSoftInput(editText)
            val str = editText.text.toString()
            if(str.isNotBlank()){
                editText.hint = ""
                editText.isFocusable = false
                onTextFinishListener?.onFinish(conversionBitmap(), editText.text.toString())
            }
            dismiss()
        }
        textWhite.setOnClickListener {
            editText.setTextColor(colors[0])
            selectedColor(it)
        }
        textBlack.setOnClickListener {
            editText.setTextColor(colors[1])
            selectedColor(it)
        }
        textRed.setOnClickListener {
            editText.setTextColor(colors[2])
            selectedColor(it)
        }
        textYellow.setOnClickListener {
            editText.setTextColor(colors[3])
            selectedColor(it)
        }
        textGreen.setOnClickListener {
            editText.setTextColor(colors[4])
            selectedColor(it)
        }
        textBlue.setOnClickListener {
            editText.setTextColor(colors[5])
            selectedColor(it)
        }
        textPurple.setOnClickListener {
            editText.setTextColor(colors[6])
            selectedColor(it)
        }
    }

    private fun selectedColor(view: View) {
        textColors.forEach {
            it.isSelected = false
        }
        view.isSelected = true
    }

    private fun showSoftInput(view: View) {
        view.context.getSystemService(Context.INPUT_METHOD_SERVICE).apply {
            if (this is InputMethodManager) {
                showSoftInput(view, 0)
            }
        }
    }

    private fun hideSoftInput(view: View) {
        view.context.getSystemService(Context.INPUT_METHOD_SERVICE).apply {
            if (this is InputMethodManager) {
                hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }

    private fun conversionBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(editText.width, editText.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)
        editText.draw(canvas)
        return bitmap
    }

}

interface OnTextFinishListener {
    fun onFinish(bitmap: Bitmap, contentDescription: String)
}