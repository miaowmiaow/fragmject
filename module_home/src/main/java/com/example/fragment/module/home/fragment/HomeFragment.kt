package com.example.fragment.module.home.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.component.view.SimplePullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.home.databinding.FragmentHomeBinding
import com.example.fragment.module.home.model.HomeViewModel

class HomeFragment : ViewModelFragment<FragmentHomeBinding, HomeViewModel>() {

    companion object {
        @JvmStatic
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    private val articleAdapter = ArticleAdapter()

    override fun setViewBinding(): (LayoutInflater) -> FragmentHomeBinding {
        return FragmentHomeBinding::inflate
    }

    override fun setViewModel(): Class<HomeViewModel> {
        return HomeViewModel::class.java
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
    }

    override fun onUserStatusUpdate(userBean: UserBean) {
        binding.pullRefresh.setRefreshing()
    }

    private fun setupView() {
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.pullRefresh.setOnRefreshListener(object :
            SimplePullRefreshLayout.OnRefreshListener {
            override fun onRefresh(refreshLayout: SimplePullRefreshLayout) {
                viewModel.getBanner()
                viewModel.getArticleList(true)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object :
            SimplePullRefreshLayout.OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: SimplePullRefreshLayout) {
                viewModel.getArticleList(false)
            }
        })
        binding.pullRefresh.setRefreshing()
    }

    private fun update() {
        viewModel.bannerResult.observe(viewLifecycleOwner, { result ->
            if (result.errorCode == "0") {
                result.data?.apply {
                    articleAdapter.setBannerData(this)
                }
            }
            if (result.errorMsg.isNotBlank()) {
                baseActivity.showTips(result.errorMsg)
            }
        })
        viewModel.articleTopResult.observe(viewLifecycleOwner, { result ->
            if (result.errorCode == "0") {
                result.data?.let { list ->
                    list.forEach {
                        it.top = true
                    }
                    articleAdapter.addData(0, list)
                }
            }
            if (result.errorMsg.isNotBlank()) {
                baseActivity.showTips(result.errorMsg)
            }
        })
        viewModel.articleListResult.observe(viewLifecycleOwner, { result ->
            if (result.errorCode == "0") {
                result.data?.datas?.let { list ->
                    if (viewModel.isRefresh) {
                        articleAdapter.setNewData(list)
                    } else {
                        articleAdapter.addData(list)
                        binding.pullRefresh.setLoadMore(true)
                    }
                }
            }
            if (result.errorMsg.isNotBlank()) {
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