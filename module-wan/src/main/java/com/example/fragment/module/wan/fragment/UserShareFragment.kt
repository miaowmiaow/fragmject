package com.example.fragment.module.wan.fragment

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
import com.example.fragment.module.wan.R
import com.example.fragment.module.wan.databinding.FragmentUserShareBinding
import com.example.fragment.module.wan.model.ShareModel

class UserShareFragment : RouterFragment() {

    private var id: String = ""
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
                    activity.navigation(R.id.action_user_share_to_web, args)
                }
            }
        }
    }

    private val viewModel: ShareModel by viewModels()
    private var _binding: FragmentUserShareBinding? = null
    private val binding get() = _binding!!

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
        articleAdapter.setOnItemChildClickListener(articleChildClickListener)
        binding.black.setOnClickListener { activity.onBackPressed() }
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.userShare(true, id)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.userShare(false, id)
            }
        })
    }

    override fun initViewModel() {
        arguments?.apply {
            id = this.getString(Keys.UID).toString()
        }
        viewModel.userShareResult.observe(viewLifecycleOwner) { result ->
            if (result.errorCode == "0") {
                result.data?.coinInfo?.let { coin ->
                    binding.title.text = coin.username
                    binding.id.text = coin.userId
                    binding.coinCount.text = coin.coinCount
                }
                result.data?.shareArticles?.datas?.let { list ->
                    if (viewModel.isRefresh) {
                        articleAdapter.setNewData(list)
                    } else {
                        articleAdapter.addData(list)
                    }
                }
            }
            if (result.errorCode.isNotBlank() && result.errorMsg.isNotBlank()) {
                activity.showTips(result.errorMsg)
            }
            binding.pullRefresh.finishRefresh()
            binding.pullRefresh.setLoadMore(viewModel.page < viewModel.pageCont)
        }
    }

    override fun onLoad() {
        if(viewModel.userShareResult.value == null){
            viewModel.userShare(true, id)
        }
    }

}