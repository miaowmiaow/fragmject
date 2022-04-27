package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.dp2px
import com.example.fragment.library.base.utils.BannerHelper
import com.example.fragment.library.base.utils.OnItemScrollListener
import com.example.fragment.library.base.view.pull.OnLoadMoreListener
import com.example.fragment.library.base.view.pull.OnRefreshListener
import com.example.fragment.library.base.view.pull.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.adapter.BannerAdapter
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.R
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

    private val articleAdapter = ArticleAdapter()
    private lateinit var bannerHelper: BannerHelper
    private val bannerAdapter = BannerAdapter()

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
        bannerHelper.start()
    }

    override fun onPause() {
        super.onPause()
        bannerHelper.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bannerHelper.stop()
        _binding = null
    }

    override fun initView() {
        binding.banner.adapter = bannerAdapter
        bannerHelper = BannerHelper(binding.banner)
        bannerHelper.setOnItemScrollListener(object : OnItemScrollListener {
            override fun onItemScroll(position: Int) {
                makeSureIndicator(position)
            }
        })
        //文章列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getArticleHome()
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
            binding.list.layoutParams.height = binding.coordinator.height
        }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.bannerResult().observe(viewLifecycleOwner) {
            httpParseSuccess(it) { result ->
                bannerAdapter.setNewData(result.data)
                bannerHelper.start()
                result.data?.apply { initIndicator(size) }
            }
        }
        viewModel.articleListResult().observe(viewLifecycleOwner) {
            if (viewModel.isHomePage()) {
                articleAdapter.setNewData(it)
            } else {
                articleAdapter.addData(it)
            }
            //结束下拉刷新状态
            binding.pullRefresh.finishRefresh()
            //设置加载更多状态
            binding.pullRefresh.setLoadMore(viewModel.hasNextPage())
        }
        return viewModel
    }

    private fun initIndicator(itemCount: Int) {
        if (itemCount > 0) {
            binding.indicator.removeAllViews()
            val layoutParams = LinearLayout.LayoutParams(
                dp2px(12f).toInt(),
                dp2px(3f).toInt()
            )
            layoutParams.marginStart = dp2px(2.5f).toInt()
            layoutParams.marginEnd = dp2px(2.5f).toInt()
            for (i in 0 until itemCount) {
                val point = View(binding.indicator.context)
                point.setBackgroundResource(R.drawable.selector_indicator)
                binding.indicator.addView(point, layoutParams)
            }
            binding.indicator.getChildAt(0).isSelected = true
        }
    }

    private fun makeSureIndicator(position: Int) {
        val itemCount = binding.indicator.childCount
        if (position in 0 until itemCount) {
            for (i in 0 until itemCount) {
                binding.indicator.getChildAt(i).isSelected = false
            }
            binding.indicator.getChildAt(position).isSelected = true
        }
    }

}