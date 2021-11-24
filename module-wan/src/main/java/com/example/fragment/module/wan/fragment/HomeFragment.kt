package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.view.OnLoadMoreListener
import com.example.fragment.library.base.view.OnRefreshListener
import com.example.fragment.library.base.view.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.databinding.FragmentHomeBinding
import com.example.fragment.module.wan.model.HomeViewModel

class HomeFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    private val viewModel: HomeViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val articleAdapter = ArticleAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        //文章列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getArticle()
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getArticleNext()
            }
        })
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.bannerResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> articleAdapter.setBannerData(result.data)
                else -> activity.showTips(result.errorMsg)
            }
        }
        viewModel.articleTopResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> {
                    result.data?.let { data ->
                        data.forEach { it.top = true }
                        articleAdapter.addData(0, data)
                    }
                }
            }
        }
        viewModel.articleListResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> {
                    if (viewModel.isHomePage()) {
                        articleAdapter.setNewData(result.data?.datas)
                    } else {
                        articleAdapter.addData(result.data?.datas)
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
        if (viewModel.articleListResult.value == null) {
            viewModel.getBanner()
            viewModel.getArticleTop()
            viewModel.getArticle()
        }
    }

}