package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SystemArticleFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStop() {
        super.onStop()
        //将数据缓存在 ViewModel 中来提升用户体验
        viewModel.listDataMap[cid] = articleAdapter.getData() as List<ArticleBean>
        viewModel.listScrollMap[cid] = binding.list.computeVerticalScrollOffset()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearSystemArticleResult(cid)
        binding.pullRefresh.recycler()
        binding.list.adapter = null
        _binding = null
    }

    override fun initView() {
        cid = requireArguments().getString(Keys.CID, "0")
        //体系列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
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
    }

    override fun initViewModel(): BaseViewModel {
        if (viewModel.listDataMap.containsKey(cid)) {
            articleAdapter.setNewData(viewModel.listDataMap[cid])
            binding.list.scrollTo(0, viewModel.listScrollMap[cid] ?: 0)
        }
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