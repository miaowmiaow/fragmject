package com.example.fragment.module.wan.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.fragment.library.base.adapter.FragmentStatePagerAdapter
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.TreeBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.databinding.TabItemTopBinding
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.databinding.FragmentSystemBinding
import com.example.fragment.module.wan.model.NavigationViewModel

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
            if(cid.isNotBlank()){
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
            binding.viewpager.offscreenPageLimit = 2
            binding.viewpager.adapter =
                object : FragmentStatePagerAdapter(childFragmentManager) {

                    override fun getCount(): Int {
                        return data.size
                    }

                    override fun getItem(position: Int): Fragment {
                        val fragment = SystemArticleFragment.newInstance()
                        fragment.arguments = bundleOf(Keys.CID to data[position].id)
                        return fragment
                    }
                }
            binding.tabBar.setupWithViewPager(binding.viewpager)
            var currentItem = binding.tabBar.selectedTabPosition
            if (currentItem == -1) currentItem = treeBean.childrenSelectPosition
            binding.tabBar.removeAllTabs()
            data.forEach {
                val item = TabItemTopBinding.inflate(LayoutInflater.from(binding.root.context))
                item.tab.text = it.name
                binding.tabBar.addTab(binding.tabBar.newTab().setCustomView(item.root))
            }
            binding.viewpager.currentItem = currentItem
        }
    }

}