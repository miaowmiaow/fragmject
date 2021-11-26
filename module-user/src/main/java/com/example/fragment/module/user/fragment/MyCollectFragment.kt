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
import com.example.fragment.module.user.databinding.FragmentMyCollectBinding
import com.example.fragment.module.user.model.MyCollectViewModel

class MyCollectFragment : RouterFragment() {

    private val viewModel: MyCollectViewModel by activityViewModels()
    private var _binding: FragmentMyCollectBinding? = null
    private val binding get() = _binding!!

    private val articleAdapter = ArticleAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyCollectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        binding.black.setOnClickListener { activity.onBackPressed() }
        //我的收藏列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getMyCollectArticle()
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getMyCollectArticleNext()
            }
        })
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.myCollectArticleResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> {
                    result.data?.datas?.let { data ->
                        data.forEach { it.collect = true }
                        if (viewModel.isHomePage()) {
                            articleAdapter.setNewData(data)
                        } else {
                            articleAdapter.addData(data)
                        }
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
        if (viewModel.myCollectArticleResult.value == null) {
            viewModel.getMyCollectArticle()
        }
    }

}