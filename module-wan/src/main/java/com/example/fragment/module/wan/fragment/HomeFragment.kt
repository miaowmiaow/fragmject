package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.BannerHelper
import com.example.fragment.library.base.view.pull.OnLoadMoreListener
import com.example.fragment.library.base.view.pull.OnRefreshListener
import com.example.fragment.library.base.view.pull.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.adapter.BannerAdapter
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.databinding.HomeFragmentBinding
import com.example.fragment.module.wan.model.HomeViewModel

class HomeFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    private val viewModel: HomeViewModel by viewModels()
    private var _binding: HomeFragmentBinding? = null
    private val binding get() = _binding!!

    private var bannerHelper: BannerHelper? = null
    private val bannerAdapter = BannerAdapter()
    private val articleAdapter = ArticleAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        bannerHelper?.startTimerTask()
    }

    override fun onPause() {
        super.onPause()
        bannerHelper?.stopTimerTask()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        binding.banner.adapter = bannerAdapter
        bannerHelper = BannerHelper(binding.banner)
        bannerHelper?.startTimerTask()
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
        binding.coordinator.post {
            binding.coordinator.setMaxScrollY(binding.banner.height)
            val layoutParams = binding.list.layoutParams
            layoutParams.height = binding.coordinator.height
            binding.list.layoutParams = layoutParams
        }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.bannerResult.observe(viewLifecycleOwner) { result ->
            wanSuccessCallback(result) {
                bannerAdapter.setNewData(result.data)
            }
        }
        viewModel.articleListResult.observe(viewLifecycleOwner) { result ->
            if (viewModel.isHomePage()) {
                articleAdapter.setNewData(result)
            } else {
                articleAdapter.addData(result)
            }
            //结束下拉刷新状态
            binding.pullRefresh.finishRefresh()
            //设置加载更多状态
            binding.pullRefresh.setLoadMore(viewModel.hasNextPage())
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.articleListResult.value == null) {
            viewModel.getArticle()
        }
    }

}