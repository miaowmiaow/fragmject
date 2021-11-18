package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.fragment.library.base.adapter.SimplePagerAdapter
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.R
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

    override fun initView() {
    }

    override fun initViewModel() {
        viewModel.projectTreeResult.observe(viewLifecycleOwner) { result ->
            when {
                result.errorCode == "0" -> {
                    result.data?.also { data ->
                        binding.viewpager.offscreenPageLimit = 1
                        binding.viewpager.adapter =
                            object : SimplePagerAdapter(childFragmentManager) {
                                override fun getCount(): Int {
                                    return data.size
                                }

                                override fun getItem(position: Int): Fragment {
                                    val args = Bundle()
                                    args.putString(Keys.CID, data[position].id)
                                    val fragment = ProjectArticleFragment.newInstance()
                                    fragment.arguments = args
                                    return fragment
                                }
                            }
                        binding.tabBar.setupWithViewPager(binding.viewpager)
                        var currentItem = binding.tabBar.selectedTabPosition
                        if (currentItem == -1) currentItem = 0
                        binding.tabBar.removeAllTabs()
                        data.forEach {
                            val layoutInflater = LayoutInflater.from(binding.root.context)
                            val tabView: View = layoutInflater.inflate(R.layout.tab_item_top, null)
                            tabView.findViewById<TextView>(R.id.tv_tab).text = it.name
                            val tab = binding.tabBar.newTab()
                            tab.customView = tabView
                            binding.tabBar.addTab(tab)
                        }
                        binding.viewpager.currentItem = currentItem
                    }
                }
                result.errorCode.isNotBlank() && result.errorMsg.isNotBlank() -> {
                    activity.showTips(result.errorMsg)
                }
            }
        }
    }

    override fun onLoad() {
        if (viewModel.projectTreeResult.value == null) {
            viewModel.getProjectTree()
        }
    }

}