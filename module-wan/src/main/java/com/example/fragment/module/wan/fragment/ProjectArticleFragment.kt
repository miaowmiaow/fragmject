package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.toppingToPosition
import com.example.fragment.library.base.view.pull.OnLoadMoreListener
import com.example.fragment.library.base.view.pull.OnRefreshListener
import com.example.fragment.library.base.view.pull.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.model.TabEventViewMode
import com.example.fragment.module.wan.databinding.ProjectArticleFragmentBinding
import com.example.fragment.module.wan.model.ProjectViewModel

class ProjectArticleFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): ProjectArticleFragment {
            return ProjectArticleFragment()
        }
    }

    private val projectViewModel: ProjectViewModel by activityViewModels()
    private var _binding: ProjectArticleFragmentBinding? = null
    private val binding get() = _binding!!
    private val articleAdapter = ArticleAdapter()
    private var cid = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ProjectArticleFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStop() {
        super.onStop()
        //将数据缓存在 ViewModel 中来提升用户体验
        projectViewModel.listDataMap[cid] = articleAdapter.getData() as List<ArticleBean>
        projectViewModel.listScrollMap[cid] = binding.list.computeVerticalScrollOffset()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        projectViewModel.clearProjectListResult(cid)
        binding.pullRefresh.recycler()
        binding.list.adapter = null
        _binding = null
    }

    override fun initView() {
        cid = requireArguments().getString(Keys.CID, "0")
        //项目列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                projectViewModel.getProjectHome(cid)
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                projectViewModel.getProjectNext(cid)
            }
        })
    }

    override fun initViewModel(): BaseViewModel {
        if (projectViewModel.listDataMap.containsKey(cid)) {
            articleAdapter.setNewData(projectViewModel.listDataMap[cid])
            binding.list.scrollTo(0, projectViewModel.listScrollMap[cid] ?: 0)
        }
        projectViewModel.projectListResult(cid).observe(viewLifecycleOwner) { result ->
            if (result.containsKey(cid)) {
                if (projectViewModel.isHomePage(cid)) {
                    articleAdapter.setNewData(result[cid])
                } else {
                    articleAdapter.addData(result[cid])
                }
            }
            //结束下拉刷新状态
            binding.pullRefresh.finishRefresh()
            //设置加载更多状态
            binding.pullRefresh.setLoadMore(projectViewModel.hasNextPage(cid))
        }
        return projectViewModel
    }

}