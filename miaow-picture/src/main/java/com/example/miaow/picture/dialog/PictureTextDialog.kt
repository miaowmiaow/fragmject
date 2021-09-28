package com.example.miaow.picture.dialog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import com.example.miaow.picture.bean.StickerAttrs
import com.example.miaow.picture.utils.ColorUtils
import com.example.miaow.picture.R
import com.example.miaow.picture.databinding.DialogPictureTextBinding

class PictureTextDialog : PictureBaseDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): PictureTextDialog {
            return PictureTextDialog()
        }
    }

    private var _binding: DialogPictureTextBinding? = null
    private val binding get() = _binding!!

    private val textColors: MutableList<RelativeLayout> = arrayListOf()
    private var _attrs: StickerAttrs? = null
    private val attrs get() = _attrs!!
    private var callback: TextFinishCallback? = null

    fun setStickerAttrs(attrs: StickerAttrs?): PictureTextDialog {
        this._attrs = attrs
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
        _attrs?.apply {
            binding.editText.setText(description)
        }
        textColors.add(binding.textWhite)
        textColors.add(binding.textBlack)
        textColors.add(binding.textRed)
        textColors.add(binding.textYellow)
        textColors.add(binding.textGreen)
        textColors.add(binding.textBlue)
        textColors.add(binding.textPurple)
        binding.bg.setOnClickListener{
            showSoftInput(binding.editText)
        }
        binding.editText.postDelayed({
            showSoftInput(binding.editText)
        }, 250)
        binding.textBack.setOnClickListener {
            hideSoftInput(binding.editText)
            binding.editText.isFocusable = false
            dismiss()
        }
        binding.textFinish.setOnClickListener {
            hideSoftInput(binding.editText)
            binding.editText.isFocusable = false
            val description = binding.editText.text.toString()
            if(description.isNotBlank()){
                if (_attrs == null) {
                    _attrs = StickerAttrs(saveBitmap())
                } else {
                    attrs.bitmap = saveBitmap()
                }
                attrs.description = description
                callback?.onFinish(attrs)
            }
            dismiss()
        }
        textColors.forEachIndexed { index, view ->
            view.setOnClickListener {
                selectedColor(view)
                binding.editText.setTextColor(ColorUtils.colorful[index])
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

    private fun saveBitmap(): Bitmap {
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
    fun onFinish(attrs: StickerAttrs)
}