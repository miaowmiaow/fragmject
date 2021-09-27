package com.example.miaow.picture.dialog

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import com.example.miaow.picture.editor.PictureEditorView
import com.example.miaow.picture.bean.StickerAttrs
import com.example.miaow.picture.editor.layer.OnStickerClickListener
import com.example.miaow.picture.utils.AlbumUtil.saveSystemAlbum
import com.example.miaow.picture.utils.ColorUtils
import com.example.miaow.picture.databinding.DialogPictureEditorBinding

class PictureEditorDialog : PictureBaseDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): PictureEditorDialog {
            return PictureEditorDialog()
        }
    }

    private var _binding: DialogPictureEditorBinding? = null
    private val binding get() = _binding!!

    private val colors: MutableList<RelativeLayout> = arrayListOf()
    private val tools: MutableList<ImageView> = arrayListOf()
    private var bitmapPath = ""
    private var callback: EditorFinishCallback? = null

    fun setBitmapPath(path: String): PictureEditorDialog {
        this.bitmapPath = path
        return this
    }

    fun setEditorFinishCallback(callback: EditorFinishCallback): PictureEditorDialog {
        this.callback = callback
        return this
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPictureEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        colors.add(binding.white)
        colors.add(binding.black)
        colors.add(binding.red)
        colors.add(binding.yellow)
        colors.add(binding.green)
        colors.add(binding.blue)
        colors.add(binding.purple)
        tools.add(binding.graffiti)
        tools.add(binding.sticker)
        tools.add(binding.mosaic)
        tools.add(binding.screenshot)
        binding.back.setOnClickListener { dismiss() }
        binding.complete.setOnClickListener {
            it.context.saveSystemAlbum(binding.picEditor.saveBitmap()) { path ->
                callback?.onFinish(path)
                dismiss()
            }
        }
        binding.picEditor.setBitmapPath(bitmapPath)
        binding.colorUndo.setOnClickListener { binding.picEditor.graffitiUndo() }
        binding.mosaicUndo.setOnClickListener { binding.picEditor.mosaicUndo() }
        colors.forEachIndexed { index, color ->
            color.setOnClickListener {
                selectedColor(color)
                binding.picEditor.setGraffitiColor(ColorUtils.colorful[index])
            }
        }
        tools.forEachIndexed { index, tool ->
            tool.setOnClickListener {
                binding.colorBar.visibility = View.GONE
                binding.mosaicUndo.visibility = View.GONE
                binding.picEditor.setMode(PictureEditorView.Mode.STICKER)
                if (!tool.isSelected) {
                    selectedTool(tool)
                    when (index) {
                        0 -> {
                            binding.colorBar.visibility = View.VISIBLE
                            binding.picEditor.setMode(PictureEditorView.Mode.GRAFFITI)
                        }
                        1 -> {
                            openTextDialog()
                            tool.isSelected = false
                        }
                        2 -> {
                            binding.mosaicUndo.visibility = View.VISIBLE
                            binding.picEditor.setMode(PictureEditorView.Mode.MOSAIC)
                        }
                        3 -> {
                            openClipDialog(binding.picEditor.saveBitmap())
                            tool.isSelected = false
                        }
                    }
                } else {
                    tool.isSelected = false
                }
            }
        }
    }

    private fun openTextDialog(attrs: StickerAttrs? = null) {
        PictureTextDialog.newInstance()
            .setStickerAttrs(attrs)
            .setTextFinishCallback(object : TextFinishCallback {
                override fun onFinish(attrs: StickerAttrs) {
                    binding.picEditor.setTextSticker(attrs, object : OnStickerClickListener {
                        override fun onClick(attrs: StickerAttrs) {
                            openTextDialog(attrs)
                        }
                    })
                }
            })
            .show(manager)
    }

    private fun openClipDialog(clipBmp: Bitmap) {
        PictureClipDialog.newInstance()
            .setBitmapResource(clipBmp)
            .setClipFinishCallback(object : ClipFinishCallback {
                override fun onFinish(path: String) {
                    callback?.onFinish(path)
                    dismiss()
                }
            })
            .show(manager)
    }

    private fun selectedColor(view: View) {
        colors.forEach {
            it.isSelected = false
        }
        view.isSelected = true
    }

    private fun selectedTool(view: View) {
        tools.forEach {
            it.isSelected = false
        }
        view.isSelected = true
    }

}

interface EditorFinishCallback {
    fun onFinish(path: String)
}