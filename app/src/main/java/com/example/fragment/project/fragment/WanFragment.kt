package com.example.fragment.project.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.example.fragment.library.base.component.view.SimpleTabLayout
import com.example.fragment.library.common.fragment.ViewBindingFragment
import com.example.fragment.module.faq.fragment.FAQFragment
import com.example.fragment.module.home.fragment.HomeFragment
import com.example.fragment.module.navigation.fragment.NavigationFragment
import com.example.fragment.module.project.fragment.ProjectListFragment
import com.example.fragment.module.system.fragment.SystemFragment
import com.example.fragment.project.R
import com.example.fragment.project.databinding.FragmentWanBinding

class WanFragment : ViewBindingFragment<FragmentWanBinding>() {

    companion object {
        @JvmStatic
        fun newInstance(): WanFragment {
            return WanFragment()
        }
    }

    private val tabDrawable = intArrayOf(
        R.drawable.ic_bottom_bar_home,
        R.drawable.ic_bottom_bar_navigation,
        R.drawable.ic_bottom_bar_faq,
        R.drawable.ic_bottom_bar_system,
        R.drawable.ic_bottom_bar_project
    )
    private val tabTexts = arrayOf("首页", "导航", "问答", "体系", "项目")
    private val fragments = arrayListOf(
        HomeFragment.newInstance(),
        NavigationFragment.newInstance(),
        FAQFragment.newInstance(),
        SystemFragment.newInstance(),
        ProjectListFragment.newInstance()
    )

    override fun setViewBinding(): (LayoutInflater, ViewGroup?, Boolean) -> FragmentWanBinding {
        return FragmentWanBinding::inflate
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("WAN_CURRENT_POSITION", binding.tab.getCurrentPosition())
    }

    private fun setupView(savedInstanceState: Bundle?) {
        binding.viewpager.offscreenPageLimit = 4
        binding.viewpager.adapter = object :
            FragmentPagerAdapter(childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

            override fun getItem(position: Int): Fragment {
                return fragments[position]
            }

            override fun getCount(): Int {
                return fragments.size
            }
        }
        binding.tab.setTabMod(SimpleTabLayout.MODE.AUTO)
        for (i in fragments.indices) {
            val layoutInflater = LayoutInflater.from(binding.root.context)
            val tabView: View = layoutInflater.inflate(R.layout.item_tab_main, null)
            val imgTab = tabView.findViewById<ImageView>(R.id.iv_tab_icon)
            val txtTab = tabView.findViewById<TextView>(R.id.tv_tab_name)
            imgTab.setImageDrawable(ContextCompat.getDrawable(imgTab.context, tabDrawable[i]))
            imgTab.setColorFilter(ContextCompat.getColor(imgTab.context, R.color.gray_alpha))
            txtTab.setTextColor(ContextCompat.getColor(txtTab.context, R.color.gray_alpha))
            txtTab.text = tabTexts[i]
            binding.tab.addTab(tabView)
        }
        binding.tab.setupWithViewPager(binding.viewpager)
        binding.tab.setOnTabSelectedListener(object : SimpleTabLayout.OnTabSelectedListener {
            override fun onTabSelected(tabView: View, position: Int, isRefresh: Boolean) {
                val imgTab = tabView.findViewById<ImageView>(R.id.iv_tab_icon)
                val txtTab = tabView.findViewById<TextView>(R.id.tv_tab_name)
                imgTab.setColorFilter(ContextCompat.getColor(imgTab.context, R.color.text_fff))
                txtTab.setTextColor(ContextCompat.getColor(txtTab.context, R.color.text_fff))
            }

            override fun onTabUnselected(tabView: View, position: Int) {
                val imgTab = tabView.findViewById<ImageView>(R.id.iv_tab_icon)
                val txtTab = tabView.findViewById<TextView>(R.id.tv_tab_name)
                imgTab.setColorFilter(ContextCompat.getColor(imgTab.context, R.color.gray_alpha))
                txtTab.setTextColor(ContextCompat.getColor(txtTab.context, R.color.gray_alpha))
            }
        })
        binding.tab.selectTab(savedInstanceState?.getInt("WAN_CURRENT_POSITION") ?: 0)
    }

}