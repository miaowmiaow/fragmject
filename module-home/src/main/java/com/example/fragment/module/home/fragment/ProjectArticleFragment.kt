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
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.home.databinding.FragmentProjectArticleBinding
import com.example.fragment.module.home.model.ProjectViewModel

class ProjectArticleFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(cid: String): ProjectArticleFragment {
            val fragment = ProjectArticleFragment()
            val args = Bundle()
            args.putString(Keys.CID, cid)
            fragment.arguments = args
            return fragment
        }
    }

    private var cid = ""
    private val articleAdapter = ArticleAdapter()
    private val viewModel: ProjectViewModel by viewModels()
    private var _binding: FragmentProjectArticleBinding? = null
    private val binding get() = _binding!!

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.apply {
            cid = this.getString(Keys.CID).toString()
        }
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
                viewModel.getProjectList(true, cid)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object :
            PullRefreshLayout.OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getProjectList(false, cid)
            }
        })
        binding.pullRefresh.setRefreshing()
    }

    private fun update() {
        viewModel.projectListResult.observe(viewLifecycleOwner, { result ->
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