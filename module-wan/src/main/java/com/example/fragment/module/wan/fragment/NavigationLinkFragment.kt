package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.R
import com.example.fragment.module.wan.adapter.LinkMenuAdapter
import com.example.fragment.module.wan.databinding.FragmentLinkBinding
import com.example.fragment.module.wan.model.LinkViewModel

class LinkFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): LinkFragment {
            return LinkFragment()
        }
    }

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

    private val viewModel: LinkViewModel by viewModels()
    private var _binding: FragmentLinkBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLinkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        linkMenuAdapter.setOnItemSelectedListener(linkMenuSelectedListener)
        binding.menu.layoutManager = LinearLayoutManager(binding.menu.context)
        binding.menu.adapter = linkMenuAdapter
    }

    override fun initViewModel() {
        viewModel.navigationResult.observe(viewLifecycleOwner) { result ->
            when {
                result.errorCode == "0" -> {
                    result.data?.let { menu ->
                        if (menu.isNotEmpty()) {
                            menu[0].isSelected = true
                            linkMenuAdapter.selectItem(0)
                            linkMenuAdapter.setNewData(menu)
                            fillFlexboxLayout(menu[0].articles)
                        }
                    }
                }
                result.errorCode.isNotBlank() && result.errorMsg.isNotBlank() -> {
                    activity.showTips(result.errorMsg)
                }
            }
        }
    }

    override fun onLoad() {
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
                val args = Bundle()
                args.putString(Keys.URL, article.link)
                activity.navigation(Router.WEB, args)
            }
            binding.fbl.addView(tv)
        }
    }

}