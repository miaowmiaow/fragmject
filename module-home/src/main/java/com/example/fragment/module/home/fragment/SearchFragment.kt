package com.example.fragment.module.home.fragment

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
import com.example.fragment.library.base.view.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.home.R
import com.example.fragment.module.home.adapter.HistorySearchAdapter
import com.example.fragment.module.home.databinding.FragmentSearchBinding
import com.example.fragment.module.home.model.HomeViewModel

class SearchFragment : RouterFragment() {

    private val historySearchAdapter = HistorySearchAdapter()
    private val articleAdapter = ArticleAdapter()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.apply {
            binding.search.setText(this.getString(Keys.TITLE).toString())
        }
        setupView()
        update()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupView() {
        binding.cancel.setOnClickListener { baseActivity.onBackPressed() }
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
        historySearchAdapter.setOnItemClickListener(object : BaseAdapter.OnItemClickListener {
            override fun onItemClick(holder: BaseAdapter.ViewBindHolder, position: Int) {
                historySearchAdapter.getItem(position).apply {
                    search(this)
                }
            }
        })
        historySearchAdapter.setOnItemChildClickListener(object :
            BaseAdapter.OnItemChildClickListener {
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
        })
        binding.historyList.layoutManager = LinearLayoutManager(binding.list.context)
        binding.historyList.adapter = historySearchAdapter
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.pullRefresh.setOnRefreshListener(object :
            PullRefreshLayout.OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                val key = binding.search.text.toString()
                viewModel.search(true, key)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object :
            PullRefreshLayout.OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                val key = binding.search.text.toString()
                viewModel.search(false, key)
            }
        })
    }

    private fun update() {
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
        initHistorySearch()
        viewModel.searchResult.observe(viewLifecycleOwner) { result ->
            if (result.errorCode == "0") {
                result.data?.datas?.let { list ->
                    if (viewModel.isRefresh) {
                        articleAdapter.setNewData(list)
                    } else {
                        articleAdapter.addData(list)
                    }
                }
            }
            if (result.errorCode.isNotBlank() && result.errorMsg.isNotBlank()) {
                baseActivity.showTips(result.errorMsg)
            }
            binding.pullRefresh.finishRefresh()
            binding.pullRefresh.setLoadMore(viewModel.page <= viewModel.pageCont)
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

    private fun initHistorySearch() {
        binding.history.visibility = View.VISIBLE
        binding.pullRefresh.visibility = View.GONE
        WanHelper.getHistorySearch().observe(viewLifecycleOwner) { result ->
            binding.historySearch.visibility = if (result.isNotEmpty()) View.VISIBLE else View.GONE
            historySearchAdapter.setNewData(result)
        }
    }

    private fun checkParameter(title: String): Boolean {
        if (title.isBlank()) {
            baseActivity.showTips("搜索关键词不能为空")
            return false
        }
        return true
    }

}