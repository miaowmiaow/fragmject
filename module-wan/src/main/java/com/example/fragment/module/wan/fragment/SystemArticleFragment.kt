package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.view.OnLoadMoreListener
import com.example.fragment.library.base.view.OnRefreshListener
import com.example.fragment.library.base.view.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.databinding.FragmentSystemArticleBinding
import com.example.fragment.module.wan.model.SystemViewModel

class SystemArticleFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): SystemArticleFragment {
            return SystemArticleFragment()
        }
    }

    private val viewModel: SystemViewModel by viewModels()
    private var _binding: FragmentSystemArticleBinding? = null
    private val binding get() = _binding!!

    private val articleAdapter = ArticleAdapter()
    private var cid = ""

    init {
        delayedLoad = 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSystemArticleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        cid = arguments?.getString(Keys.CID).toString()
        //体系列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.addItemDecoration(
            object : DividerItemDecoration(binding.list.context, VERTICAL) {}
        )
        binding.list.adapter = articleAdapter
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getSystemArticle(cid)
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getSystemArticleNext(cid)
            }
        })
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.systemArticleResult.observe(viewLifecycleOwner) { result ->
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
        if (viewModel.systemArticleResult.value == null) {
            viewModel.getSystemArticle(cid)
        }
    }

}