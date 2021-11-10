package com.example.fragment.module.home.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.view.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.home.databinding.FragmentSystemArticleBinding
import com.example.fragment.module.home.model.SystemViewModel

class SystemArticleFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): SystemArticleFragment {
            return SystemArticleFragment()
        }
    }

    private var cid = ""
    private val articleAdapter = ArticleAdapter()

    private val viewModel: SystemViewModel by viewModels()
    private var _binding: FragmentSystemArticleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSystemArticleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.apply {
            cid = this.getString(Keys.CID).toString()
        }
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.pullRefresh.setOnRefreshListener(object :
            PullRefreshLayout.OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getTreeList(true, cid)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object :
            PullRefreshLayout.OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getTreeList(false, cid)
            }
        })
        viewModel.treeListResult.observe(viewLifecycleOwner) { result ->
            if (result.errorCode == "0") {
                result.data?.datas?.let { list ->
                    if (viewModel.isRefresh) {
                        articleAdapter.setNewData(list)
                    } else {
                        articleAdapter.addData(list)
                    }
                }
            } else if (result.errorCode.isNotBlank()) {
                baseActivity.showTips(result.errorMsg)
            }
            binding.pullRefresh.finishRefresh()
            binding.pullRefresh.setLoadMore(viewModel.page < viewModel.pageCont)
        }
        binding.pullRefresh.setRefreshing()
    }

}