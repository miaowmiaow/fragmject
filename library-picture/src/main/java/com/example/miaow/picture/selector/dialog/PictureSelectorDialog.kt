package com.example.miaow.picture.selector.dialog

import android.content.ContentValues
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.fragment.library.base.R
import com.example.fragment.library.base.dialog.FullDialog
import com.example.fragment.library.base.dialog.PermissionDialog
import com.example.fragment.library.base.utils.CacheUtils
import com.example.fragment.library.base.utils.PermissionsCallback
import com.example.fragment.library.base.utils.requestCamera
import com.example.fragment.library.base.utils.requestMediaImages
import com.example.miaow.picture.databinding.PictureSelectorDialogBinding
import com.example.miaow.picture.selector.adapter.OnPictureClickListener
import com.example.miaow.picture.selector.adapter.PictureSelectorAdapter
import com.example.miaow.picture.selector.bean.MediaBean
import com.example.miaow.picture.selector.pop.PictureAlbumPopupWindow
import com.example.miaow.picture.selector.vm.PictureViewModel
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
    private val selectorAdapter = PictureSelectorAdapter()
    private var _callback: PictureSelectorCallback? = null
    private var _pictureAlbumPopupWindow: PictureAlbumPopupWindow? = null
    private val pictureAlbumPopupWindow get() = _pictureAlbumPopupWindow!!

    private var takePictureUri: Uri? = null

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            if (result) {
                val bean = MediaBean("拍照", takePictureUri())
                selectorAdapter.addData(1, listOf(bean))
                viewModel.updateMediaMap(bean)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PictureSelectorDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.apply {
            setDimAmount(0f)
            attributes.gravity = Gravity.END
            setWindowAnimations(R.style.AnimRight)
        }
        setStatusBar(binding.root, Color.parseColor("#555555"), false)
        _pictureAlbumPopupWindow = PictureAlbumPopupWindow(view.context)
        initView()
        initViewModel()
        initData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        setStatusBar(binding.root, Color.parseColor("#00000000"), false)
        binding.list.adapter = null
        pictureAlbumPopupWindow.onDestroy()
        _pictureAlbumPopupWindow = null
        _callback = null
        _binding = null
    }

    private fun initView() {
        binding.back.setOnClickListener { dismiss() }
        binding.config.setOnClickListener {
            _callback?.onSelectedData(selectorAdapter.getSelectPositionData())
            dismiss()
        }
        binding.album.setOnClickListener {
            val isAlbum = binding.albumBox.isSelected
            if (isAlbum) {
                pictureAlbumPopupWindow.dismiss()
            } else {
                pictureAlbumPopupWindow.show(binding.titleBar)
            }
            binding.albumBox.isSelected = !isAlbum
        }
        binding.preview.setOnClickListener {
            val data = selectorAdapter.getSelectPosition()
            if (data.isNotEmpty()) {
                PicturePreviewDialog.newInstance()
                    .setMode(PicturePreviewDialog.Mode.SELECT)
                    .setSelectedPosition(data)
                    .setPicturePreviewCallback(object : PicturePreviewCallback {
                        override fun onFinish(selectPosition: List<Int>) {
                            selectorAdapter.setAlbumData(viewModel.currAlbumResult.value)
                            selectorAdapter.setSelectPosition(selectPosition)
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
                takePicture()
            }

            override fun onSelectClick(position: Int) {
                PicturePreviewDialog.newInstance()
                    .setMode(PicturePreviewDialog.Mode.NORM)
                    .setSelectedPosition(selectorAdapter.getSelectPosition())
                    .setPreviewPosition(position)
                    .setPicturePreviewCallback(object : PicturePreviewCallback {
                        override fun onFinish(selectPosition: List<Int>) {
                            selectorAdapter.setAlbumData(viewModel.currAlbumResult.value)
                            selectorAdapter.setSelectPosition(selectPosition)
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
            pictureAlbumPopupWindow.let { popupWindow ->
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

    private fun initData() {
        if (viewModel.albumResult.value == null) {
            childFragmentManager.requestMediaImages(object : PermissionsCallback {
                override fun allow() {
                    viewModel.queryAlbum(requireActivity())
                }

                override fun deny() {
                    PermissionDialog.alert(requireActivity(), "照片")
                }
            })
        }
    }

    fun setPictureSelectorCallback(callback: PictureSelectorCallback): PictureSelectorDialog {
        _callback = callback
        return this
    }

    /**
     * 拍照的方法
     */
    private fun takePicture() {
        childFragmentManager.requestCamera(object : PermissionsCallback {
            override fun allow() {
                takePicture.launch(takePictureUri())
            }

            override fun deny() {
                PermissionDialog.alert(requireActivity(), "相机")
            }
        })
    }

    private fun takePictureUri(): Uri {
        val pictureName = "${System.currentTimeMillis()}.png"
        return takePictureUri ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, pictureName)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            requireContext().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: Uri.EMPTY
        } else {
            val cachePath = CacheUtils.getDirPath(requireContext(), Environment.DIRECTORY_PICTURES)
            val imageFile = File(cachePath, pictureName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //适配Android 7.0文件权限，通过FileProvider创建一个content类型的Uri
                val authority = "${requireContext().packageName}.FileProvider"
                FileProvider.getUriForFile(requireContext(), authority, imageFile)
            } else {
                Uri.fromFile(imageFile)
            }
        }.also {
            takePictureUri = it
        }
    }
}

interface PictureSelectorCallback {
    fun onSelectedData(data: List<MediaBean>)
}