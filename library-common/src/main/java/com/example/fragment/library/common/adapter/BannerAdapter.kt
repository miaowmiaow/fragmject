package com.example.fragment.library.common.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.utils.MetricsUtils
import com.example.fragment.library.base.utils.loadRoundedCorners
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.bean.BannerBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.databinding.BannerItemBinding

class BannerAdapter : BaseAdapter<BannerBean>() {

    override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
        return BannerItemBinding::inflate
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: BannerBean) {
        val binding = holder.binding as BannerItemBinding
        binding.root.layoutParams.apply {
            width = MetricsUtils.screenWidth - MetricsUtils.dp2px(60f).toInt()
        }
        if (item.imagePath.isNotEmpty()) {
            binding.banner.loadRoundedCorners(item.imagePath, 15f)
        }
        binding.root.setOnClickListener {
            val baseActivity: RouterActivity = contextToActivity(binding.root.context)
            val url = Uri.encode(item.url)
            baseActivity.navigation(Router.WEB, bundleOf(Keys.URL to url))
        }
    }

}