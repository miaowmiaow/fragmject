package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.view.pull.OnLoadMoreListener
import com.example.fragment.library.base.view.pull.OnRefreshListener
import com.example.fragment.library.base.view.pull.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.databinding.QaQuizFragmentBinding
import com.example.fragment.module.wan.model.QAQuizModel

class QAQuizFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): QAQuizFragment {
            return QAQuizFragment()
        }
    }

    private val viewModel: QAQuizModel by viewModels()
    private var _binding: QaQuizFragmentBinding? = null
    private val binding get() = _binding!!
    private var _articleAdapter: ArticleAdapter? = null
    private val articleAdapter get() = _articleAdapter!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = QaQuizFragmentBinding.inflate(inflater, container, false)
        _articleAdapter = ArticleAdapter()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.listData = articleAdapter.getData()
        _articleAdapter = null
        _binding = null
    }

    override fun initView() {
        //问答列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                viewModel.listScroll += dy
            }
        })
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getWenDaHome()
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getWenDaNext()
            }
        })
        //将数据从 ViewModel 取出渲染
        if (viewModel.listData.isNotEmpty()) {
            articleAdapter.setNewData(viewModel.listData)
        }
        if (viewModel.listScroll > 0) {
            binding.list.scrollTo(0, viewModel.listScroll)
        }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.wendaResult().observe(viewLifecycleOwner) { result ->
            httpParseSuccess(result) {
                if (viewModel.isHomePage()) {
                    articleAdapter.setNewData(it.data?.datas)
                } else {
                    articleAdapter.addData(it.data?.datas)
                }
            }
            //结束下拉刷新状态
            binding.pullRefresh.finishRefresh()
            //设置加载更多状态
            binding.pullRefresh.setLoadMore(viewModel.hasNextPage())
        }
        return viewModel
    }

}