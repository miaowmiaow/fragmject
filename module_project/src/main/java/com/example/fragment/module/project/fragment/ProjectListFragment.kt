package com.example.fragment.module.project.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.example.fragment.library.base.component.view.SimpleTabLayout
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.project.R
import com.example.fragment.module.project.databinding.FragmentProjectListBinding
import com.example.fragment.module.project.model.ProjectViewModel

class ProjectListFragment : ViewModelFragment<FragmentProjectListBinding, ProjectViewModel>() {

    companion object {
        @JvmStatic
        fun newInstance(): ProjectListFragment {
            return ProjectListFragment()
        }
    }

    override fun setViewBinding(inflater: LayoutInflater): FragmentProjectListBinding {
        return FragmentProjectListBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update(savedInstanceState)
        viewModel.getProjectTree()
    }

    private fun setupView() {
        binding.tab.setTabMod(SimpleTabLayout.MODE.FIXED)
        binding.tab.setSelectedIndicatorColor(R.color.black)
        binding.tab.setSelectedIndicatorHeight(10)
    }

    private fun update(savedInstanceState: Bundle?) {
        viewModel.projectTreeResult.observe(viewLifecycleOwner, { result ->
            result.data?.also { data ->
                binding.viewpager.offscreenPageLimit = 1
                binding.viewpager.adapter = object :
                    FragmentPagerAdapter(childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

                    override fun getItem(position: Int): Fragment {
                        return ProjectArticleFragment.newInstance(data[position].id)
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
        })
    }
}