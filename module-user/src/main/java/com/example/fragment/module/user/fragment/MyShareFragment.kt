package com.example.fragment.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.view.OnLoadMoreListener
import com.example.fragment.library.base.view.OnRefreshListener
import com.example.fragment.library.base.view.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.databinding.FragmentMyShareBinding
import com.example.fragment.module.user.model.MyShareViewModel

class MyShareFragment : RouterFragment() {

    private val viewModel: MyShareViewModel by activityViewModels()
    private var _binding: FragmentMyShareBinding? = null
    private val binding get() = _binding!!

    private val articleAdapter = ArticleAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        binding.black.setOnClickListener { activity.onBackPressed() }
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getMyShareArticle()
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getMyShareArticleNext()
            }
        })
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.myShareArticleResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> {
                    if (viewModel.isHomePage()) {
                        articleAdapter.setNewData(result.data?.shareArticles?.datas)
                    } else {
                        articleAdapter.addData(result.data?.shareArticles?.datas)
                    }
                }
                else -> activity.showTips(result.errorMsg)
            }
            binding.pullRefresh.finishRefresh()
            binding.pullRefresh.setLoadMore(viewModel.hasNextPage())
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.myShareArticleResult.value == null) {
            viewModel.getMyShareArticle()
        }
    }

}