package com.example.miaow.picture.clip.dialog

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.dispose
import coil.load
import com.example.fragment.library.base.R
import com.example.fragment.library.base.dialog.FullDialog
import com.example.fragment.library.base.utils.dp2px
import com.example.fragment.library.base.utils.saveImagesToAlbum
import com.example.miaow.picture.databinding.PictureClipDialogBinding

class PictureClipDialog : FullDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): PictureClipDialog {
            return PictureClipDialog()
        }
    }

    private lateinit var bitmap: Bitmap
    private var isSaving = false
    private var _binding: PictureClipDialogBinding? = null
    private val binding get() = _binding!!
    private var _callback: PictureClipCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PictureClipDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.apply {
            setDimAmount(1f)
        }
        binding.clip.setBitmapResource(bitmap)
        binding.clip.setPadding(dp2px(45f), dp2px(90f))
        binding.rotate.setOnClickListener { binding.clip.rotate() }
        binding.reset.setOnClickListener { binding.clip.reset() }
        binding.cancel.setOnClickListener { dismiss() }
        binding.confirm.setOnClickListener {
            if (!isSaving) {
                isSaving = true
                binding.confirm.isEnabled = false
                binding.progress.visibility = View.VISIBLE
                binding.progress.load(R.drawable.icons8_monkey)
                it.context.saveImagesToAlbum(binding.clip.saveBitmap()) { path, uri ->
                    binding.confirm.isEnabled = true
                    binding.progress.visibility = View.GONE
                    binding.progress.dispose()
                    _callback?.onFinish(path, uri)
                    isSaving = false
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _callback = null
        _binding = null
    }

    fun setBitmapResource(bitmap: Bitmap): PictureClipDialog {
        this.bitmap = bitmap
        return this
    }

    fun setPictureClipCallback(callback: PictureClipCallback): PictureClipDialog {
        this._callback = callback
        return this
    }

}

interface PictureClipCallback {
    fun onFinish(path: String, uri: Uri)
}
