package com.example.fragment.library.common.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.viewbinding.ViewBinding
import coil.load
import com.example.fragment.library.base.adapter.BaseAdapter
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
            binding.banner.load(item.imagePath)
        }
        val baseActivity: RouterActivity = contextToActivity(binding.root.context)
        binding.root.setOnClickListener {
            val url = Uri.encode(item.url)
            baseActivity.navigation(Router.WEB, bundleOf(Keys.URL to url))
        }
    }

}