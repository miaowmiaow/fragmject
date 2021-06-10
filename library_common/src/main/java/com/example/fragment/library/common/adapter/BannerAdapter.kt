package com.example.fragment.library.common.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.library.base.utils.ImageLoader
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.bean.BannerBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.databinding.ItemBannerBinding

class BannerAdapter : BaseAdapter<BannerBean>() {

    override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
        return ItemBannerBinding::inflate
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: BannerBean) {
        val binding = holder.binding as ItemBannerBinding
        if (item.imagePath.isNotEmpty()) {
            ImageLoader.with(binding.banner.context).load(item.imagePath).into(binding.banner)
        }
        val baseActivity: RouterActivity = contextToActivity(binding.root.context)
        binding.root.setOnClickListener {
            val args = Bundle()
            args.putString(Keys.URL, item.url)
            baseActivity.navigation(Router.WEB, args)
        }
    }

}