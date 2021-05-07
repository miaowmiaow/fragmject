package com.example.fragment.project.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.example.fragment.library.base.component.view.SimpleTabLayout
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.module.faq.fragment.FAQFragment
import com.example.fragment.module.home.fragment.HomeFragment
import com.example.fragment.module.project.fragment.ProjectFragment
import com.example.fragment.module.system.fragment.SystemFragment
import com.example.fragment.project.R
import com.example.fragment.project.databinding.FragmentWanAndroidBinding

class WanAndroidFragment : ViewModelFragment<FragmentWanAndroidBinding, BaseViewModel>(){

    companion object {
        @JvmStatic
        fun newInstance(): WanAndroidFragment {
            return WanAndroidFragment()
        }
    }

    private val tabDrawable = intArrayOf(
        R.drawable.ic_bottom_bar_home,
        R.drawable.ic_bottom_bar_faq,
        R.drawable.ic_bottom_bar_system,
        R.drawable.ic_bottom_bar_project
    )

    private val tabTexts = arrayOf("首页", "问答", "体系", "项目")

    private val fragments = arrayListOf(
        HomeFragment.newInstance(),
        FAQFragment.newInstance(),
        SystemFragment.newInstance(),
        ProjectFragment.newInstance()
    )

    override fun setViewBinding(inflater: LayoutInflater): FragmentWanAndroidBinding {
        return FragmentWanAndroidBinding.inflate(inflater)
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
        setupView(savedInstanceState)
    }

    private fun setupView(savedInstanceState: Bundle?){
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
        binding.tab.setTabMod(SimpleTabLayout.MODE.AUTO)
        for (i in fragments.indices) {
            val tabView: View = LayoutInflater.from(binding.root.context).inflate(
                R.layout.tab_item_main,
                null
            )
            val imgTab = tabView.findViewById<ImageView>(R.id.iv_tab_icon)
            val txtTab = tabView.findViewById<TextView>(R.id.tv_tab_name)
            imgTab.setImageDrawable(ContextCompat.getDrawable(imgTab.context, tabDrawable[i]))
            imgTab.setColorFilter(ContextCompat.getColor(imgTab.context, R.color.gray_alpha))
            txtTab.text = tabTexts[i]
            binding.tab.addTab(tabView)
        }
        binding.tab.setupWithViewPager(binding.viewpager)
        binding.tab.setOnTabSelectedListener(object : SimpleTabLayout.OnTabSelectedListener {
            override fun onTabSelected(tabView: View, position: Int, isRefresh: Boolean) {
                val imgTab = tabView.findViewById<ImageView>(R.id.iv_tab_icon)
                val txtTab = tabView.findViewById<TextView>(R.id.tv_tab_name)
                imgTab.setColorFilter(ContextCompat.getColor(imgTab.context, R.color.white))
                txtTab.setTextColor(ContextCompat.getColor(txtTab.context, R.color.white))
            }

            override fun onTabUnselected(tabView: View, position: Int) {
                val imgTab = tabView.findViewById<ImageView>(R.id.iv_tab_icon)
                val txtTab = tabView.findViewById<TextView>(R.id.tv_tab_name)
                imgTab.setColorFilter(ContextCompat.getColor(imgTab.context, R.color.gray_alpha))
                txtTab.setTextColor(ContextCompat.getColor(txtTab.context, R.color.gray_alpha))
            }
        })
        if (savedInstanceState == null) {
            binding.tab.selectTab(0)
        }
    }

}