package com.example.fragment.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.view.OnLoadMoreListener
import com.example.fragment.library.base.view.OnRefreshListener
import com.example.fragment.library.base.view.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.R
import com.example.fragment.module.user.databinding.FragmentMyShareBinding
import com.example.fragment.module.user.model.UserViewModel

class MyShareFragment : RouterFragment() {

    private val articleAdapter = ArticleAdapter()
    private val articleChildClickListener = object : BaseAdapter.OnItemChildClickListener {
        override fun onItemChildClick(
            view: View,
            holder: BaseAdapter.ViewBindHolder,
            position: Int
        ) {
            val item = articleAdapter.getItem(position)
            when (view.id) {
                R.id.rl_item -> {
                    val args = Bundle()
                    args.putString(Keys.URL, item.link)
                    activity.navigation(R.id.action_my_collect_to_web, args)
                }
            }
        }
    }

    private val viewModel: UserViewModel by viewModels()
    private var _binding: FragmentMyShareBinding? = null
    private val binding get() = _binding!!

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
        articleAdapter.setOnItemChildClickListener(articleChildClickListener)
        binding.black.setOnClickListener { activity.onBackPressed() }
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.myShareArticle(true)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.myShareArticle(false)
            }
        })
    }

    override fun initViewModel() {
        viewModel.myShareArticleResult.observe(viewLifecycleOwner) { result ->
            when {
                result.errorCode == "0" -> {
                    result.data?.shareArticles?.datas?.let { list ->
                        if (viewModel.isRefresh) {
                            articleAdapter.setNewData(list)
                        } else {
                            articleAdapter.addData(list)
                        }
                    }
                }
                result.errorCode.isNotBlank() && result.errorMsg.isNotBlank() -> {
                    activity.showTips(result.errorMsg)
                }
            }
            binding.pullRefresh.finishRefresh()
            binding.pullRefresh.setLoadMore(viewModel.page < viewModel.pageCont)
        }
    }

    override fun onLoad() {
        if(viewModel.myShareArticleResult.value == null){
            viewModel.myShareArticle(true)
        }
    }

}