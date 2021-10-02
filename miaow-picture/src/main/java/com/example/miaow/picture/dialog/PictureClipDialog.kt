package com.example.miaow.picture.dialog

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.miaow.picture.databinding.DialogPictureClipBinding
import com.example.miaow.picture.utils.AlbumUtils.saveSystemAlbum

class PictureClipDialog : PictureBaseDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): PictureClipDialog {
            return PictureClipDialog()
        }
    }

    private lateinit var bitmap: Bitmap
    private var isSaving = false
    private var callback: ClipFinishCallback? = null

    fun setBitmapResource(bitmap: Bitmap): PictureClipDialog {
        this.bitmap = bitmap
        return this
    }

    fun setClipFinishCallback(callback: ClipFinishCallback): PictureClipDialog {
        this.callback = callback
        return this
    }

    private var _binding: DialogPictureClipBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPictureClipBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.clip.setBitmapResource(bitmap)
        binding.rotate.setOnClickListener { binding.clip.rotate() }
        binding.reset.setOnClickListener { binding.clip.reset() }
        binding.cancel.setOnClickListener { dismiss() }
        binding.confirm.setOnClickListener {
            if (!isSaving) {
                isSaving = true
                Toast.makeText(it.context, "正在保存中...", Toast.LENGTH_LONG).show()
                it.context.saveSystemAlbum(binding.clip.saveBitmap()) { path ->
                    callback?.onFinish(path)
                    isSaving = false
                    dismiss()
                }
            }
        }
    }

}

interface ClipFinishCallback {
    fun onFinish(path: String)
}