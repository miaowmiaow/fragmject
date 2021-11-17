package com.example.fragment.module.wan.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.view.OnLoadMoreListener
import com.example.fragment.library.base.view.OnRefreshListener
import com.example.fragment.library.base.view.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.wan.R
import com.example.fragment.module.wan.adapter.HistorySearchAdapter
import com.example.fragment.module.wan.databinding.FragmentSearchBinding
import com.example.fragment.module.wan.model.HomeViewModel

class SearchFragment : RouterFragment() {

    private val historySearchAdapter = HistorySearchAdapter()
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
                binding.historySearch.visibility =
                    if (data.isNotEmpty()) View.VISIBLE else View.GONE
                WanHelper.setHistorySearch(data)
            }
        }
    }
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
                    activity.navigation(R.id.action_search_to_web, args)
                }
            }
        }
    }

    private val viewModel: HomeViewModel by viewModels()
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView() {
        historySearchAdapter.setOnItemClickListener(historySearchClickListener)
        historySearchAdapter.setOnItemChildClickListener(historySearchChildClickListener)
        articleAdapter.setOnItemChildClickListener(articleChildClickListener)
        binding.cancel.setOnClickListener { activity.onBackPressed() }
        binding.search.setOnTouchListener { _, _ ->
            initHistorySearch()
            false
        }
        binding.search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search(binding.search.text.toString())
                return@setOnEditorActionListener true
            }
            false
        }
        binding.historyList.layoutManager = LinearLayoutManager(binding.list.context)
        binding.historyList.adapter = historySearchAdapter
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.search(true, binding.search.text.toString())
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.search(false, binding.search.text.toString())
            }
        })
    }

    override fun initViewModel() {
        arguments?.apply {
            binding.search.setText(this.getString(Keys.TITLE).toString())
        }
        viewModel.searchResult.observe(viewLifecycleOwner) { result ->
            if (result.errorCode == "0") {
                result.data?.datas?.let { list ->
                    if (viewModel.isRefresh) {
                        articleAdapter.setNewData(list)
                    } else {
                        articleAdapter.addData(list)
                    }
                }
            } else if (result.errorCode.isNotBlank() && result.errorMsg.isNotBlank()) {
                activity.showTips(result.errorMsg)
            }
            binding.pullRefresh.finishRefresh()
            binding.pullRefresh.setLoadMore(viewModel.page < viewModel.pageCont)
        }
        WanHelper.getHotKey().observe(viewLifecycleOwner) { result ->
            binding.hotKey.visibility = if (result.isNotEmpty()) View.VISIBLE else View.GONE
            binding.fbl.removeAllViews()
            result.forEach { hotKey ->
                val inflater = LayoutInflater.from(binding.fbl.context)
                val tv = inflater.inflate(R.layout.fbl_hot_key, binding.fbl, false) as TextView
                tv.text = hotKey.name
                tv.setOnClickListener {
                    search(hotKey.name)
                }
                binding.fbl.addView(tv)
            }
        }
    }

    override fun onLoad() {
    }

    private fun initHistorySearch() {
        binding.history.visibility = View.VISIBLE
        binding.pullRefresh.visibility = View.GONE
        WanHelper.getHistorySearch().observe(viewLifecycleOwner) { result ->
            binding.historySearch.visibility = if (result.isNotEmpty()) View.VISIBLE else View.GONE
            historySearchAdapter.setNewData(result)
        }
    }

    private fun search(key: String) {
        if (checkParameter(key)) {
            binding.history.visibility = View.GONE
            binding.pullRefresh.visibility = View.VISIBLE
            binding.search.setText(key)
            binding.pullRefresh.setRefreshing()
            val list = historySearchAdapter.getData()
            if (list.contains(key)) {
                list.remove(key)
            }
            list.add(0, key)
            WanHelper.setHistorySearch(list)
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