package com.example.fragment.module.system.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.example.fragment.library.base.component.view.SimpleTabLayout
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.system.R
import com.example.fragment.module.system.bean.TreeBean
import com.example.fragment.module.system.databinding.FragmentSystemListBinding
import com.example.fragment.module.system.model.SystemViewModel

class SystemListFragment : ViewModelFragment<FragmentSystemListBinding, SystemViewModel>() {

    private var tree: TreeBean? = null

    override fun setViewBinding(): (LayoutInflater) -> FragmentSystemListBinding {
        return FragmentSystemListBinding::inflate
    }

    override fun setViewModel(): Class<SystemViewModel> {
        return SystemViewModel::class.java
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.apply {
            tree = this.getParcelable(Keys.BEAN)
        }
        setupView()
        update(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("TAB_CURRENT_POSITION", binding.tab.getCurrentPosition())
    }

    private fun setupView() {
        binding.black.setOnClickListener {
            baseActivity.onBackPressed()
        }
        binding.tab.setTabMod(SimpleTabLayout.MODE.FIXED)
        binding.tab.setSelectedIndicatorColor(R.color.black)
        binding.tab.setSelectedIndicatorHeight(5)
    }

    private fun update(savedInstanceState: Bundle?) {
        binding.viewpager.offscreenPageLimit = 2
        tree?.let { tree ->
            binding.title.text = tree.name
            tree.children?.let { data ->
                binding.viewpager.adapter = object :
                    FragmentPagerAdapter(
                        childFragmentManager,
                        BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
                    ) {

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
            }
            binding.tab.selectTab(
                savedInstanceState?.getInt("TAB_CURRENT_POSITION") ?: tree.childrenSelectPosition
            )
        }
    }
}