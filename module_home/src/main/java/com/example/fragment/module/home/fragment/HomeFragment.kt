package com.example.fragment.module.home.fragment

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.fragment.library.base.utils.ImageLoader
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.home.R
import com.example.fragment.module.home.adapter.BannerAdapter
import com.example.fragment.module.home.databinding.FragmentHomeBinding
import com.example.fragment.module.home.model.HomeViewModel

class HomeFragment : ViewModelFragment<FragmentHomeBinding, HomeViewModel>() {

    private val bannerAdapter = BannerAdapter()

    override fun setViewBinding(inflater: LayoutInflater): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
        viewModel.getConfig()
        viewModel.getBanner()
    }

    private fun setupView() {
        binding.titleBar.setLeft(resId = R.drawable.ic_scan) {

        }
        binding.titleBar.setRight(resId = R.drawable.ic_search) {

        }
        binding.titleBar.setCenter(text = "首页")
        binding.banner.setOrientation(RecyclerView.HORIZONTAL)
        binding.banner.setAdapter(bannerAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>)
    }

    private fun update() {
        viewModel.configResult.observe(viewLifecycleOwner, {
            it.data?.apply {
                if (this.homeTitle != null) {
                    binding.titleBar.setCenter(text = this.homeTitle)
                }
                if (this.actionBarBgImageUrl != null) {
                    ImageLoader.with(binding.titleBar.context)
                        .load(this.actionBarBgImageUrl)
                        .override(binding.titleBar.width, binding.titleBar.height)
                        .into(object : ImageLoader.DrawableTarget {
                            override fun onResourceReady(resource: Drawable) {
                                binding.titleBar.setBgDrawable(resource)
                            }
                        })
                }
            }
        })
        viewModel.bannerResult.observe(viewLifecycleOwner, {
            it.data?.apply {
                bannerAdapter.setNewData(this)
            }
        })
    }

}