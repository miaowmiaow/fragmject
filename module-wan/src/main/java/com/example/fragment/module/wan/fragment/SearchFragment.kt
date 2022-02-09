package com.example.fragment.module.wan.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.view.pull.OnLoadMoreListener
import com.example.fragment.library.base.view.pull.OnRefreshListener
import com.example.fragment.library.base.view.pull.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.wan.R
import com.example.fragment.module.wan.adapter.SearchHistoryAdapter
import com.example.fragment.module.wan.databinding.SearchFragmentBinding
import com.example.fragment.module.wan.model.SearchViewModel

class SearchFragment : RouterFragment() {

    private val viewModel: SearchViewModel by viewModels()
    private var _binding: SearchFragmentBinding? = null
    private val binding get() = _binding!!

    private val historySearchAdapter = SearchHistoryAdapter()
    private val historySearchClickListener = object : BaseAdapter.OnItemClickListener {
        override fun onItemClick(holder: BaseAdapter.ViewBindHolder, position: Int) {
            historySearchAdapter.getItem(position).apply {
                search(this)
            }
        }
    }
    private val historySearchChildClickListener = object : BaseAdapter.OnItemChildClickListener {
        override fun onItemChildClick(
            view: View,
            holder: BaseAdapter.ViewBindHolder,
            position: Int
        ) {
            if (view.id == R.id.delete) {
                historySearchAdapter.removeData(position)
                val data = historySearchAdapter.getData()
                binding.searchHistory.visibility = if (data.isNotEmpty()) VISIBLE else GONE
                WanHelper.setSearchHistory(data)
            }
        }
    }
    private val articleAdapter = ArticleAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SearchFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        hideInputMethod()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView() {
        binding.search.setText(requireArguments().getString(Keys.VALUE).toString())
        binding.cancel.setOnClickListener { activity.onBackPressed() }
        //搜索
        binding.search.setOnTouchListener { _, _ ->
            viewModel.getSearchHistory()
            false
        }
        binding.search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search(binding.search.text.toString())
                return@setOnEditorActionListener true
            }
            false
        }
        //搜索历史
        binding.historyList.layoutManager = LinearLayoutManager(binding.list.context)
        binding.historyList.adapter = historySearchAdapter
        historySearchAdapter.setOnItemClickListener(historySearchClickListener)
        historySearchAdapter.setOnItemChildClickListener(historySearchChildClickListener)
        //搜索结果列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getSearch(binding.search.text.toString())
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getSearchNext(binding.search.text.toString())
            }
        })
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.hotKeyResult.observe(viewLifecycleOwner) { result ->
            binding.history.visibility = VISIBLE
            binding.pullRefresh.visibility = GONE
            binding.hotKey.visibility = if (result.isNotEmpty()) VISIBLE else GONE
            binding.fbl.removeAllViews()
            result.forEach { hotKey ->
                val inflater = LayoutInflater.from(binding.fbl.context)
                val tv = inflater.inflate(R.layout.hot_key_fbl, binding.fbl, false)
                (tv as TextView).text = hotKey.name
                tv.setOnClickListener { search(hotKey.name) }
                binding.fbl.addView(tv)
            }
        }
        viewModel.searchHistoryResult.observe(viewLifecycleOwner) { result ->
            binding.history.visibility = VISIBLE
            binding.pullRefresh.visibility = GONE
            binding.searchHistory.visibility = if (result.isNotEmpty()) VISIBLE else GONE
            historySearchAdapter.setNewData(result)
        }
        viewModel.searchResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> result.data?.datas?.let { data ->
                    if (viewModel.isHomePage()) {
                        articleAdapter.setNewData(data)
                    } else {
                        articleAdapter.addData(data)
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
        if (viewModel.hotKeyResult.value == null) {
            viewModel.getHotKey()
        }
        if (viewModel.searchHistoryResult.value == null) {
            viewModel.getSearchHistory()
        }
    }

    private fun search(key: String) {
        if (checkParameter(key)) {
            binding.history.visibility = GONE
            binding.pullRefresh.visibility = VISIBLE
            binding.search.setText(key)
            binding.pullRefresh.setRefreshing()
            val list = historySearchAdapter.getData()
            if (list.contains(key)) {
                list.remove(key)
            }
            list.add(0, key)
            WanHelper.setSearchHistory(list)
        }
    }

    private fun checkParameter(title: String): Boolean {
        if (title.isBlank()) {
            activity.showTips("搜索关键词不能为空")
            return false
        }
        return true
    }

}