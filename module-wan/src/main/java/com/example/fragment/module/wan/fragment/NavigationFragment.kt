package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fragment.library.base.adapter.FragmentPagerAdapter
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.databinding.TabItemTopBinding
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.databinding.FragmentNavigationBinding

class NavigationFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): NavigationFragment {
            return NavigationFragment()
        }
    }

    private var _binding: FragmentNavigationBinding? = null
    private val binding get() = _binding!!

    private val tabTexts = arrayOf("导航", "体系")
    private val fragments = arrayListOf(
        NavigationLinkFragment.newInstance(),
        NavigationSystemFragment.newInstance()
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNavigationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        //TabBar与ViewPager
        binding.viewpager.adapter = FragmentPagerAdapter(childFragmentManager, fragments)
        binding.tabBar.setupWithViewPager(binding.viewpager)
        var currentItem = binding.tabBar.selectedTabPosition
        if (currentItem == -1) currentItem = 0
        binding.tabBar.removeAllTabs()
        for (i in tabTexts.indices) {
            val item = TabItemTopBinding.inflate(LayoutInflater.from(binding.root.context))
            item.tab.text = tabTexts[i]
            binding.tabBar.addTab(binding.tabBar.newTab().setCustomView(item.root))
        }
        binding.viewpager.currentItem = currentItem
    }

    override fun initViewModel(): BaseViewModel? {
        return null
    }

    override fun initLoad() {}

}