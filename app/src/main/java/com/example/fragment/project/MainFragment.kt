package com.example.fragment.project

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.fragment.library.base.component.view.SimpleTabLayout
import com.example.fragment.library.base.utils.FragmentUtils
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.module.faq.fragment.FAQFragment
import com.example.fragment.module.home.fragment.HomeFragment
import com.example.fragment.module.personal.fragment.PersonalFragment
import com.example.fragment.module.setup.fragment.SetupFragment
import com.example.fragment.project.databinding.FragmentMainBinding

class MainFragment : ViewModelFragment<FragmentMainBinding, BaseViewModel>() {

    private var curFragment: Class<out Fragment>? = null

    private val tabDrawable = intArrayOf(
        R.drawable.ic_bottom_bar_home,
        R.drawable.ic_bottom_bar_wechat,
        R.drawable.ic_bottom_bar_navi,
        R.drawable.ic_bottom_bar_mine
    )
    private val tabTexts = arrayOf("首页", "问答", "体系", "我的")

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
                    R.color.background
                )
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tab.setTabMod(SimpleTabLayout.MODE.AUTO)
        for (i in tabDrawable.indices) {
            val tabView: View = LayoutInflater.from(binding.root.context).inflate(
                R.layout.tab_item_main,
                null
            )
            val imgTab = tabView.findViewById<ImageView>(R.id.iv_tab_icon)
            val txtTab = tabView.findViewById<TextView>(R.id.tv_tab_name)
            imgTab.setImageDrawable(ContextCompat.getDrawable(imgTab.context, tabDrawable[i]))
            imgTab.setColorFilter(ContextCompat.getColor(imgTab.context, R.color.third))
            txtTab.text = tabTexts[i]
            binding.tab.addTab(tabView)
        }
        binding.tab.setOnTabSelectedListener(object : SimpleTabLayout.OnTabSelectedListener {
            override fun onTabSelected(tabView: View, position: Int, isRefresh: Boolean) {
                val imgTab = tabView.findViewById<ImageView>(R.id.iv_tab_icon)
                val txtTab = tabView.findViewById<TextView>(R.id.tv_tab_name)
                imgTab.setColorFilter(ContextCompat.getColor(imgTab.context, R.color.main))
                txtTab.setTextColor(ContextCompat.getColor(txtTab.context, R.color.main))
                when (position) {
                    0 -> switcherFragment(HomeFragment::class.java)
                    1 -> switcherFragment(FAQFragment::class.java)
                    2 -> switcherFragment(SetupFragment::class.java)
                    3 -> switcherFragment(PersonalFragment::class.java)
                }
            }

            override fun onTabUnselected(tabView: View, position: Int) {
                val imgTab = tabView.findViewById<ImageView>(R.id.iv_tab_icon)
                val txtTab = tabView.findViewById<TextView>(R.id.tv_tab_name)
                imgTab.setColorFilter(ContextCompat.getColor(imgTab.context, R.color.third))
                txtTab.setTextColor(ContextCompat.getColor(txtTab.context, R.color.third))
            }

        })
        if (savedInstanceState == null) {
            binding.tab.selectTab(0)
        }
    }

    fun switcherFragment(
        fragment: Class<out Fragment>,
        bundle: Bundle? = null,
        addToBackStack: Boolean = false
    ) {
        curFragment = FragmentUtils.switcher(
            childFragmentManager,
            R.id.main,
            curFragment,
            fragment,
            bundle,
            addToBackStack
        )
    }
}