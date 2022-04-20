package com.example.miaow.picture.selector.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.utils.MetricsUtils
import com.example.fragment.library.base.utils.load
import com.example.miaow.picture.databinding.PicturePreviewItemBinding
import com.example.miaow.picture.selector.bean.Album

class PicturePreviewAdapter : BaseAdapter<Album>() {

    private val selPosition: MutableList<Int> = ArrayList()

    override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
        return PicturePreviewItemBinding::inflate
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: Album) {
        val binding = holder.binding as PicturePreviewItemBinding
        binding.root.layoutParams.apply {
            height = MetricsUtils.screenWidth / 4
        }
        binding.image.load(item.path)
        if (selPosition.contains(position)) {
            binding.dim.alpha = 0.5f
            binding.serial.text = (selPosition.indexOf(position) + 1).toString()
            binding.originalBox.isSelected = true
        } else {
            binding.dim.alpha = if (selPosition.size < 9) 0f else 0.9f
            binding.serial.text = ""
            binding.originalBox.isSelected = false
        }
        binding.originalBox.setOnClickListener {
            if (selPosition.contains(position)) {
                selPosition.remove(position)
            } else if (selPosition.size < 9) {
                selPosition.add(position)
            }
            notifyDataSetChanged()
        }
    }

    fun setPreviewData(data: List<Album>? = null) {
        selPosition.clear()
        setNewData(data)
    }

    fun getSelectedImage(): List<Uri> {
        val selectedImage: MutableList<Uri> = ArrayList()
        selPosition.forEach {
            selectedImage.add(getItem(it).path)
        }
        return selectedImage
    }

}