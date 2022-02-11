package com.example.fragment.library.common.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.common.bean.HotKeyBean
import com.example.fragment.library.common.databinding.HotKeyItemBinding

class HotKeyAdapter : BaseAdapter<HotKeyBean>() {

    override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
        return HotKeyItemBinding::inflate
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: HotKeyBean) {
        val binding = holder.binding as HotKeyItemBinding
        binding.hotKey.text = item.name
    }

}