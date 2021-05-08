package com.example.fragment.module.system.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.example.fragment.library.base.component.view.SimpleTabLayout
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.system.R
import com.example.fragment.module.system.bean.TreeBean
import com.example.fragment.module.system.databinding.FragmentSystemListBinding
import com.example.fragment.module.system.model.SystemViewModel

class SystemListFragment : ViewModelFragment<FragmentSystemListBinding, SystemViewModel>() {

    companion object {

        const val KEY = "tree"

        @JvmStatic
        fun newInstance(): SystemListFragment {
            return SystemListFragment()
        }
    }

    private var tree: TreeBean? = null

    override fun setViewBinding(inflater: LayoutInflater): FragmentSystemListBinding {
        return FragmentSystemListBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.apply {
            tree = this.getParcelable(KEY)
        }
        setupView()
        update(savedInstanceState)
    }

    private fun setupView() {
        binding.black.setOnClickListener {
            getBaseActivity().onBackPressed()
        }
        tree?.apply {
            binding.title.text = name
        }
        binding.tab.setTabMod(SimpleTabLayout.MODE.FIXED)
        binding.tab.setSelectedIndicatorColor(R.color.main)
        binding.tab.setSelectedIndicatorHeight(10)
    }

    private fun update(savedInstanceState: Bundle?) {
        binding.viewpager.offscreenPageLimit = 1
        tree?.children?.let { data ->
            binding.viewpager.adapter = object :
                FragmentPagerAdapter(childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

                override fun getItem(position: Int): Fragment {
                    return SystemArticleFragment.newInstance(data[position].id)
                }

                override fun getCount(): Int {
                    return data.size
                }
            }
            data.forEach {
                val tabView: View = LayoutInflater.from(binding.root.context)
                    .inflate(R.layout.tab_item_top, null)
                tabView.findViewById<TextView>(R.id.tv_tab).text = it.name
                binding.tab.addTab(tabView)
            }
            binding.tab.setupWithViewPager(binding.viewpager)
            if (savedInstanceState == null) {
                binding.tab.selectTab(0)
            }
        }
    }
}