package com.example.fragment.library.common.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.library.base.utils.ImageLoader
import com.example.fragment.library.common.bean.BannerBean
import com.example.fragment.library.common.databinding.ItemBannerBinding

class BannerAdapter : BaseAdapter<BannerBean>() {

    override fun onCreateViewBinding(parent: ViewGroup, viewType: Int): ViewBinding {
        return ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }


    override fun onItemView(holder: ViewBindHolder, position: Int, item: BannerBean) {
        val binding = holder.binding as ItemBannerBinding
        if(item.imagePath.isNotEmpty()){
            ImageLoader.with(binding.banner.context).load(item.imagePath).into(binding.banner)
        }
    }

}