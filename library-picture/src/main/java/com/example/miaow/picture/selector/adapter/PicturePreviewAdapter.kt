package com.example.miaow.picture.selector.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import coil.load
import com.example.miaow.base.adapter.BaseAdapter
import com.example.miaow.picture.databinding.PicturePreviewItemBinding
import com.example.miaow.picture.selector.bean.MediaBean
import com.example.miaow.picture.selector.view.PhotoView

class PicturePreviewAdapter : BaseAdapter<MediaBean>() {

    override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
        return PicturePreviewItemBinding::inflate
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: MediaBean) {
        val binding = holder.binding as PicturePreviewItemBinding
        binding.picPreview.load(item.uri)
        binding.picPreview.setOnPhotoClickListener(object : PhotoView.OnPhotoClickListener {
            override fun onClick(view: View) {
                listener?.onClick(view)
            }
        })
    }

    private var listener: OnPicturePreviewClickListener? = null

    fun setOnPicturePreviewClickListener(listener: OnPicturePreviewClickListener) {
        this.listener = listener
    }

    interface OnPicturePreviewClickListener {
        fun onClick(view: View)
    }
}