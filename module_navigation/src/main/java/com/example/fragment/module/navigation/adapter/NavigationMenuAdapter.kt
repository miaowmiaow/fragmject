package com.example.fragment.module.navigation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.module.navigation.bean.NavigationBean
import com.example.fragment.module.navigation.databinding.ItemNavigationMenuBinding

class NavigationMenuAdapter : BaseAdapter<NavigationBean>() {

    override fun onCreateViewBinding(parent: ViewGroup, viewType: Int): ViewBinding {
        return ItemNavigationMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: NavigationBean) {
        val binding = holder.binding as ItemNavigationMenuBinding
        binding.tv.text = item.name
        binding.tv.isSelected = item.isSelected
    }

}