package com.example.fragment.module.system.fragment

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
import com.example.fragment.module.system.databinding.FragmentSystemArticleBinding
import com.example.fragment.module.system.model.SystemViewModel

class SystemArticleFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(cid: String): SystemArticleFragment {
            val fragment = SystemArticleFragment()
            val args = Bundle()
            args.putString(Keys.CID, cid)
            fragment.arguments = args
            return fragment
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
        setupView()
        update()
    }

    private fun setupView() {
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
        binding.pullRefresh.setRefreshing()
    }

    private fun update() {
        viewModel.treeListResult.observe(viewLifecycleOwner, { result ->
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