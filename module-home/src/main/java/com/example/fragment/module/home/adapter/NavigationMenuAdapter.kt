package com.example.fragment.module.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.module.home.R
import com.example.fragment.module.home.bean.NavigationBean
import com.example.fragment.module.home.databinding.ItemNavigationMenuBinding

class NavigationMenuAdapter : BaseAdapter<NavigationBean>() {

    override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
        return ItemNavigationMenuBinding::inflate
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: NavigationBean) {
        val binding = holder.binding as ItemNavigationMenuBinding
        binding.tv.text = item.name
        holder.itemView.background = if (item.isSelected) {
            ContextCompat.getDrawable(holder.itemView.context, R.drawable.layer_while_item_top)
        } else {
            ContextCompat.getDrawable(holder.itemView.context, R.drawable.layer_gray_item_top)
        }
    }

}