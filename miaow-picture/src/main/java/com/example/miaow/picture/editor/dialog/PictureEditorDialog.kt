package com.example.miaow.picture.editor.dialog

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import coil.clear
import com.example.fragment.library.base.R
import com.example.fragment.library.base.dialog.FullDialog
import com.example.fragment.library.base.utils.ActivityCallback
import com.example.fragment.library.base.utils.ActivityResultHelper.startForResult
import com.example.fragment.library.base.utils.load
import com.example.miaow.picture.clip.dialog.ClipFinishCallback
import com.example.miaow.picture.clip.dialog.PictureClipDialog
import com.example.miaow.picture.databinding.PictureEditorDialogBinding
import com.example.miaow.picture.editor.bean.StickerAttrs
import com.example.miaow.picture.editor.utils.ColorUtils
import com.example.miaow.picture.editor.view.PictureEditorView
import com.example.miaow.picture.editor.view.layer.OnStickerClickListener
import com.example.miaow.picture.utils.getBitmapFromPath
import com.example.miaow.picture.utils.getBitmapFromUri
import com.example.miaow.picture.utils.getBitmapPathFromUri
import com.example.miaow.picture.utils.saveSystemAlbum

class PictureEditorDialog : FullDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): PictureEditorDialog {
            return PictureEditorDialog()
        }
    }

    private var _binding: PictureEditorDialogBinding? = null
    private val binding get() = _binding!!
    private val colors: MutableList<RelativeLayout> = arrayListOf()
    private val tools: MutableList<ImageView> = arrayListOf()
    private var bitmapPath: String? = null
    private var bitmapUri: Uri? = null
    private var callback: EditorFinishCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PictureEditorDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.apply {
            setDimAmount(1f)
        }
        colors.add(binding.white)
        colors.add(binding.black)
        colors.add(binding.red)
        colors.add(binding.yellow)
        colors.add(binding.green)
        colors.add(binding.blue)
        colors.add(binding.purple)
        tools.add(binding.graffiti)
        tools.add(binding.sticker)
        tools.add(binding.text)
        tools.add(binding.screenshot)
        tools.add(binding.mosaic)
        binding.back.setOnClickListener { dismiss() }
        binding.complete.setOnClickListener {
            if (binding.complete.isEnabled) {
                binding.complete.isEnabled = false
                binding.progress.visibility = View.VISIBLE
                binding.progress.load(R.drawable.icons8_monkey)
                Toast.makeText(it.context, "正在保存中...", Toast.LENGTH_SHORT).show()
                it.context.saveSystemAlbum(binding.picEditor.saveBitmap()) { path, uri ->
                    binding.complete.isEnabled = true
                    binding.progress.visibility = View.GONE
                    binding.progress.clear()
                    callback?.onFinish(path, uri)
                    dismiss()
                }
            }
        }
        bitmapPath?.let {
            binding.picEditor.setBitmapPath(it)
        }
        bitmapUri?.let {
            binding.picEditor.setBitmapUri(it)
        }
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
                            openAlbum()
                            tool.isSelected = false
                        }
                        2 -> {
                            openTextDialog()
                            tool.isSelected = false
                        }
                        3 -> {
                            openClipDialog(binding.picEditor.saveBitmap())
                            tool.isSelected = false
                        }
                        4 -> {
                            binding.mosaicUndo.visibility = View.VISIBLE
                            binding.picEditor.setMode(PictureEditorView.Mode.MOSAIC)
                        }
                    }
                } else {
                    tool.isSelected = false
                }
            }
        }
    }

    fun setBitmapPathOrUri(path: String?, uri: Uri?): PictureEditorDialog {
        this.bitmapPath = path
        this.bitmapUri = uri
        return this
    }

    fun setEditorFinishCallback(callback: EditorFinishCallback): PictureEditorDialog {
        this.callback = callback
        return this
    }

    private fun openAlbum() {
        activity?.apply {
            val data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val type = "image/*"
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setDataAndType(data, type)
            startForResult(intent, object : ActivityCallback {
                override fun onActivityResult(resultCode: Int, data: Intent?) {
                    data?.data?.let { uri ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            getBitmapFromUri(uri, 200)?.let { bitmap ->
                                binding.picEditor.setSticker(StickerAttrs(bitmap))
                            }
                        } else {
                            getBitmapFromPath(getBitmapPathFromUri(uri), 200)?.let { bitmap ->
                                binding.picEditor.setSticker(StickerAttrs(bitmap))
                            }
                        }
                    }
                }
            })
        }
    }

    private fun openTextDialog(attrs: StickerAttrs? = null) {
        PictureTextDialog.newInstance()
            .setStickerAttrs(attrs)
            .setTextFinishCallback(object : TextFinishCallback {
                override fun onFinish(attrs: StickerAttrs) {
                    binding.picEditor.setSticker(attrs, object : OnStickerClickListener {
                        override fun onClick(attrs: StickerAttrs) {
                            openTextDialog(attrs)
                        }
                    })
                }
            })
            .show(manager)
    }

    private fun openClipDialog(bitmap: Bitmap) {
        PictureClipDialog.newInstance()
            .setBitmapResource(bitmap)
            .setClipFinishCallback(object : ClipFinishCallback {
                override fun onFinish(path: String, uri: Uri) {
                    callback?.onFinish(path, uri)
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
    fun onFinish(path: String, uri: Uri)
}