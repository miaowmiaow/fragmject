package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.toppingToPosition
import com.example.fragment.library.base.view.pull.OnLoadMoreListener
import com.example.fragment.library.base.view.pull.OnRefreshListener
import com.example.fragment.library.base.view.pull.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.model.TabEventViewMode
import com.example.fragment.module.wan.databinding.QaQuizFragmentBinding
import com.example.fragment.module.wan.model.QAQuizModel

class QAQuizFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): QAQuizFragment {
            return QAQuizFragment()
        }
    }

    private val eventViewModel: TabEventViewMode by activityViewModels()
    private val qaQuizViewModel: QAQuizModel by viewModels()
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

    override fun onResume() {
        super.onResume()
        eventViewModel.qaTab().observe(viewLifecycleOwner) {
            if (it == 1) {
                binding.list.toppingToPosition(0)
                eventViewModel.setQATab(0)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        eventViewModel.qaTab().removeObservers(viewLifecycleOwner)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        qaQuizViewModel.clearWendaResult()
        binding.pullRefresh.recycler()
        binding.list.adapter = null
        _binding = null
    }

    override fun initView() {
        //问答列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                qaQuizViewModel.getWenDaHome()
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                qaQuizViewModel.getWenDaNext()
            }
        })
    }

    override fun initViewModel(): BaseViewModel {
        qaQuizViewModel.wendaResult().observe(viewLifecycleOwner) { result ->
            httpParseSuccess(result) {
                if (qaQuizViewModel.isHomePage()) {
                    articleAdapter.setNewData(it.data?.datas)
                } else {
                    articleAdapter.addData(it.data?.datas)
                }
            }
            //结束下拉刷新状态
            binding.pullRefresh.finishRefresh()
            //设置加载更多状态
            binding.pullRefresh.setLoadMore(qaQuizViewModel.hasNextPage())
        }
        return qaQuizViewModel
    }

}