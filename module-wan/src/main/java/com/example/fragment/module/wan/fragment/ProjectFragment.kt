package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.fragment.library.base.adapter.FragmentStatePagerAdapter
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.databinding.TabItemTopBinding
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.bean.ProjectTreeDataBean
import com.example.fragment.module.wan.databinding.FragmentProjectBinding
import com.example.fragment.module.wan.model.ProjectViewModel

class ProjectFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): ProjectFragment {
            return ProjectFragment()
        }
    }

    private val viewModel: ProjectViewModel by viewModels()
    private var _binding: FragmentProjectBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {}

    override fun initViewModel(): BaseViewModel {
        viewModel.projectTreeResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> updateView(result.data)
                else -> activity.showTips(result.errorMsg)
            }
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.projectTreeResult.value == null) {
            viewModel.getProjectTree()
        }
    }

    private fun updateView(data: List<ProjectTreeDataBean>? = null) {
        if (data.isNullOrEmpty()) {
            return
        }
        binding.viewpager.offscreenPageLimit = 2
        binding.viewpager.adapter =
            object : FragmentStatePagerAdapter(childFragmentManager) {
                override fun getCount(): Int {
                    return data.size
                }

                override fun getItem(position: Int): Fragment {
                    val fragment = ProjectArticleFragment.newInstance()
                    fragment.arguments = bundleOf(Keys.CID to data[position].id)
                    return fragment
                }
            }
        binding.tabBar.setupWithViewPager(binding.viewpager)
        var currentItem = binding.tabBar.selectedTabPosition
        if (currentItem == -1) currentItem = 0
        binding.tabBar.removeAllTabs()
        data.forEach {
            val item = TabItemTopBinding.inflate(LayoutInflater.from(binding.root.context))
            item.tab.text = it.name
            binding.tabBar.addTab(binding.tabBar.newTab().setCustomView(item.root))
        }
        binding.viewpager.currentItem = currentItem
    }

}