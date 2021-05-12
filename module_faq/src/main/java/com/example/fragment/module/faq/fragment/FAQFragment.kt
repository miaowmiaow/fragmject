package com.example.fragment.module.faq.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.library.base.component.view.SimplePullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.faq.databinding.FragmentFaqBinding
import com.example.fragment.module.faq.model.FAQViewModel

class FAQFragment : ViewModelFragment<FragmentFaqBinding, FAQViewModel>() {

    companion object {
        @JvmStatic
        fun newInstance(): FAQFragment {
            return FAQFragment()
        }
    }

    private val articleAdapter = ArticleAdapter()

    override fun setViewBinding(inflater: LayoutInflater): FragmentFaqBinding {
        return FragmentFaqBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
    }

    private fun setupView() {
        articleAdapter.setOnItemClickListener(object : BaseAdapter.OnItemClickListener{
            override fun onItemClick(holder: BaseAdapter.ViewBindHolder, position: Int) {
                articleAdapter.getItem(position)?.let { article ->
                    val args = Bundle()
                    args.putString(Keys.URL, article.link)
                    baseActivity.navigation(Router.WEB, args)
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
        viewModel.wendaResult.observe(viewLifecycleOwner, { result ->
            result.data?.datas?.let { list ->
                if (viewModel.isRefresh) {
                    articleAdapter.setNewData(list)
                } else {
                    articleAdapter.addData(list)
                    binding.pullRefresh.setLoadMore(true)
                }
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