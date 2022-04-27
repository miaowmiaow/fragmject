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
import com.example.fragment.library.common.bean.ProjectTreeBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.databinding.ProjectFragmentBinding
import com.example.fragment.module.wan.model.ProjectTreeViewModel
import com.google.android.material.tabs.TabLayoutMediator

class ProjectFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): ProjectFragment {
            return ProjectFragment()
        }
    }

    private val viewModel: ProjectTreeViewModel by activityViewModels()
    private var _binding: ProjectFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ProjectFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {}

    override fun initViewModel(): BaseViewModel {
        viewModel.projectTreeResult().observe(viewLifecycleOwner) { updateView(it) }
        return viewModel
    }

    private fun updateView(data: List<ProjectTreeBean>) {
        //TabLayout与ViewPager2
        binding.viewpager2.adapter = object : FragmentStateAdapter(
            childFragmentManager,
            viewLifecycleOwner.lifecycle
        ) {
            override fun getItemCount(): Int {
                return data.size
            }

            override fun createFragment(position: Int): Fragment {
                val fragment = ProjectArticleFragment.newInstance()
                fragment.arguments = bundleOf(Keys.CID to data[position].id)
                return fragment
            }
        }
        TabLayoutMediator(binding.tabLayout, binding.viewpager2) { tab, position ->
            tab.text = data[position].name
        }.attach()
    }

}