package com.example.fragment.module.user.fragment

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
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.databinding.FragmentUserShareBinding
import com.example.fragment.module.user.model.UserShareViewModel

class UserShareFragment : RouterFragment() {

    private val viewModel: UserShareViewModel by viewModels()
    private var _binding: FragmentUserShareBinding? = null
    private val binding get() = _binding!!

    private val articleAdapter = ArticleAdapter()
    private var id: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        id = requireArguments().getString(Keys.UID).toString()
        binding.black.setOnClickListener { activity.onBackPressed() }
        //用户分享 列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getUserShareArticles(id)
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getUserShareArticlesNext(id)
            }
        })
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.userShareArticleResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> {
                    result.data?.coinInfo?.let { coin ->
                        binding.title.text = coin.username
                        binding.id.text = coin.userId
                        binding.coinCount.text = coin.coinCount
                    }
                    if (viewModel.isHomePage()) {
                        articleAdapter.setNewData(result.data?.shareArticles?.datas)
                    } else {
                        articleAdapter.addData(result.data?.shareArticles?.datas)
                    }
                }
                else -> activity.showTips(result.errorMsg)
            }
            //结束下拉刷新状态
            binding.pullRefresh.finishRefresh()
            //设置加载更多状态
            binding.pullRefresh.setLoadMore(viewModel.hasNextPage())
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.userShareArticleResult.value == null) {
            viewModel.getUserShareArticles(id)
        }
    }

}