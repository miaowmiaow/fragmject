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
    private var _articleAdapter: ArticleAdapter? = null
    private val articleAdapter get() = _articleAdapter!!
    private var cid = ""
    private var listOffsetY = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SystemArticleFragmentBinding.inflate(inflater, container, false)
        _articleAdapter = ArticleAdapter()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.systemArticleResultMap[cid] = articleAdapter.getData() as List<ArticleBean>
        viewModel.systemArticleScrollMap[cid] = listOffsetY
        _articleAdapter = null
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
                viewModel.getSystemArticleHome(cid)
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getSystemArticleNext(cid)
            }
        })
        if (viewModel.systemArticleResultMap.containsKey(cid)) {
            articleAdapter.setNewData(viewModel.systemArticleResultMap[cid])
        }
        if (viewModel.systemArticleScrollMap.containsKey(cid)) {
            binding.list.scrollTo(0, viewModel.systemArticleScrollMap[cid] ?: 0)
        }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.systemArticleResult(cid).observe(viewLifecycleOwner) { result ->
            if (result.containsKey(cid)) {
                httpParseSuccess(result[cid] as ArticleListBean) {
                    if (viewModel.isHomePage(cid)) {
                        articleAdapter.setNewData(it.data?.datas)
                    } else {
                        articleAdapter.addData(it.data?.datas)
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

}