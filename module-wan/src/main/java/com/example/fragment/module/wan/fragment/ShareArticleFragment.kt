package com.example.fragment.module.wan.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.view.pull.OnLoadMoreListener
import com.example.fragment.library.base.view.pull.OnRefreshListener
import com.example.fragment.library.base.view.pull.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.databinding.ShareArticleFragmentBinding
import com.example.fragment.module.wan.model.ShareArticleViewModel

class ShareArticleFragment : RouterFragment() {

    private val viewModel: ShareArticleViewModel by viewModels()
    private var _binding: ShareArticleFragmentBinding? = null
    private val binding get() = _binding!!
    private val articleAdapter = ArticleAdapter()
    private var id: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShareArticleFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearUserShareArticleResult()
        binding.pullRefresh.recycler()
        binding.list.adapter = null
        _binding = null
    }

    override fun initView() {
        id = requireArguments().getString(Keys.UID).toString()
        binding.black.setOnClickListener { onBackPressed() }
        //用户分享 列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getUserShareArticlesHome(id)
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getUserShareArticlesNext(id)
            }
        })
    }

    @SuppressLint("SetTextI18n")
    override fun initViewModel(): BaseViewModel {
        viewModel.userShareArticleResult(id).observe(viewLifecycleOwner) {
            httpParseSuccess(it) { result ->
                result.data?.coinInfo?.let { coin ->
                    binding.title.text = coin.username
                    binding.id.text = "id:${coin.userId}"
                    binding.coinCount.text = "积分:${coin.coinCount}"
                }
                if (viewModel.isHomePage()) {
                    articleAdapter.setNewData(result.data?.shareArticles?.datas)
                } else {
                    articleAdapter.addData(result.data?.shareArticles?.datas)
                }
            }
            //结束下拉刷新状态
            binding.pullRefresh.finishRefresh()
            //设置加载更多状态
            binding.pullRefresh.setLoadMore(viewModel.hasNextPage())
        }
        return viewModel
    }

}