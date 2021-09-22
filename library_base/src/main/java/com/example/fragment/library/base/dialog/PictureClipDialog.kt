package com.example.fragment.library.base.dialog

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.view.*
import com.example.fragment.library.base.R
import com.example.fragment.library.base.databinding.DialogPictureClipBinding
import com.example.fragment.library.base.utils.AlbumUtil
import java.io.File
import java.io.FileOutputStream

class PictureClipDialog : BaseDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): PictureClipDialog {
            return PictureClipDialog()
        }
    }

    private var _binding: DialogPictureClipBinding? = null
    private val binding get() = _binding!!

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
        _binding = DialogPictureClipBinding.inflate(inflater, container, false)
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
        binding.clip.setClipBitmapResource(bitmap)
        binding.rotate.setOnClickListener { binding.clip.rotate() }
        binding.reset.setOnClickListener { binding.clip.reset() }
        binding.cancel.setOnClickListener { dismiss() }
        binding.confirm.setOnClickListener { view ->
            val cacheBmp = binding.clip.conversionBitmap()
            val type = Environment.DIRECTORY_MOVIES
            val currentTimeMillis = System.currentTimeMillis()
            var fos: FileOutputStream? = null
            try {
                if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                    val moviesPath = view.context.getExternalFilesDir(type)?.absolutePath
                    val recordPath = moviesPath + File.separator + currentTimeMillis + ".png"
                    val file = File(recordPath)
                    fos = FileOutputStream(file)
                    cacheBmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.flush()
                    context?.let {
                        AlbumUtil.saveSystemAlbum(it, file) { path ->
                            callback?.onFinish(path)
                            dismiss()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                fos?.close()
            }
        }
    }
}

interface ClipFinishCallback {
    fun onFinish(path: String)
}