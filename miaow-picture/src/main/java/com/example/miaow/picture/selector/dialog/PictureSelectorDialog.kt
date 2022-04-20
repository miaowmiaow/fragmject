package com.example.miaow.picture.selector.dialog

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.fragment.library.base.R
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.dialog.FullDialog
import com.example.miaow.picture.databinding.PictureSelectorDialogBinding
import com.example.miaow.picture.selector.adapter.PictureSelectorAdapter
import com.example.miaow.picture.selector.bean.MediaBean
import com.example.miaow.picture.selector.model.PictureViewModel
import com.example.miaow.picture.selector.pop.PictureAlbumPopupWindow

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
                    .setPicturePreviewCallback(object : PicturePreviewCallback {
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
        selectorAdapter.setOnItemClickListener(object : BaseAdapter.OnItemClickListener {
            override fun onItemClick(holder: BaseAdapter.ViewBindHolder, position: Int) {
                PicturePreviewDialog.newInstance()
                    .setSelectedPosition(selectorAdapter.getSelectPosition(), position)
                    .setPicturePreviewCallback(object : PicturePreviewCallback {
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
        viewModel.albumResult.observe(viewLifecycleOwner) {
            pictureAlbumPopupWindow?.let { popupWindow ->
                popupWindow.setAlbumData(it, 0)
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

    fun setPictureSelectorCallback(callback: PictureSelectorCallback): PictureSelectorDialog {
        pictureSelectorCallback = callback
        return this
    }

}

interface PictureSelectorCallback {
    fun onSelectedData(data: List<MediaBean>)
}