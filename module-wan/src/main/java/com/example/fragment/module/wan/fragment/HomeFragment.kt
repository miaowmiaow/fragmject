package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.toppingToPosition
import com.example.fragment.library.base.view.pull.OnLoadMoreListener
import com.example.fragment.library.base.view.pull.OnRefreshListener
import com.example.fragment.library.base.view.pull.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.model.TabEventViewMode
import com.example.fragment.module.wan.databinding.HomeFragmentBinding
import com.example.fragment.module.wan.model.HomeViewModel

class HomeFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    private val tabEventViewModel: TabEventViewMode by activityViewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private var _binding: HomeFragmentBinding? = null
    private val binding get() = _binding!!
    private val articleAdapter = ArticleAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 解决 LiveData 粘性事件问题，防止缓存的数据都重复添加到List
        // 还可以通过 SingleLiveEvent 解决该问题，但是个人不太推荐
        homeViewModel.clearArticleListResult()
        // Fragment 的存在时间比其视图长。请务必在 Fragment 的 onDestroyView() 方法中清除对视图的所有引用。
        binding.pullRefresh.recycler()
        binding.list.adapter = null
        _binding = null
    }

    override fun initView() {
        //文章列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                homeViewModel.getArticleHome()
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                homeViewModel.getArticleNext()
            }
        })
    }

    override fun initViewModel(): BaseViewModel {
        tabEventViewModel.homeTab().observe(viewLifecycleOwner) {
            if (it == 1) {
                binding.list.toppingToPosition(0)
                tabEventViewModel.setHomeTab(0)
            }
        }
        homeViewModel.articleListResult().observe(viewLifecycleOwner) {
            if (homeViewModel.isHomePage()) {
                articleAdapter.setNewData(it)
            } else {
                articleAdapter.addData(it)
            }
            //结束下拉刷新状态
            binding.pullRefresh.finishRefresh()
            //设置加载更多状态
            binding.pullRefresh.setLoadMore(homeViewModel.hasNextPage())
        }
        return homeViewModel
    }

}