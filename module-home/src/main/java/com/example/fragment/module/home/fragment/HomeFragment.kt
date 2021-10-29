package com.example.fragment.module.home.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.view.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.home.databinding.FragmentHomeBinding
import com.example.fragment.module.home.model.HomeViewModel

class HomeFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    private val articleAdapter = ArticleAdapter()
    private val viewModel: HomeViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            PullRefreshLayout.OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getBanner()
                viewModel.getArticleList(true)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object :
            PullRefreshLayout.OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getArticleList(false)
            }
        })
        binding.pullRefresh.setRefreshing()
    }

    private fun update() {
        viewModel.bannerResult.observe(viewLifecycleOwner, { result ->
            if (result.errorCode == "0") {
                result.data?.apply {
                    articleAdapter.setBannerData(this)
                }
            }
            if (result.errorCode.isNotBlank() && result.errorMsg.isNotBlank()) {
                baseActivity.showTips(result.errorMsg)
            }
        })
        viewModel.articleTopResult.observe(viewLifecycleOwner, { result ->
            if (result.errorCode == "0") {
                result.data?.let { list ->
                    list.forEach {
                        it.top = true
                    }
                    articleAdapter.addData(0, list)
                }
            }
            if (result.errorCode.isNotBlank() && result.errorMsg.isNotBlank()) {
                baseActivity.showTips(result.errorMsg)
            }
        })
        viewModel.articleListResult.observe(viewLifecycleOwner, { result ->
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
        })
    }

}