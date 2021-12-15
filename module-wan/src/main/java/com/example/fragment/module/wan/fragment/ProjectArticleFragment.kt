package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.view.pull.OnLoadMoreListener
import com.example.fragment.library.base.view.pull.OnRefreshListener
import com.example.fragment.library.base.view.pull.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.databinding.FragmentProjectArticleBinding
import com.example.fragment.module.wan.model.ProjectViewModel

class ProjectArticleFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): ProjectArticleFragment {
            return ProjectArticleFragment()
        }
    }

    private val viewModel: ProjectViewModel by viewModels()
    private var _binding: FragmentProjectArticleBinding? = null
    private val binding get() = _binding!!

    private val articleAdapter = ArticleAdapter()
    private var cid = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectArticleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        cid = requireArguments().getString(Keys.CID).toString()
        //项目列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getProject(cid)
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getProjectNext(cid)
            }
        })
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.projectListResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> if (viewModel.isHomePage()) {
                    articleAdapter.setNewData(result.data?.datas)
                } else {
                    articleAdapter.addData(result.data?.datas)
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
        if (viewModel.projectListResult.value == null) {
            viewModel.getProject(cid)
        }
    }

}