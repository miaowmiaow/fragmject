package com.example.fragment.module.faq.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.component.view.SimplePullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.fragment.ViewBindingFragment
import com.example.fragment.module.faq.databinding.FragmentFaqBinding
import com.example.fragment.module.faq.model.FAQViewModel

class FAQFragment : ViewBindingFragment<FragmentFaqBinding>() {

    companion object {
        @JvmStatic
        fun newInstance(): FAQFragment {
            return FAQFragment()
        }
    }

    private val viewModel: FAQViewModel by viewModels()
    private val articleAdapter = ArticleAdapter()

    override fun setViewBinding(): (LayoutInflater, ViewGroup?, Boolean) -> FragmentFaqBinding {
        return FragmentFaqBinding::inflate
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
                viewModel.getUserArticleList(true)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object :
            SimplePullRefreshLayout.OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: SimplePullRefreshLayout) {
                viewModel.getUserArticleList(false)
            }
        })
        binding.pullRefresh.setRefreshing()
    }

    private fun update() {
        viewModel.wendaResult.observe(viewLifecycleOwner, { result ->
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
            if (result.errorCode.isNotBlank() && result.errorMsg.isNotBlank()) {
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