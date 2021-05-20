package com.example.fragment.module.project.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.component.view.SimplePullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.project.databinding.FragmentProjectArticleBinding
import com.example.fragment.module.project.model.ProjectViewModel

class ProjectArticleFragment :
    ViewModelFragment<FragmentProjectArticleBinding, ProjectViewModel>() {

    companion object {
        @JvmStatic
        fun newInstance(cid: String): ProjectArticleFragment {
            val fragment = ProjectArticleFragment()
            val args = Bundle()
            args.putString(Keys.CID, cid)
            fragment.arguments = args
            return fragment
        }
    }

    private val articleAdapter = ArticleAdapter()
    private var cid = ""

    override fun setViewBinding(inflater: LayoutInflater): FragmentProjectArticleBinding {
        return FragmentProjectArticleBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.apply {
            cid = this.getString(Keys.CID).toString()
        }
        setupView()
        update()
    }

    override fun onUserStatusUpdate(userBean: UserBean) {
        binding.pullRefresh.setRefreshing()
    }

    private fun setupView() {
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.pullRefresh.setOnRefreshListener(object :
            SimplePullRefreshLayout.OnRefreshListener {
            override fun onRefresh(refreshLayout: SimplePullRefreshLayout) {
                viewModel.getProjectList(true, cid)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object :
            SimplePullRefreshLayout.OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: SimplePullRefreshLayout) {
                viewModel.getProjectList(false, cid)
            }
        })
        binding.pullRefresh.setRefreshing()
    }

    private fun update() {
        viewModel.projectListResult.observe(viewLifecycleOwner, { result ->
            if (result.errorCode == "0") {
                result.data?.datas?.let { list ->
                    if (viewModel.isRefresh) {
                        articleAdapter.setNewData(list)
                    } else {
                        articleAdapter.addData(list)
                        binding.pullRefresh.setLoadMore(true)
                    }
                }
            }
            if (result.errorMsg.isNotBlank()) {
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