package com.example.miaow.picture.selector.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.utils.load
import com.example.miaow.picture.databinding.PictureBucketItemBinding
import com.example.miaow.picture.selector.bean.Bucket

class PictureBucketAdapter : BaseAdapter<Bucket>() {

    private var selPosition = 0

    override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
        return PictureBucketItemBinding::inflate
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: Bucket) {
        val binding = holder.binding as PictureBucketItemBinding
        binding.image.load(item.coverPath)
        binding.name.text = item.name
        binding.size.text = "(${item.size})"
        binding.selected.visibility = if (selPosition == position) View.VISIBLE else View.INVISIBLE
    }

    fun setSelectedPosition(position: Int) {
        selPosition = position
        notifyDataSetChanged()
    }

}