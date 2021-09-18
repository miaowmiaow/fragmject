package com.example.fragment.library.base.dialog

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.example.fragment.library.base.R
import com.example.fragment.library.base.picture.editor.PictureEditorView
import com.example.fragment.library.base.picture.editor.bean.Mode
import java.io.File
import java.io.FileOutputStream

class PictureEditorDialog : BaseDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): PictureEditorDialog {
            return PictureEditorDialog()
        }
    }

    private lateinit var back: ImageView

    private lateinit var pictureEditorView: PictureEditorView

    private lateinit var colorBar: LinearLayout
    private lateinit var graffitiWhite: RelativeLayout
    private lateinit var graffitiBlack: RelativeLayout
    private lateinit var graffitiRed: RelativeLayout
    private lateinit var graffitiYellow: RelativeLayout
    private lateinit var graffitiGreen: RelativeLayout
    private lateinit var graffitiBlue: RelativeLayout
    private lateinit var graffitiPurple: RelativeLayout
    private lateinit var colorUndo: RelativeLayout

    private lateinit var mosaicUndo: RelativeLayout

    private lateinit var graffiti: ImageView
    private lateinit var sticker: ImageView
    private lateinit var mosaic: ImageView
    private lateinit var screenshot: ImageView
    private lateinit var complete: RelativeLayout

    private val colors = arrayListOf(
        Color.parseColor("#ffffff"),
        Color.parseColor("#000000"),
        Color.parseColor("#ff0000"),
        Color.parseColor("#ffb636"),
        Color.parseColor("#00FF00"),
        Color.parseColor("#508cee"),
        Color.parseColor("#7B68EE"),
    )

    private val graffitiColors: MutableList<RelativeLayout> = arrayListOf()
    private val tools: MutableList<ImageView> = arrayListOf()

    private var pathName: String = ""

    private var onPictureEditorListener: OnPictureEditorListener? = null

    fun setOnPictureEditorListener(onPictureEditorListener: OnPictureEditorListener): PictureEditorDialog {
        this.onPictureEditorListener = onPictureEditorListener
        return this
    }

    fun setBitmapResource(pathName: String): PictureEditorDialog {
        this.pathName = pathName
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
        val view = inflater.inflate(R.layout.dialog_picture_editor, container, false)
        back = view.findViewById(R.id.back)

        pictureEditorView = view.findViewById(R.id.multi_layer)

        colorBar = view.findViewById(R.id.color_bar)
        graffitiWhite = view.findViewById(R.id.graffiti_white)
        graffitiBlack = view.findViewById(R.id.graffiti_black)
        graffitiRed = view.findViewById(R.id.graffiti_red)
        graffitiYellow = view.findViewById(R.id.graffiti_yellow)
        graffitiGreen = view.findViewById(R.id.graffiti_green)
        graffitiBlue = view.findViewById(R.id.graffiti_blue)
        graffitiPurple = view.findViewById(R.id.graffiti_purple)
        colorUndo = view.findViewById(R.id.color_undo)

        mosaicUndo = view.findViewById(R.id.mosaic_undo)

        graffiti = view.findViewById(R.id.graffiti)
        sticker = view.findViewById(R.id.sticker)
        mosaic = view.findViewById(R.id.mosaic)
        screenshot = view.findViewById(R.id.screenshot)
        complete = view.findViewById(R.id.complete)

        graffitiColors.add(graffitiWhite)
        graffitiColors.add(graffitiBlack)
        graffitiColors.add(graffitiRed)
        graffitiColors.add(graffitiYellow)
        graffitiColors.add(graffitiGreen)
        graffitiColors.add(graffitiBlue)
        graffitiColors.add(graffitiPurple)

        tools.add(graffiti)
        tools.add(sticker)
        tools.add(mosaic)
        tools.add(screenshot)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pictureEditorView.setBitmapResource(pathName)
        pictureEditorView.setOnStickerClickListener(object : PictureEditorView.OnStickerClickListener {
            override fun onStickerClick(
                bitmap: Bitmap?,
                contentDescription: String?,
                parentTouchX: Float,
                parentTouchY: Float
            ) {
                TextDialog.newInstance()
                    .setText(contentDescription)
                    .setOnTextFinishListener(object : OnTextFinishListener {
                        override fun onFinish(bitmap: Bitmap, contentDescription: String) {
                            pictureEditorView.setStickerBitmap(
                                bitmap,
                                contentDescription,
                                parentTouchX,
                                parentTouchY
                            )
                        }
                    })
                    .show(manager)
                pictureEditorView.setMode(Mode.STICKER)
            }
        })
        back.setOnClickListener { dismiss() }
        complete.setOnClickListener {
            val cacheBmp = pictureEditorView.conversionBitmap()
            var fos: FileOutputStream? = null
            var imagePath = ""
            try {
                if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
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
            onPictureEditorListener?.onPicture(imagePath)
            dismiss()
        }

        graffitiWhite.setOnClickListener {
            pictureEditorView.setGraffitiColor(colors[0])
            selectedColor(it)
        }
        graffitiBlack.setOnClickListener {
            pictureEditorView.setGraffitiColor(colors[1])
            selectedColor(it)
        }
        graffitiRed.setOnClickListener {
            pictureEditorView.setGraffitiColor(colors[2])
            selectedColor(it)
        }
        graffitiYellow.setOnClickListener {
            pictureEditorView.setGraffitiColor(colors[3])
            selectedColor(it)
        }
        graffitiGreen.setOnClickListener {
            pictureEditorView.setGraffitiColor(colors[4])
            selectedColor(it)
        }
        graffitiBlue.setOnClickListener {
            pictureEditorView.setGraffitiColor(colors[5])
            selectedColor(it)
        }
        graffitiPurple.setOnClickListener {
            pictureEditorView.setGraffitiColor(colors[6])
            selectedColor(it)
        }
        colorUndo.setOnClickListener { pictureEditorView.graffitiUndo() }
        mosaicUndo.setOnClickListener { pictureEditorView.mosaicUndo() }

        graffiti.setOnClickListener {
            mosaicUndo.visibility = View.GONE
            if (!it.isSelected) {
                pictureEditorView.setMode(Mode.GRAFFITI)
                colorBar.visibility = View.VISIBLE
                selectedTool(it)
            } else {
                pictureEditorView.setMode(Mode.STICKER)
                colorBar.visibility = View.GONE
                it.isSelected = false
            }
        }
        sticker.setOnClickListener {
            colorBar.visibility = View.GONE
            mosaicUndo.visibility = View.GONE
            selectedTool(it)
            TextDialog.newInstance()
                .setOnTextFinishListener(object : OnTextFinishListener {
                    override fun onFinish(bitmap: Bitmap, contentDescription: String) {
                        pictureEditorView.setStickerBitmap(bitmap, contentDescription, 0f, 0f)
                    }
                })
                .show(manager)
            pictureEditorView.setMode(Mode.STICKER)
        }
        mosaic.setOnClickListener {
            colorBar.visibility = View.GONE
            if (!it.isSelected) {
                pictureEditorView.setMode(Mode.MOSAIC)
                mosaicUndo.visibility = View.VISIBLE
                selectedTool(it)
            } else {
                pictureEditorView.setMode(Mode.STICKER)
                mosaicUndo.visibility = View.GONE
                it.isSelected = false
            }
        }
        screenshot.setOnClickListener {
            colorBar.visibility = View.GONE
            mosaicUndo.visibility = View.GONE
            selectedTool(it)
            val cacheBmp = pictureEditorView.conversionBitmap()
            PictureClipDialog.newInstance()
                .setBitmapResource(cacheBmp)
                .setOnPictureClipListener(object : OnPictureClipListener {
                    override fun onPicture(pathName: String) {
                        onPictureEditorListener?.onPicture(pathName)
                        dismiss()
                    }
                })
                .show(manager)
        }
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

interface OnPictureEditorListener {
    fun onPicture(pathName: String)
}