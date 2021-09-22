package com.example.fragment.library.base.dialog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import com.example.fragment.library.base.R
import com.example.fragment.library.base.databinding.DialogPictureTextBinding

class PictureTextDialog : BaseDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): PictureTextDialog {
            return PictureTextDialog()
        }
    }

    private var _binding: DialogPictureTextBinding? = null
    private val binding get() = _binding!!
    private val textColors: MutableList<RelativeLayout> = arrayListOf()
    private val colors = arrayListOf(
        Color.parseColor("#ffffff"),
        Color.parseColor("#000000"),
        Color.parseColor("#ff0000"),
        Color.parseColor("#ffb636"),
        Color.parseColor("#00FF00"),
        Color.parseColor("#508cee"),
        Color.parseColor("#7B68EE"),
    )

    private var text = ""
    private var callback: TextFinishCallback? = null

    fun setText(text: String): PictureTextDialog {
        this.text = text
        return this
    }

    fun setTextFinishCallback(callback: TextFinishCallback): PictureTextDialog {
        this.callback = callback
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
    ): View {
        _binding = DialogPictureTextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
    }

    private fun setupView() {
        binding.editText.setText(text)
        textColors.add(binding.textWhite)
        textColors.add(binding.textBlack)
        textColors.add(binding.textRed)
        textColors.add(binding.textYellow)
        textColors.add(binding.textGreen)
        textColors.add(binding.textBlue)
        textColors.add(binding.textPurple)
        binding.editText.postDelayed({
            showSoftInput(binding.editText)
        }, 250)
        binding.textBack.setOnClickListener {
            hideSoftInput(binding.editText)
            if (text.isNotBlank()) {
                binding.editText.setText(text)
                binding.editText.isFocusable = false
                callback?.onFinish(conversionBitmap(), text)
            }
            dismiss()
        }
        binding.textFinish.setOnClickListener {
            hideSoftInput(binding.editText)
            val text = binding.editText.text.toString()
            if (text.isNotBlank()) {
                binding.editText.hint = ""
                binding.editText.isFocusable = false
                callback?.onFinish(conversionBitmap(), text)
            }
            dismiss()
        }
        textColors.forEachIndexed { index, view ->
            view.setOnClickListener {
                selectedColor(view)
                binding.editText.setTextColor(colors[index])
            }
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
        val bitmap = Bitmap.createBitmap(
            binding.editText.width,
            binding.editText.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)
        binding.editText.draw(canvas)
        return bitmap
    }

}

interface TextFinishCallback {
    fun onFinish(bitmap: Bitmap, contentDescription: String)
}