package com.example.fragment.module.navigation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.library.base.component.view.SimplePullRefreshLayout
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.navigation.R
import com.example.fragment.module.navigation.adapter.NavigationMenuAdapter
import com.example.fragment.module.navigation.databinding.FragmentNavigationBinding
import com.example.fragment.module.navigation.model.NavigationViewModel

class NavigationFragment : ViewModelFragment<FragmentNavigationBinding, NavigationViewModel>() {

    companion object {
        @JvmStatic
        fun newInstance(): NavigationFragment {
            return NavigationFragment()
        }
    }

    private val navigationMenuAdapter = NavigationMenuAdapter()

    override fun setViewBinding(inflater: LayoutInflater): FragmentNavigationBinding {
        return FragmentNavigationBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
    }

    private fun setupView() {
        binding.menu.layoutManager = LinearLayoutManager(binding.menu.context)
        binding.menu.adapter = navigationMenuAdapter
        navigationMenuAdapter.setOnItemSelectedListener(object :
            BaseAdapter.OnItemSelectedListener {
            override fun onItemSelected(holder: BaseAdapter.ViewBindHolder, position: Int) {
                navigationMenuAdapter.getItem(position)?.let { item ->
                    item.isSelected = true
                    navigationMenuAdapter.notifyItemRangeChanged(position, 1)
                    fillFlexboxLayout(item.articles)
                }
            }

            override fun onItemUnselected(holder: BaseAdapter.ViewBindHolder, position: Int) {
                navigationMenuAdapter.getItem(position)?.let { item ->
                    item.isSelected = false
                    navigationMenuAdapter.notifyItemRangeChanged(position, 1)
                }
            }
        })
        binding.pullRefresh.setLoadMore(false)
        binding.pullRefresh.setOnRefreshListener(object :
            SimplePullRefreshLayout.OnRefreshListener {
            override fun onRefresh(refreshLayout: SimplePullRefreshLayout) {
                viewModel.getNavigation()
            }
        })
        binding.pullRefresh.setRefreshing()
    }

    private fun update() {
        viewModel.navigationResult.observe(viewLifecycleOwner, { result ->
            result.data?.let { menu ->
                if (menu.isNotEmpty()) {
                    menu[0].isSelected = true
                    navigationMenuAdapter.selectItem(0)
                    navigationMenuAdapter.setNewData(menu)
                    fillFlexboxLayout(menu[0].articles)
                }
            }
            if (binding.pullRefresh.isRefresh()) {
                binding.pullRefresh.finishRefresh()
            }
        })
    }

    private fun fillFlexboxLayout(data: List<ArticleBean>? = null) {
        binding.fbl.removeAllViews()
        data?.forEach {
            val inflater = LayoutInflater.from(binding.fbl.context)
            val tv: TextView =
                inflater.inflate(R.layout.item_navigation, binding.fbl, false) as TextView
            tv.text = it.title
            tv.setOnClickListener {

            }
            binding.fbl.addView(tv)
        }
    }

}