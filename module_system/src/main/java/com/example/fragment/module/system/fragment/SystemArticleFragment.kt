package com.example.fragment.module.system.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.component.view.SimplePullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.ViewBindingFragment
import com.example.fragment.module.system.databinding.FragmentSystemArticleBinding
import com.example.fragment.module.system.model.SystemViewModel

class SystemArticleFragment : ViewBindingFragment<FragmentSystemArticleBinding>() {

    companion object {
        @JvmStatic
        fun newInstance(cid: String): SystemArticleFragment {
            val fragment = SystemArticleFragment()
            val args = Bundle()
            args.putString(Keys.CID, cid)
            fragment.arguments = args
            return fragment
        }
    }

    private val viewModel: SystemViewModel by viewModels()
    private val articleAdapter = ArticleAdapter()
    private var cid = ""

    override fun setViewBinding(): (LayoutInflater, ViewGroup?, Boolean) -> FragmentSystemArticleBinding {
        return FragmentSystemArticleBinding::inflate
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.apply {
            cid = this.getString(Keys.CID).toString()
        }
        setupView()
        update()
    }

    private fun setupView() {
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.pullRefresh.setOnRefreshListener(object :
            SimplePullRefreshLayout.OnRefreshListener {
            override fun onRefresh(refreshLayout: SimplePullRefreshLayout) {
                viewModel.getTreeList(true, cid)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object :
            SimplePullRefreshLayout.OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: SimplePullRefreshLayout) {
                viewModel.getTreeList(false, cid)
            }
        })
        binding.pullRefresh.setRefreshing()
    }

    private fun update() {
        viewModel.treeListResult.observe(viewLifecycleOwner, { result ->
            if (result.errorCode == "0") {
                result.data?.datas?.let { list ->
                    if (viewModel.isRefresh) {
                        articleAdapter.setNewData(list)
                    } else {
                        articleAdapter.addData(list)
                        binding.pullRefresh.setLoadMore(true)
                    }
                }
            } else if (result.errorCode.isNotBlank()) {
                baseActivity.showTips(result.errorMsg)
            }
            if (binding.pullRefresh.isRefresh()) {
                binding.pullRefresh.finishRefresh()
            }
            if (viewModel.page >= viewModel.pageCont) {
                binding.pullRefresh.setLoadMore(false)
            }
        })
    }

}