package com.example.fragment.library.base.dialog

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import com.example.fragment.library.base.R
import com.example.fragment.library.base.databinding.DialogPictureEditorBinding
import com.example.fragment.library.base.picture.editor.PictureEditorView
import com.example.fragment.library.base.picture.editor.bean.EditorMode
import com.example.fragment.library.base.utils.AlbumUtil
import java.io.File
import java.io.FileOutputStream

class PictureEditorDialog : BaseDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): PictureEditorDialog {
            return PictureEditorDialog()
        }
    }

    private var _binding: DialogPictureEditorBinding? = null
    private val binding get() = _binding!!
    private val graffitiColors: MutableList<RelativeLayout> = arrayListOf()
    private val tools: MutableList<ImageView> = arrayListOf()
    private val colors = arrayListOf(
        Color.parseColor("#ffffff"),
        Color.parseColor("#000000"),
        Color.parseColor("#ff0000"),
        Color.parseColor("#ffb636"),
        Color.parseColor("#00FF00"),
        Color.parseColor("#508cee"),
        Color.parseColor("#7B68EE"),
    )

    private var bitmapPath: String = ""
    private var callback: EditorFinishCallback? = null

    fun setBitmapResource(bitmapPath: String): PictureEditorDialog {
        this.bitmapPath = bitmapPath
        return this
    }

    fun setEditorFinishCallback(callback: EditorFinishCallback): PictureEditorDialog {
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
        _binding = DialogPictureEditorBinding.inflate(inflater, container, false)
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
        binding.pictureEditor.setBitmapResource(bitmapPath)
        binding.pictureEditor.setOnStickerClickListener(object :
            PictureEditorView.OnStickerClickListener {
            override fun onStickerClick(
                bitmap: Bitmap?,
                contentDescription: String,
                parentTouchX: Float,
                parentTouchY: Float
            ) {
                pictureText(contentDescription, parentTouchX, parentTouchY)
            }
        })
        graffitiColors.add(binding.graffitiWhite)
        graffitiColors.add(binding.graffitiBlack)
        graffitiColors.add(binding.graffitiRed)
        graffitiColors.add(binding.graffitiYellow)
        graffitiColors.add(binding.graffitiGreen)
        graffitiColors.add(binding.graffitiBlue)
        graffitiColors.add(binding.graffitiPurple)
        tools.add(binding.graffiti)
        tools.add(binding.sticker)
        tools.add(binding.mosaic)
        tools.add(binding.screenshot)
        binding.back.setOnClickListener { dismiss() }
        binding.complete.setOnClickListener { view ->
            val cacheBmp = binding.pictureEditor.conversionBitmap()
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
        binding.colorUndo.setOnClickListener { binding.pictureEditor.graffitiUndo() }
        binding.mosaicUndo.setOnClickListener { binding.pictureEditor.mosaicUndo() }
        graffitiColors.forEachIndexed { index, view ->
            view.setOnClickListener {
                selectedColor(view)
                binding.pictureEditor.setGraffitiColor(colors[index])
            }
        }
        tools.forEachIndexed { index, view ->
            view.setOnClickListener {
                binding.colorBar.visibility = View.GONE
                binding.mosaicUndo.visibility = View.GONE
                binding.pictureEditor.setMode(EditorMode.STICKER)
                if (!view.isSelected) {
                    selectedTool(view)
                    when (index) {
                        0 -> {
                            binding.colorBar.visibility = View.VISIBLE
                            binding.pictureEditor.setMode(EditorMode.GRAFFITI)
                        }
                        1 -> {
                            pictureText("", 0f, 0f)
                        }
                        2 -> {
                            binding.mosaicUndo.visibility = View.VISIBLE
                            binding.pictureEditor.setMode(EditorMode.MOSAIC)
                        }
                        3 -> {
                            pictureClip(binding.pictureEditor.conversionBitmap())
                        }
                    }
                } else {
                    view.isSelected = false
                }
            }
        }
    }



    private fun pictureText(contentDescription: String, parentTouchX: Float, parentTouchY: Float) {
        PictureTextDialog.newInstance()
            .setText(contentDescription)
            .setTextFinishCallback(object : TextFinishCallback {
                override fun onFinish(bitmap: Bitmap, contentDescription: String) {
                    binding.pictureEditor.setStickerBitmap(
                        bitmap,
                        contentDescription,
                        parentTouchX,
                        parentTouchY
                    )
                }
            })
            .show(manager)
    }

    private fun pictureClip(clipBmp: Bitmap) {
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
        graffitiColors.forEach {
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