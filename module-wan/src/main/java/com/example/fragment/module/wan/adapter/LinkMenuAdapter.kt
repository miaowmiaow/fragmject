package com.example.fragment.module.wan.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.common.bean.NavigationBean
import com.example.fragment.module.wan.R
import com.example.fragment.module.wan.databinding.NavigationMenuItemBinding

class LinkMenuAdapter : BaseAdapter<NavigationBean>() {

    override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
        return NavigationMenuItemBinding::inflate
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: NavigationBean) {
        val binding = holder.binding as NavigationMenuItemBinding
        binding.tv.text = item.name
        binding.bg.setBackgroundResource(
            if (item.isSelected) R.drawable.layer_while_item_bottom else R.drawable.layer_gray_item_bottom
        )
    }

}