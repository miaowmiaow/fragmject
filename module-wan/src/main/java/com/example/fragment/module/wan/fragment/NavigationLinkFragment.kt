package com.example.fragment.module.wan.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.R
import com.example.fragment.module.wan.adapter.LinkMenuAdapter
import com.example.fragment.module.wan.databinding.FragmentNavigationLinkBinding
import com.example.fragment.module.wan.model.NavigationViewModel

class NavigationLinkFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): NavigationLinkFragment {
            return NavigationLinkFragment()
        }
    }

    private val viewModel: NavigationViewModel by activityViewModels()
    private var _binding: FragmentNavigationLinkBinding? = null
    private val binding get() = _binding!!

    private val linkMenuAdapter = LinkMenuAdapter()
    private val linkMenuSelectedListener = object : BaseAdapter.OnItemSelectedListener {
        override fun onItemSelected(holder: BaseAdapter.ViewBindHolder, position: Int) {
            linkMenuAdapter.getItem(position).let { item ->
                item.isSelected = true
                linkMenuAdapter.notifyItemRangeChanged(position, 1)
                fillFlexboxLayout(item.articles)
            }
        }

        override fun onItemUnselected(holder: BaseAdapter.ViewBindHolder, position: Int) {
            linkMenuAdapter.getItem(position).let { item ->
                item.isSelected = false
                linkMenuAdapter.notifyItemRangeChanged(position, 1)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNavigationLinkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        //导航列表
        binding.menu.layoutManager = LinearLayoutManager(binding.menu.context)
        binding.menu.adapter = linkMenuAdapter
        linkMenuAdapter.setOnItemSelectedListener(linkMenuSelectedListener)
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.navigationResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> result.data?.let { data ->
                    var selectItem = 0
                    data.forEachIndexed { index, bean ->
                        if (bean.isSelected) {
                            selectItem = index
                        }
                    }
                    data[selectItem].isSelected = true
                    linkMenuAdapter.setNewData(data)
                    linkMenuAdapter.selectItem(selectItem)
                    fillFlexboxLayout(data[selectItem].articles)
                }
                else -> activity.showTips(result.errorMsg)
            }
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.navigationResult.value == null) {
            viewModel.getNavigation()
        }
    }

    private fun fillFlexboxLayout(data: List<ArticleBean>? = null) {
        binding.fbl.removeAllViews()
        data?.forEach { article ->
            val inflater = LayoutInflater.from(binding.fbl.context)
            val tv = inflater.inflate(R.layout.fbl_link, binding.fbl, false) as TextView
            tv.text = article.title
            tv.setOnClickListener {
                val url = Uri.encode(article.link)
                activity.navigation(Router.WEB, bundleOf(Keys.URL to url))
            }
            binding.fbl.addView(tv)
        }
    }

}