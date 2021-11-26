package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.bean.ProjectTreeDataBean
import com.example.fragment.module.wan.databinding.FragmentProjectBinding
import com.example.fragment.module.wan.model.ProjectViewModel
import com.google.android.material.tabs.TabLayoutMediator

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
        binding.viewpager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return data.size
            }

            override fun createFragment(position: Int): Fragment {
                val args = bundleOf(Keys.CID to data[position].id)
                val fragment = ProjectArticleFragment.newInstance()
                fragment.arguments = args
                return fragment
            }
        }
        TabLayoutMediator(binding.tabBar, binding.viewpager) { tab, position ->
            tab.text = data[position].name
        }.attach()
    }

}