package com.example.fragment.module.home.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.library.base.component.view.SimplePullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.constant.Argument
import com.example.fragment.library.common.constant.Router
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

    override fun setViewBinding(inflater: LayoutInflater): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
        viewModel.getBanner()
    }

    private fun setupView() {
        articleAdapter.setOnItemClickListener(object : BaseAdapter.OnItemClickListener{
            override fun onItemClick(holder: BaseAdapter.ViewBindHolder, position: Int) {
                articleAdapter.getItem(position)?.let { article ->
                    val args = Bundle()
                    args.putString(Argument.URL, article.link)
                    baseActivity.navigation(Router.WEB, args)
                }
            }
        })
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.pullRefresh.setOnRefreshListener(object :
            SimplePullRefreshLayout.OnRefreshListener {
            override fun onRefresh(refreshLayout: SimplePullRefreshLayout) {
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
            result.data?.apply {
                articleAdapter.setBannerData(this)
            }
        })
        viewModel.articleTopResult.observe(viewLifecycleOwner, { result ->
            result.data?.let { list ->
                list.forEach {
                    it.top = true
                }
                articleAdapter.addData(0, list)
            }
        })
        viewModel.articleListResult.observe(viewLifecycleOwner, { result ->
            result.data?.datas?.let { list ->
                if (viewModel.isRefresh) {
                    articleAdapter.setNewData(list)
                } else {
                    articleAdapter.addData(list)
                    binding.pullRefresh.setLoadMore(true)
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