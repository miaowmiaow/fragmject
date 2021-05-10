package com.example.fragment.project.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fragment.library.base.component.activity.OnBackPressedListener
import com.example.fragment.library.base.utils.SimpleBannerHelper
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.home.fragment.SquareFragment
import com.example.fragment.project.adapter.HotKeyAdapter
import com.example.fragment.project.databinding.FragmentMainBinding
import com.example.fragment.project.model.MainViewModel

class MainFragment : ViewModelFragment<FragmentMainBinding, MainViewModel>(),
    OnBackPressedListener {

    private lateinit var bannerHelper: SimpleBannerHelper

    private val hotKeyAdapter = HotKeyAdapter()

    private val fragments = arrayListOf(
        SquareFragment.newInstance(),
        WanFragment.newInstance()
    )

    override fun setViewBinding(inflater: LayoutInflater): FragmentMainBinding {
        return FragmentMainBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
        viewModel.getHotKey()
    }

    override fun onResume() {
        super.onResume()
        getRouterActivity().registerOnBackPressedListener(this::class.java.simpleName, this)
        bannerHelper.startTimerTask()
    }

    override fun onPause() {
        super.onPause()
        getRouterActivity().removerOnBackPressedListener(this::class.java.simpleName)
        bannerHelper.stopTimerTask()
    }

    override fun onBackPressed(): Boolean {
        return if (binding.viewpager.currentItem == 1) {
            false
        } else {
            binding.viewpager.currentItem = 1
            true
        }
    }

    private fun setupView() {
        binding.menu.setOnClickListener {
            if (binding.drawer.isDrawerOpen(GravityCompat.START)) {
                binding.drawer.closeDrawer(GravityCompat.START)
            } else {
                binding.drawer.openDrawer(GravityCompat.START)
            }
        }
        binding.points.setOnClickListener { }
        binding.share.setOnClickListener { }
        binding.collection.setOnClickListener { }
        binding.setting.setOnClickListener { }
        binding.feedback.setOnClickListener { }
        bannerHelper = SimpleBannerHelper(binding.hotKey, RecyclerView.VERTICAL)
        binding.hotKey.adapter = hotKeyAdapter
        binding.viewpager.offscreenPageLimit = 1
        binding.viewpager.adapter = object :
            FragmentPagerAdapter(childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

            override fun getItem(position: Int): Fragment {
                return fragments[position]
            }

            override fun getCount(): Int {
                return fragments.size
            }
        }
        binding.viewpager.currentItem = 1
    }

    private fun update() {
        viewModel.hotKeyResult.observe(viewLifecycleOwner, { result ->
            result.data?.apply {
                hotKeyAdapter.setNewData(this)
                bannerHelper.startTimerTask()
            }
        })
    }

}