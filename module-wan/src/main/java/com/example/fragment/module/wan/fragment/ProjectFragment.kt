package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.R
import com.example.fragment.module.wan.databinding.FragmentProjectListBinding
import com.example.fragment.module.wan.model.ProjectViewModel
import com.google.android.material.tabs.TabLayoutMediator

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

    override fun initView() {
    }

    override fun initViewModel() {
        viewModel.projectTreeResult.observe(viewLifecycleOwner) { result ->
            when {
                result.errorCode == "0" -> {
                    result.data?.also { data ->
                        binding.viewpager.offscreenPageLimit = 1
                        binding.viewpager.adapter = object : FragmentStateAdapter(this@ProjectListFragment) {
                                override fun getItemCount(): Int {
                                    return data.size
                                }

                                override fun createFragment(position: Int): Fragment {
                                    val args = Bundle()
                                    args.putString(Keys.CID, data[position].id)
                                    val fragment = ProjectArticleFragment.newInstance()
                                    fragment.arguments = args
                                    return fragment
                                }
                            }
                        binding.tabBar.removeAllTabs()
                        TabLayoutMediator(binding.tabBar, binding.viewpager) { tab, position ->
                            val layoutInflater = LayoutInflater.from(binding.root.context)
                            val tabView: View = layoutInflater.inflate(R.layout.tab_item_top, null)
                            tabView.findViewById<TextView>(R.id.tv_tab).text = data[position].name
                            tab.customView = tabView
                        }.attach()
                        binding.viewpager.currentItem = 0
                    }
                }
                result.errorCode.isNotBlank() && result.errorMsg.isNotBlank() -> {
                    activity.showTips(result.errorMsg)
                }
            }
        }
    }

    override fun onLoad() {
        if(viewModel.projectTreeResult.value == null){
            viewModel.getProjectTree()
        }
    }

}