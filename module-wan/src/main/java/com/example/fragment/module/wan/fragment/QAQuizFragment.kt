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
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.databinding.QaQuizFragmentBinding
import com.example.fragment.module.wan.model.QAModel

class QAQuizFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): QAQuizFragment {
            return QAQuizFragment()
        }
    }

    private val viewModel: QAModel by viewModels()
    private var _binding: QaQuizFragmentBinding? = null
    private val binding get() = _binding!!

    private val articleAdapter = ArticleAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = QaQuizFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        //问答列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getWenDa()
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getWenDaNext()
            }
        })
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.wendaResult.observe(viewLifecycleOwner) {
            httpParseSuccess(it) { result ->
                if (viewModel.isHomePage()) {
                    articleAdapter.setNewData(result.data?.datas)
                } else {
                    articleAdapter.addData(result.data?.datas)
                }
            }
            //结束下拉刷新状态
            binding.pullRefresh.finishRefresh()
            //设置加载更多状态
            binding.pullRefresh.setLoadMore(viewModel.hasNextPage())
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.wendaResult.value == null) {
            binding.pullRefresh.setRefreshing()
        }
    }

}