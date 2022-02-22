package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.view.pull.OnLoadMoreListener
import com.example.fragment.library.base.view.pull.OnRefreshListener
import com.example.fragment.library.base.view.pull.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.databinding.SystemArticleFragmentBinding
import com.example.fragment.module.wan.model.SystemViewModel

class SystemArticleFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): SystemArticleFragment {
            return SystemArticleFragment()
        }
    }

    private val viewModel: SystemViewModel by activityViewModels()
    private var _binding: SystemArticleFragmentBinding? = null
    private val binding get() = _binding!!

    private val articleAdapter = ArticleAdapter()
    private var cid = ""
    private var listOffsetY = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SystemArticleFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.listMap[cid] = articleAdapter.getData() as List<ArticleBean>
        viewModel.listScrollMap[cid] = listOffsetY
        _binding = null
    }

    override fun initView() {
        cid = requireArguments().getString(Keys.CID, "0")
        //体系列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                listOffsetY += dy
            }
        })
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getSystemArticle(cid)
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getSystemArticleNext(cid)
            }
        })
        if (viewModel.listMap.containsKey(cid)) {
            articleAdapter.setNewData(viewModel.listMap[cid])
        }
        if (viewModel.listScrollMap.containsKey(cid)) {
            binding.list.scrollTo(0, viewModel.listScrollMap[cid] ?: 0)
        }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.listResult.observe(viewLifecycleOwner) {
            if (it.containsKey(cid)) {
                httpParseSuccess(it[cid] as ArticleListBean) { result ->
                    if (viewModel.isHomePage(cid)) {
                        articleAdapter.setNewData(result.data?.datas)
                    } else {
                        articleAdapter.addData(result.data?.datas)
                    }
                }
            }
            //结束下拉刷新状态
            binding.pullRefresh.finishRefresh()
            //设置加载更多状态
            binding.pullRefresh.setLoadMore(viewModel.hasNextPage(cid))
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.listMap[cid].isNullOrEmpty()) {
            binding.pullRefresh.setRefreshing()
        }
    }

}