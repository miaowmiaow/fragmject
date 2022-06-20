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
import androidx.fragment.app.activityViewModels
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
import com.example.fragment.module.wan.model.HotKeyViewModel
import com.example.fragment.module.wan.model.SearchViewModel

class SearchFragment : RouterFragment() {

    private val hotKeyViewModel: HotKeyViewModel by activityViewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private var _binding: SearchFragmentBinding? = null
    private val binding get() = _binding!!
    private var _articleAdapter: ArticleAdapter? = null
    private val articleAdapter get() = _articleAdapter!!
    private var _historySearchAdapter: SearchHistoryAdapter? = null
    private val historySearchAdapter get() = _historySearchAdapter!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SearchFragmentBinding.inflate(inflater, container, false)
        _articleAdapter = ArticleAdapter()
        _historySearchAdapter = SearchHistoryAdapter()
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        hideInputMethod()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _articleAdapter = null
        _historySearchAdapter = null
        _binding = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView() {
        binding.search.setText(requireArguments().getString(Keys.VALUE).toString())
        binding.cancel.setOnClickListener { activity.onBackPressed() }
        //搜索
        binding.search.setOnTouchListener { _, _ ->
            searchViewModel.getSearchHistory()
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
        historySearchAdapter.setOnItemClickListener(object : BaseAdapter.OnItemClickListener {
            override fun onItemClick(holder: BaseAdapter.ViewBindHolder, position: Int) {
                historySearchAdapter.getItem(position).apply {
                    search(this)
                }
            }
        })
        historySearchAdapter.setOnItemChildClickListener(
            object : BaseAdapter.OnItemChildClickListener {
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
            })
        //搜索结果列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                searchViewModel.getArticleQueryHome(binding.search.text.toString())
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                searchViewModel.getArticleQueryNext(binding.search.text.toString())
            }
        })
    }

    override fun initViewModel(): BaseViewModel {
        hotKeyViewModel.hotKeyResult().observe(viewLifecycleOwner) {
            binding.history.visibility = VISIBLE
            binding.pullRefresh.visibility = GONE
            binding.hotKey.visibility = if (it.isNotEmpty()) VISIBLE else GONE
            binding.fbl.removeAllViews()
            it.forEach { hotKey ->
                val inflater = LayoutInflater.from(binding.fbl.context)
                val tv = inflater.inflate(R.layout.hot_key_fbl, binding.fbl, false)
                (tv as TextView).text = hotKey.name
                tv.setOnClickListener { search(hotKey.name) }
                binding.fbl.addView(tv)
            }
        }
        searchViewModel.searchHistoryResult().observe(viewLifecycleOwner) {
            binding.history.visibility = VISIBLE
            binding.pullRefresh.visibility = GONE
            binding.searchHistory.visibility = if (it.isNotEmpty()) VISIBLE else GONE
            historySearchAdapter.setNewData(it)
        }
        val key = binding.search.text.toString()
        searchViewModel.articleQueryResult(key).observe(viewLifecycleOwner) {
            httpParseSuccess(it) { result ->
                result.data?.datas?.let { data ->
                    if (searchViewModel.isHomePage()) {
                        articleAdapter.setNewData(data)
                    } else {
                        articleAdapter.addData(data)
                    }
                }
            }
            //结束下拉刷新状态
            binding.pullRefresh.finishRefresh()
            //设置加载更多状态
            binding.pullRefresh.setLoadMore(searchViewModel.hasNextPage())
        }
        return searchViewModel
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