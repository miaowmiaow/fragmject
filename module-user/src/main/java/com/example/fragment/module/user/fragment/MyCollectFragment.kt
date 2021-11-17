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
import com.example.fragment.module.user.databinding.FragmentMyCollectBinding
import com.example.fragment.module.user.model.UserViewModel

class MyCollectFragment : RouterFragment() {

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
    private var _binding: FragmentMyCollectBinding? = null
    private val binding get() = _binding!!

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
        articleAdapter.setOnItemChildClickListener(articleChildClickListener)
        binding.black.setOnClickListener { activity.onBackPressed() }
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.myCollectArticle(true)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.myCollectArticle(false)
            }
        })
    }

    override fun initViewModel() {
        viewModel.myCollectArticleResult.observe(viewLifecycleOwner) { result ->
            when {
                result.errorCode == "0" -> {
                    result.data?.datas?.let { list ->
                        list.forEach {
                            it.collect = true
                        }
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
        if (viewModel.myCollectArticleResult.value == null) {
            viewModel.myCollectArticle(true)
        }
    }

}