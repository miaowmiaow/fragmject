package com.example.miaow.picture.selector.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.utils.MetricsUtils
import com.example.fragment.library.base.utils.load
import com.example.miaow.picture.databinding.PictureSelectorItemBinding
import com.example.miaow.picture.selector.bean.MediaBean

class PictureSelectorAdapter : BaseAdapter<MediaBean>() {

    private val selectPosition: MutableList<Int> = ArrayList()

    override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
        return PictureSelectorItemBinding::inflate
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: MediaBean) {
        val binding = holder.binding as PictureSelectorItemBinding
        binding.root.layoutParams.apply {
            height = MetricsUtils.screenWidth / 4
        }
        binding.image.load(item.uri)
        if (selectPosition.contains(position)) {
            if (!selectPosition.contains(position)) {
                selectPosition.add(position)
            }
            binding.dim.alpha = 0.5f
            binding.serial.text = (selectPosition.indexOf(position) + 1).toString()
            binding.originalBox.isSelected = true
        } else {
            if (selectPosition.contains(position)) {
                selectPosition.remove(position)
            }
            binding.dim.alpha = if (selectPosition.size < 9) 0f else 0.9f
            binding.serial.text = ""
            binding.originalBox.isSelected = false
        }
        binding.originalBox.setOnClickListener {
            if (selectPosition.contains(position)) {
                selectPosition.remove(position)
            } else if (selectPosition.size < 9) {
                selectPosition.add(position)
            }
            notifyDataSetChanged()
        }
    }

    fun setAlbumData(data: List<MediaBean>) {
        selectPosition.clear()
        setNewData(data)
    }

    fun setSelectPosition(data: List<Int>) {
        selectPosition.clear()
        selectPosition.addAll(data)
        notifyDataSetChanged()
    }

    fun getSelectPositionData(): List<Int> {
        return selectPosition
    }

}