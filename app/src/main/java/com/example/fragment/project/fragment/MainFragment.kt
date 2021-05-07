package com.example.fragment.project.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fragment.library.base.component.activity.OnBackPressedListener
import com.example.fragment.library.base.utils.SimpleBannerHelper
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.home.fragment.SquareFragment
import com.example.fragment.project.R
import com.example.fragment.project.adapter.HotKeyAdapter
import com.example.fragment.project.databinding.FragmentMainBinding
import com.example.fragment.project.model.MainViewModel

class MainFragment : ViewModelFragment<FragmentMainBinding, MainViewModel>(),
    OnBackPressedListener {

    private lateinit var bannerHelper: SimpleBannerHelper

    private val hotKeyAdapter = HotKeyAdapter()

    private val fragments = arrayListOf(
        SquareFragment.newInstance(),
        WanAndroidFragment.newInstance()
    )

    override fun setViewBinding(inflater: LayoutInflater): FragmentMainBinding {
        return FragmentMainBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.apply {
            val lp = window.attributes
            lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN.inv()
            window.attributes = lp
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            window.decorView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.white
                )
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getBaseActivity().registerOnBackPressedListener(MainFragment::class.java.simpleName, this)
        setupView()
        update()
        viewModel.getHotKey()
    }

    override fun onResume() {
        super.onResume()
        bannerHelper.startTimerTask()
    }

    override fun onPause() {
        super.onPause()
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

    override fun onDestroyView() {
        super.onDestroyView()
        getBaseActivity().removerOnBackPressedListener(MainFragment::class.java.simpleName)
    }

    private fun setupView() {
        bannerHelper = SimpleBannerHelper(binding.hotKey, RecyclerView.VERTICAL)
        binding.hotKey.adapter = hotKeyAdapter
        binding.menu.setOnClickListener {
            if (binding.drawer.isDrawerOpen(GravityCompat.START)) {
                binding.drawer.closeDrawer(GravityCompat.START)
            } else {
                binding.drawer.openDrawer(GravityCompat.START)
            }
        }
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