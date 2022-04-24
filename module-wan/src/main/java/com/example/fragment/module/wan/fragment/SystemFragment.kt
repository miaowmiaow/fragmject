package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.SystemTreeBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.databinding.SystemFragmentBinding
import com.example.fragment.module.wan.model.SystemViewModel
import com.google.android.material.tabs.TabLayoutMediator

class SystemFragment : RouterFragment() {

    private val viewModel: SystemViewModel by activityViewModels()
    private var _binding: SystemFragmentBinding? = null
    private val binding get() = _binding!!

    private var cid = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SystemFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        requireArguments().takeIf { it.containsKey(Keys.CID) }?.let {
            cid = it.getString(Keys.CID, "0")
        }
        binding.black.setOnClickListener {
            activity.onBackPressed()
        }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.treeResult.observe(viewLifecycleOwner) {
            it.forEach { data ->
                data.children?.forEachIndexed { index, children ->
                    if (children.id == cid) {
                        data.childrenSelectPosition = index
                        updateView(data)
                        return@observe
                    }
                }
            }
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.treeResult.value == null) {
            viewModel.getSystemTree()
        }
    }

    private fun updateView(treeBean: SystemTreeBean) {
        binding.title.text = treeBean.name
        treeBean.children?.let { data ->
            //TabLayout与ViewPager2
            binding.viewpager2.adapter = object : FragmentStateAdapter(
                childFragmentManager,
                viewLifecycleOwner.lifecycle
            ) {
                override fun getItemCount(): Int {
                    return data.size
                }

                override fun createFragment(position: Int): Fragment {
                    val fragment = SystemArticleFragment.newInstance()
                    fragment.arguments = bundleOf(Keys.CID to data[position].id)
                    return fragment
                }
            }
            TabLayoutMediator(binding.tabLayout, binding.viewpager2) { tab, position ->
                tab.text = data[position].name
            }.attach()
            var selectPosition = binding.tabLayout.selectedTabPosition
            if (selectPosition == 0) selectPosition = treeBean.childrenSelectPosition
            binding.viewpager2.setCurrentItem(selectPosition, false)
        }
    }

}