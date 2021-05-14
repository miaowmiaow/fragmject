package com.example.fragment.module.home.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.library.base.component.view.SimplePullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.home.R
import com.example.fragment.module.home.databinding.FragmentSquareBinding
import com.example.fragment.module.home.model.SquareViewModel

class SquareFragment : ViewModelFragment<FragmentSquareBinding, SquareViewModel>() {

    companion object {
        @JvmStatic
        fun newInstance(): SquareFragment {
            return SquareFragment()
        }
    }

    private val articleAdapter = ArticleAdapter()

    override fun setViewBinding(inflater: LayoutInflater): FragmentSquareBinding {
        return FragmentSquareBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
    }

    private fun setupView() {
        articleAdapter.setOnItemChildClickListener(object : BaseAdapter.OnItemChildClickListener {
            override fun onItemChildClick(
                view: View,
                holder: BaseAdapter.ViewBindHolder,
                position: Int
            ) {
                val item = articleAdapter.getItem(position)
                if (view.id == R.id.iv_collect) {
                    if (item.collect) {
                        viewModel.unCollect(item.id).observe(viewLifecycleOwner, { result ->
                            if (result.errorCode == "0") {
                                (view as ImageView).setImageResource(R.drawable.ic_collect_unchecked_stroke)
                                item.collect = false
                            }
                        })
                    } else {
                        viewModel.collect(item.id).observe(viewLifecycleOwner, { result ->
                            if (result.errorCode == "0") {
                                (view as ImageView).setImageResource(R.drawable.ic_collect_checked)
                                item.collect = true
                            }
                        })
                    }
                }
            }
        })
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.pullRefresh.setOnRefreshListener(object :
            SimplePullRefreshLayout.OnRefreshListener {
            override fun onRefresh(refreshLayout: SimplePullRefreshLayout) {
                viewModel.getUserArticleList(true)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object :
            SimplePullRefreshLayout.OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: SimplePullRefreshLayout) {
                viewModel.getUserArticleList(false)
            }
        })
        binding.pullRefresh.setRefreshing()
    }

    private fun update() {
        viewModel.userArticleResult.observe(viewLifecycleOwner, { result ->
            if (result.errorCode == "0") {
                result.data?.datas?.let { list ->
                    if (viewModel.isRefresh) {
                        articleAdapter.setNewData(list)
                    } else {
                        articleAdapter.addData(list)
                        binding.pullRefresh.setLoadMore(true)
                    }
                }
            } else if (result.errorCode.isNotBlank()) {
                baseActivity.showTips(result.errorMsg)
            }
            if (binding.pullRefresh.isRefresh()) {
                binding.pullRefresh.finishRefresh()
            }
            if (viewModel.page >= viewModel.pageCont) {
                binding.pullRefresh.setLoadMore(false)
            }
        })
    }

}