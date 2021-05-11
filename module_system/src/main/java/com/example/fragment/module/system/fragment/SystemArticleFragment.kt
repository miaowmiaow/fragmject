package com.example.fragment.module.system.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.library.base.component.view.SimplePullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.constant.Argument
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.system.databinding.FragmentSystemArticleBinding
import com.example.fragment.module.system.model.SystemViewModel

class SystemArticleFragment : ViewModelFragment<FragmentSystemArticleBinding, SystemViewModel>() {

    companion object {
        @JvmStatic
        fun newInstance(cid: String): SystemArticleFragment {
            val fragment = SystemArticleFragment()
            val args = Bundle()
            args.putString(Argument.CID, cid)
            fragment.arguments = args
            return fragment
        }
    }

    private val articleAdapter = ArticleAdapter()
    private var cid = ""

    override fun setViewBinding(inflater: LayoutInflater): FragmentSystemArticleBinding {
        return FragmentSystemArticleBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.apply {
            cid = this.getString(Argument.CID).toString()
        }
        setupView()
        update()
    }

    private fun setupView() {
        articleAdapter.setOnItemClickListener(object : BaseAdapter.OnItemClickListener{
            override fun onItemClick(holder: BaseAdapter.ViewBindHolder, position: Int) {
                articleAdapter.getItem(position)?.let { article ->
                    val args = Bundle()
                    args.putString(Argument.URL, article.link)
                    baseActivity.navigation(Router.WEB, args)
                }
            }
        })
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.pullRefresh.setOnRefreshListener(object :
            SimplePullRefreshLayout.OnRefreshListener {
            override fun onRefresh(refreshLayout: SimplePullRefreshLayout) {
                viewModel.getTreeList(true, cid)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object :
            SimplePullRefreshLayout.OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: SimplePullRefreshLayout) {
                viewModel.getTreeList(false, cid)
            }
        })
        binding.pullRefresh.setRefreshing()
    }

    private fun update() {
        viewModel.treeListResult.observe(viewLifecycleOwner, { result ->
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