package com.example.miaow.picture.selector.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import coil.load
import com.example.miaow.base.adapter.BaseAdapter
import com.example.miaow.picture.databinding.PicturePreviewItemBinding
import com.example.miaow.picture.selector.bean.MediaBean

class PicturePreviewAdapter : BaseAdapter<MediaBean>() {

    override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
        return PicturePreviewItemBinding::inflate
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: MediaBean) {
        val binding = holder.binding as PicturePreviewItemBinding
        binding.picPreview.load(item.uri)
    }

}