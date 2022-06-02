package com.example.miaow.picture.selector.dialog

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.fragment.library.base.R
import com.example.fragment.library.base.dialog.FullDialog
import com.example.fragment.library.base.utils.ActivityCallback
import com.example.fragment.library.base.utils.startForResult
import com.example.miaow.picture.databinding.PictureSelectorDialogBinding
import com.example.miaow.picture.selector.adapter.OnPictureClickListener
import com.example.miaow.picture.selector.adapter.PictureSelectorAdapter
import com.example.miaow.picture.selector.bean.MediaBean
import com.example.miaow.picture.selector.model.PictureViewModel
import com.example.miaow.picture.selector.pop.PictureAlbumPopupWindow
import com.example.miaow.picture.utils.saveSystemAlbum
import java.io.File

class PictureSelectorDialog : FullDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): PictureSelectorDialog {
            return PictureSelectorDialog()
        }
    }

    private val viewModel: PictureViewModel by activityViewModels()
    private var _binding: PictureSelectorDialogBinding? = null
    private val binding get() = _binding!!
    private var pictureAlbumPopupWindow: PictureAlbumPopupWindow? = null
    private val selectorAdapter = PictureSelectorAdapter()

    private var pictureSelectorCallback: PictureSelectorCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PictureSelectorDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        setStatusBar(binding.root, Color.parseColor("#00000000"), false)
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.apply {
            setDimAmount(0f)
            attributes.gravity = Gravity.END
            setWindowAnimations(R.style.AnimRight)
        }
        setStatusBar(binding.root, Color.parseColor("#555555"), false)
        pictureAlbumPopupWindow = PictureAlbumPopupWindow(view.context)
        initView()
        initViewModel()
        initData(view.context)
    }

    private fun initView() {
        binding.back.setOnClickListener { dismiss() }
        binding.config.setOnClickListener {
            pictureSelectorCallback?.onSelectedData(selectorAdapter.getSelectPositionData())
            dismiss()
        }
        binding.album.setOnClickListener {
            val isAlbum = binding.albumBox.isSelected
            if (isAlbum) {
                pictureAlbumPopupWindow?.dismiss()
            } else {
                pictureAlbumPopupWindow?.show(binding.titleBar)
            }
            binding.albumBox.isSelected = !isAlbum
        }
        binding.preview.setOnClickListener {
            val data = selectorAdapter.getSelectPosition()
            if (data.isNotEmpty()) {
                PicturePreviewDialog.newInstance()
                    .setSelectedPosition(data)
                    .setPicturePreviewCallback(
                        object : PicturePreviewDialog.PicturePreviewCallback {
                            override fun onSelectedPosition(data: List<Int>) {
                                selectorAdapter.setSelectPosition(data)
                            }
                        })
                    .show(childFragmentManager)
            } else {
                Toast.makeText(it.context, "请至少选择一张图片", Toast.LENGTH_SHORT).show()
            }
        }
        binding.list.layoutManager = GridLayoutManager(binding.list.context, 4)
        binding.list.adapter = selectorAdapter
        selectorAdapter.setOnPictureClickListener(object : OnPictureClickListener {
            override fun onCamera() {
                requireActivity().takePicture()
            }

            override fun onSelectClick(position: Int) {
                PicturePreviewDialog.newInstance()
                    .setSelectedPosition(selectorAdapter.getSelectPosition(), position)
                    .setPicturePreviewCallback(
                        object : PicturePreviewDialog.PicturePreviewCallback {
                            override fun onSelectedPosition(data: List<Int>) {
                                selectorAdapter.setSelectPosition(data)
                            }
                        })
                    .show(childFragmentManager)
            }
        })
    }

    private fun initViewModel() {
        viewModel.currAlbumResult.observe(viewLifecycleOwner) {
            selectorAdapter.setAlbumData(it)
        }
        viewModel.albumResult.observe(viewLifecycleOwner) { result ->
            pictureAlbumPopupWindow?.let { popupWindow ->
                binding.albumName.text = result[0].name
                popupWindow.setAlbumData(result, 0)
                popupWindow.setOnAlbumSelectedListener(
                    object : PictureAlbumPopupWindow.OnAlbumSelectedListener {
                        override fun onAlbumSelected(name: String) {
                            binding.albumName.text = name
                            viewModel.updateCurrAlbum(name)
                        }
                    })
                popupWindow.setOnDismissListener {
                    binding.albumBox.isSelected = false
                }
            }
        }
    }

    private fun initData(context: Context) {
        if (viewModel.albumResult.value == null) {
            viewModel.queryAlbum(context)
        }
    }

    /**
     * 拍照的方法
     */
    private fun FragmentActivity.takePicture() {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            return
        }
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 适配android 10
            val url = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            contentResolver.insert(url, ContentValues())?.let {
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, it)
                startForResult(intent, object : ActivityCallback {
                    override fun onActivityResult(resultCode: Int, data: Intent?) {
                        val bean = MediaBean("拍照", it)
                        selectorAdapter.addData(1, listOf(bean))
                        viewModel.updateMediaMap(bean)
                    }
                })
            }
        } else {
            File(externalCacheDir, "wan").let { parentFile ->
                parentFile.mkdirs()
                val imageFile = File(parentFile, "${System.currentTimeMillis()}.png")
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    //适配Android 7.0文件权限，通过FileProvider创建一个content类型的Uri
                    val authority = "${packageName}.FileProvider"
                    FileProvider.getUriForFile(this, authority, imageFile)
                } else {
                    Uri.fromFile(imageFile)
                }
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                startForResult(intent, object : ActivityCallback {
                    override fun onActivityResult(resultCode: Int, data: Intent?) {
                        saveSystemAlbum(BitmapFactory.decodeFile(imageFile.absolutePath)) { _, _ ->
                            val bean = MediaBean("拍照", uri)
                            selectorAdapter.addData(1, listOf(bean))
                            viewModel.updateMediaMap(bean)
                        }
                    }
                })
            }
        }
    }

    fun setPictureSelectorCallback(callback: PictureSelectorCallback): PictureSelectorDialog {
        pictureSelectorCallback = callback
        return this
    }

    interface PictureSelectorCallback {
        fun onSelectedData(data: List<MediaBean>)
    }

}