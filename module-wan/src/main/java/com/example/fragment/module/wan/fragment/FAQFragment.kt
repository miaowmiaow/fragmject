package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.view.OnLoadMoreListener
import com.example.fragment.library.base.view.OnRefreshListener
import com.example.fragment.library.base.view.PullRefreshLayout
import com.example.fragment.library.common.adapter.ArticleAdapter
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.R
import com.example.fragment.module.wan.databinding.FragmentFaqBinding
import com.example.fragment.module.wan.model.FAQViewModel

class FAQFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): FAQFragment {
            return FAQFragment()
        }
    }

    private val articleAdapter = ArticleAdapter()
    private val articleChildClickListener = object : BaseAdapter.OnItemChildClickListener {
        override fun onItemChildClick(
            view: View,
            holder: BaseAdapter.ViewBindHolder,
            position: Int
        ) {
            val item = articleAdapter.getItem(position)
            when (view.id) {
                R.id.rl_item -> {
                    val args = Bundle()
                    args.putString(Keys.URL, item.link)
                    activity.navigation(Router.WEB, args)
                }
                R.id.tv_author -> {
                    val args = Bundle()
                    args.putString(Keys.UID, item.userId)
                    activity.navigation(Router.USER_SHARE, args)
                }
                R.id.tv_tag -> {
                    item.tags?.let {
                        articleAdapter.urlToSystemList(activity, it[0].url) { treeBean ->
                            val args = Bundle()
                            args.putParcelable(Keys.BEAN, treeBean)
                            activity.navigation(Router.SYSTEM, args)
                        }
                    }
                }
                R.id.tv_chapter_name -> {
                    articleAdapter.chapterIdToSystemList(activity, item) { treeBean ->
                        val args = Bundle()
                        args.putParcelable(Keys.BEAN, treeBean)
                        activity.navigation(Router.SYSTEM, args)
                    }
                }
            }
        }
    }

    private val viewModel: FAQViewModel by viewModels()
    private var _binding: FragmentFaqBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFaqBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        articleAdapter.setOnItemChildClickListener(articleChildClickListener)
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = articleAdapter
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getUserArticleList(true)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getUserArticleList(false)
            }
        })
    }

    override fun initViewModel() {
        viewModel.wendaResult.observe(viewLifecycleOwner) { result ->
            when {
                result.errorCode == "0" -> {
                    result.data?.datas?.let { list ->
                        if (viewModel.isRefresh) {
                            articleAdapter.setNewData(list)
                        } else {
                            articleAdapter.addData(list)
                        }
                    }
                }
                result.errorCode.isNotBlank() && result.errorMsg.isNotBlank() -> {
                    activity.showTips(result.errorMsg)
                }
            }
            binding.pullRefresh.finishRefresh()
            binding.pullRefresh.setLoadMore(viewModel.page < viewModel.pageCont)
        }
    }

    override fun onLoad() {
        if(viewModel.wendaResult.value == null){
            viewModel.getUserArticleList(true)
        }
    }

}