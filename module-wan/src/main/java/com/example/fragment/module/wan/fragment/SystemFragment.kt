package com.example.fragment.module.wan.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.TreeBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.databinding.FragmentSystemBinding
import com.example.fragment.module.wan.model.NavigationViewModel
import com.google.android.material.tabs.TabLayoutMediator

class SystemFragment : RouterFragment() {

    private val viewModel: NavigationViewModel by activityViewModels()
    private var _binding: FragmentSystemBinding? = null
    private val binding get() = _binding!!

    private var cid = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSystemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        cid = getCid()
        binding.black.setOnClickListener {
            activity.onBackPressed()
        }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.systemTreeResult.observe(viewLifecycleOwner) { treeList ->
            treeList.data?.forEach { treeBean ->
                treeBean.children?.forEachIndexed { index, childrenTreeBean ->
                    if (childrenTreeBean.id == cid) {
                        treeBean.childrenSelectPosition = index
                        updateView(treeBean)
                        return@observe
                    }
                }
            }
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.systemTreeResult.value == null) {
            viewModel.getSystemTree()
        }
    }

    private fun getCid(): String {
        var cid = ""
        if (requireArguments().containsKey(Keys.CID)) {
            cid = requireArguments().getString(Keys.CID).toString()
            if (cid.isNotBlank()) {
                return cid
            }
        }
        if (requireArguments().containsKey(Keys.URL)) {
            val url = requireArguments().getString(Keys.URL)
            val uri = Uri.parse("https://www.wanandroid.com/${Uri.decode(url)}")
            var chapterId = uri.getQueryParameter("cid")
            if (chapterId.isNullOrBlank()) {
                val paths = uri.pathSegments
                if (paths != null && paths.size >= 3) {
                    chapterId = paths[2]
                }
            }
            cid = chapterId.toString()
        }
        return cid
    }

    private fun updateView(treeBean: TreeBean) {
        binding.title.text = treeBean.name
        treeBean.children?.let { data ->
            //TabLayout与ViewPager2
            binding.viewpager.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount(): Int {
                    return data.size
                }

                override fun createFragment(position: Int): Fragment {
                    val args = bundleOf(Keys.CID to data[position].id)
                    val fragment = SystemArticleFragment.newInstance()
                    fragment.arguments = args
                    return fragment
                }
            }
            TabLayoutMediator(binding.tabBar, binding.viewpager) { tab, position ->
                tab.text = data[position].name
            }.attach()
            var selectPosition = binding.tabBar.selectedTabPosition
            if (selectPosition == 0) selectPosition = treeBean.childrenSelectPosition
            binding.viewpager.setCurrentItem(selectPosition, false)
        }
    }

}