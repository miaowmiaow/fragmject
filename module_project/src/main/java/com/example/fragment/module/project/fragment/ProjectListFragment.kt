package com.example.fragment.module.project.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.viewModels
import com.example.fragment.library.base.component.view.SimpleTabLayout
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.project.R
import com.example.fragment.module.project.databinding.FragmentProjectListBinding
import com.example.fragment.module.project.model.ProjectViewModel

class ProjectListFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): ProjectListFragment {
            return ProjectListFragment()
        }
    }

    private val viewModel: ProjectViewModel by viewModels()
    private var _binding: FragmentProjectListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update(savedInstanceState)
        viewModel.getProjectTree()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("TAB_CURRENT_POSITION", binding.tab.getCurrentPosition())
    }

    override fun onUserStatusUpdate(userBean: UserBean) {
        viewModel.getProjectTree()
    }

    private fun setupView() {
        binding.tab.setTabMod(SimpleTabLayout.MODE.FIXED)
        binding.tab.setSelectedIndicatorColor(R.color.black)
        binding.tab.setSelectedIndicatorHeight(5)
    }

    private fun update(savedInstanceState: Bundle?) {
        viewModel.projectTreeResult.observe(viewLifecycleOwner, { result ->
            if (result.errorCode == "0") {
                result.data?.also { data ->
                    binding.viewpager.offscreenPageLimit = 2
                    binding.viewpager.adapter = object : FragmentPagerAdapter(
                        childFragmentManager,
                        BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
                    ) {
                        override fun getItem(position: Int): Fragment {
                            return ProjectArticleFragment.newInstance(data[position].id)
                        }

                        override fun getCount(): Int {
                            return data.size
                        }
                    }
                    binding.tab.removeAllTabs()
                    data.forEach {
                        val tabView: View = LayoutInflater.from(binding.root.context)
                            .inflate(R.layout.tab_item_top, null)
                        tabView.findViewById<TextView>(R.id.tv_tab).text = it.name
                        binding.tab.addTab(tabView)
                    }
                    binding.tab.setupWithViewPager(binding.viewpager)
                    binding.tab.selectTab(savedInstanceState?.getInt("TAB_CURRENT_POSITION") ?: 0)
                }
            }
            if (result.errorCode.isNotBlank() && result.errorMsg.isNotBlank()) {
                baseActivity.showTips(result.errorMsg)
            }
        })
    }
}