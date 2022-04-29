package com.example.miaow.picture.selector.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import coil.load
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.miaow.picture.databinding.PictureAlbumItemBinding
import com.example.miaow.picture.selector.bean.AlbumBean

class PictureAlbumAdapter : BaseAdapter<AlbumBean>() {

    private var selPosition = 0

    override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
        return PictureAlbumItemBinding::inflate
    }

    @SuppressLint("SetTextI18n")
    override fun onItemView(holder: ViewBindHolder, position: Int, item: AlbumBean) {
        val binding = holder.binding as PictureAlbumItemBinding
        binding.image.load(item.uri)
        binding.name.text = item.name
        binding.size.text = "(${item.size})"
        binding.selected.visibility = if (selPosition == position) View.VISIBLE else View.INVISIBLE
    }

    fun setSelectedPosition(position: Int) {
        val orgPosition = selPosition
        selPosition = position
        notifyItemChanged(orgPosition)
        notifyItemChanged(selPosition)
    }

}