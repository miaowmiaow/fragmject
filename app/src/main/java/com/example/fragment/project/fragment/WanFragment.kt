package com.example.fragment.project.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.fragment.library.base.adapter.BaseViewPagerAdapter
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.home.fragment.*
import com.example.fragment.project.R
import com.example.fragment.project.databinding.FragmentWanBinding
import com.google.android.material.tabs.TabLayout

class WanFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): WanFragment {
            return WanFragment()
        }
    }

    private val tabTexts = arrayOf("首页", "导航", "问答", "体系", "项目")
    private val tabDrawable = intArrayOf(
        R.drawable.ic_bottom_bar_home,
        R.drawable.ic_bottom_bar_navigation,
        R.drawable.ic_bottom_bar_faq,
        R.drawable.ic_bottom_bar_system,
        R.drawable.ic_bottom_bar_project
    )
    private val fragments = arrayListOf(
        HomeFragment.newInstance(),
        NavigationFragment.newInstance(),
        FAQFragment.newInstance(),
        SystemFragment.newInstance(),
        ProjectListFragment.newInstance()
    )

    private var _binding: FragmentWanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewpager.offscreenPageLimit = 4
        binding.viewpager.adapter = BaseViewPagerAdapter(childFragmentManager, fragments)
        binding.tab.setupWithViewPager(binding.viewpager)
        binding.tab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.customView?.apply {
                    val icon = findViewById<ImageView>(R.id.iv_tab_icon)
                    val text = findViewById<TextView>(R.id.tv_tab_name)
                    icon.setColorFilter(ContextCompat.getColor(icon.context, R.color.text_fff))
                    text.setTextColor(ContextCompat.getColor(text.context, R.color.text_fff))
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.customView?.apply {
                    val icon = findViewById<ImageView>(R.id.iv_tab_icon)
                    val text = findViewById<TextView>(R.id.tv_tab_name)
                    icon.setColorFilter(ContextCompat.getColor(icon.context, R.color.gray_alpha))
                    text.setTextColor(ContextCompat.getColor(text.context, R.color.gray_alpha))
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
        binding.tab.removeAllTabs()
        for (i in fragments.indices) {
            val layoutInflater = LayoutInflater.from(binding.root.context)
            val tabView: View = layoutInflater.inflate(R.layout.item_tab_main, null)
            val imgTab = tabView.findViewById<ImageView>(R.id.iv_tab_icon)
            val txtTab = tabView.findViewById<TextView>(R.id.tv_tab_name)
            imgTab.setImageDrawable(ContextCompat.getDrawable(imgTab.context, tabDrawable[i]))
            imgTab.setColorFilter(ContextCompat.getColor(imgTab.context, R.color.gray_alpha))
            txtTab.setTextColor(ContextCompat.getColor(txtTab.context, R.color.gray_alpha))
            txtTab.text = tabTexts[i]
            val tab = binding.tab.newTab()
            tab.customView = tabView
            binding.tab.addTab(tab)
        }
        binding.viewpager.currentItem = savedInstanceState?.getInt("WAN_CURRENT_POSITION") ?: 0
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("WAN_CURRENT_POSITION", binding.viewpager.currentItem)
    }

}