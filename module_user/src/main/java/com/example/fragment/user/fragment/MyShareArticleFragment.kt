package com.example.fragment.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.component.view.SimplePullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.databinding.FragmentMyShareArticleBinding
import com.example.fragment.user.model.UserViewModel

class MyShareArticleFragment : ViewModelFragment<FragmentMyShareArticleBinding, UserViewModel>() {

    private val articleAdapter = ArticleAdapter()

    override fun setViewBinding(): (LayoutInflater) -> FragmentMyShareArticleBinding {
        return FragmentMyShareArticleBinding::inflate
    }

    override fun setViewModel(): Class<UserViewModel> {
        return UserViewModel::class.java
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
    }

    private fun setupView() {
        binding.black.setOnClickListener { baseActivity.onBackPressed() }
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.pullRefresh.setOnRefreshListener(object :
            SimplePullRefreshLayout.OnRefreshListener {
            override fun onRefresh(refreshLayout: SimplePullRefreshLayout) {
                viewModel.myShareArticle(true)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object :
            SimplePullRefreshLayout.OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: SimplePullRefreshLayout) {
                viewModel.myShareArticle(false)
            }
        })
        binding.pullRefresh.setRefreshing()
    }

    private fun update() {
        viewModel.myShareArticleResult.observe(viewLifecycleOwner, { result ->
            when {
                result.errorCode == "0" -> {
                    result.data?.shareArticles?.datas?.let { list ->
                        if (viewModel.isRefresh) {
                            articleAdapter.setNewData(list)
                        } else {
                            articleAdapter.addData(list)
                            binding.pullRefresh.setLoadMore(true)
                        }
                    }
                }
                result.errorCode == "-1001" -> {
                    WanHelper.setUser(UserBean())
                    baseActivity.showTips(result.errorMsg)
                    baseActivity.navigation(Router.LOGIN)
                }
                result.errorCode.isNotBlank() && result.errorMsg.isNotBlank() -> {
                    baseActivity.showTips(result.errorMsg)
                }
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