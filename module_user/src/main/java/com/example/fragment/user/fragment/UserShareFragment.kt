package com.example.fragment.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.component.view.SimplePullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.user.databinding.FragmentUserShareBinding
import com.example.fragment.user.model.UserViewModel

class UserShareFragment : ViewModelFragment<FragmentUserShareBinding, UserViewModel>() {

    private val articleAdapter = ArticleAdapter()

    private var id: String = ""

    override fun setViewBinding(inflater: LayoutInflater): FragmentUserShareBinding {
        return FragmentUserShareBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.apply {
            id = this.getString(Keys.ID).toString()
        }
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
                viewModel.userShare(true, id)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object :
            SimplePullRefreshLayout.OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: SimplePullRefreshLayout) {
                viewModel.userShare(false, id)
            }
        })
        binding.pullRefresh.setRefreshing()
    }

    private fun update() {
        viewModel.userShareResult.observe(viewLifecycleOwner, { result ->
            if (result.errorCode == "0") {
                result.data?.coinInfo?.let { coin ->
                    binding.title.text = coin.username
                    binding.id.text = coin.userId
                    binding.coinCount.text = coin.coinCount
                }
                result.data?.shareArticles?.datas?.let { list ->
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