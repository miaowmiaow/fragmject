package com.example.fragment.library.base.dialog

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.example.fragment.library.base.R
import com.example.fragment.library.base.picture.editor.PictureClipView
import java.io.File
import java.io.FileOutputStream

class PictureClipDialog : BaseDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): PictureClipDialog {
            return PictureClipDialog()
        }
    }

    private lateinit var rotate: ImageView
    private lateinit var reset: TextView
    private lateinit var cancel: ImageView
    private lateinit var confirm: ImageView
    private lateinit var clip: PictureClipView

    private var bitmap: Bitmap? = null

    private var onPictureClipListener: OnPictureClipListener? = null

    fun setOnPictureClipListener(onPictureClipListener: OnPictureClipListener): PictureClipDialog {
        this.onPictureClipListener = onPictureClipListener
        return this
    }

    fun setBitmapResource(bitmap: Bitmap): PictureClipDialog {
        this.bitmap = bitmap
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
        val view = inflater.inflate(R.layout.dialog_picture_clip, container, false)
        rotate = view.findViewById(R.id.rotate)
        reset = view.findViewById(R.id.reset)
        cancel = view.findViewById(R.id.cancel)
        confirm = view.findViewById(R.id.confirm)
        clip = view.findViewById(R.id.clip)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bitmap?.let {
            clip.setClipBitmapResource(it)
        }
        rotate.setOnClickListener { clip.rotate() }
        reset.setOnClickListener { clip.reset() }
        cancel.setOnClickListener { dismiss() }
        confirm.setOnClickListener {
            val cacheBmp = clip.conversionBitmap()
            var fos: FileOutputStream? = null
            var imagePath = ""
            try {
                // 判断手机设备是否有SD卡
                val isHasSDCard: Boolean = Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED
                )
                if (isHasSDCard) {
                    val moviesPath =
                        view.context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.absolutePath
                    val recordPath =
                        moviesPath + File.separator + System.currentTimeMillis() + ".png"
                    val file = File(recordPath)
                    fos = FileOutputStream(file)
                    imagePath = file.absolutePath
                } else throw Exception("创建文件失败!")
                cacheBmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
                fos.close()
            } catch (e: Exception) {
                e.printStackTrace()
                fos?.close()
            }
            onPictureClipListener?.onPicture(imagePath)
            dismiss()
        }
    }
}

interface OnPictureClipListener {
    fun onPicture(pathName: String)
}