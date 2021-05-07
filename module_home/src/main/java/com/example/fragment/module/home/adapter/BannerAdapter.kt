package com.example.fragment.module.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.library.base.utils.ImageLoader
import com.example.fragment.module.home.bean.BannerDataBean
import com.example.fragment.module.home.databinding.ItemBannerBinding

class BannerAdapter : BaseAdapter<BannerDataBean>() {

    override fun onCreateViewBinding(parent: ViewGroup, viewType: Int): ViewBinding {
        return ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }


    override fun onItemView(holder: ViewBindHolder, position: Int, item: BannerDataBean) {
        val binding = holder.binding as ItemBannerBinding
        item.imagePath?.let {
            ImageLoader.with(binding.banner.context).load(it).into(binding.banner)
        }
    }

}