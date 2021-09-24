package com.example.fragment.library.base.dialog

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fragment.library.base.databinding.DialogPictureClipBinding
import com.example.fragment.library.base.utils.AlbumUtil.saveSystemAlbum

class PictureClipDialog : PictureBaseDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): PictureClipDialog {
            return PictureClipDialog()
        }
    }

    private lateinit var bitmap: Bitmap
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
            it.context.saveSystemAlbum(binding.clip.saveBitmap()) { path ->
                callback?.onFinish(path)
                dismiss()
            }
        }
    }

}

interface ClipFinishCallback {
    fun onFinish(path: String)
}