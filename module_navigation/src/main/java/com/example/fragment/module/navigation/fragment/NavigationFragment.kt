package com.example.fragment.module.navigation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.view.PullRefreshLayout
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.navigation.R
import com.example.fragment.module.navigation.adapter.NavigationMenuAdapter
import com.example.fragment.module.navigation.databinding.FragmentNavigationBinding
import com.example.fragment.module.navigation.model.NavigationViewModel

class NavigationFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): NavigationFragment {
            return NavigationFragment()
        }
    }

    private val navigationMenuAdapter = NavigationMenuAdapter()
    private val viewModel: NavigationViewModel by viewModels()
    private var _binding: FragmentNavigationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNavigationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
    }

    override fun onUserStatusUpdate(userBean: UserBean) {
        binding.pullRefresh.setRefreshing()
    }

    private fun setupView() {
        binding.menu.layoutManager = LinearLayoutManager(binding.menu.context)
        binding.menu.adapter = navigationMenuAdapter
        navigationMenuAdapter.setOnItemSelectedListener(object :
            BaseAdapter.OnItemSelectedListener {
            override fun onItemSelected(holder: BaseAdapter.ViewBindHolder, position: Int) {
                navigationMenuAdapter.getItem(position).let { item ->
                    item.isSelected = true
                    navigationMenuAdapter.notifyItemRangeChanged(position, 1)
                    fillFlexboxLayout(item.articles)
                }
            }

            override fun onItemUnselected(holder: BaseAdapter.ViewBindHolder, position: Int) {
                navigationMenuAdapter.getItem(position).let { item ->
                    item.isSelected = false
                    navigationMenuAdapter.notifyItemRangeChanged(position, 1)
                }
            }
        })
        binding.pullRefresh.setLoadMore(false)
        binding.pullRefresh.setOnRefreshListener(object :
            PullRefreshLayout.OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getNavigation()
            }
        })
        binding.pullRefresh.setRefreshing()
    }

    private fun update() {
        viewModel.navigationResult.observe(viewLifecycleOwner, { result ->
            if (result.errorCode == "0") {
                result.data?.let { menu ->
                    if (menu.isNotEmpty()) {
                        menu[0].isSelected = true
                        navigationMenuAdapter.selectItem(0)
                        navigationMenuAdapter.setNewData(menu)
                        fillFlexboxLayout(menu[0].articles)
                    }
                }
            }
            if (result.errorCode.isNotBlank() && result.errorMsg.isNotBlank()) {
                baseActivity.showTips(result.errorMsg)
            }
            if (binding.pullRefresh.isRefresh()) {
                binding.pullRefresh.finishRefresh()
            }
        })
    }

    private fun fillFlexboxLayout(data: List<ArticleBean>? = null) {
        binding.fbl.removeAllViews()
        data?.forEach { article ->
            val inflater = LayoutInflater.from(binding.fbl.context)
            val tv = inflater.inflate(R.layout.fbl_navigation, binding.fbl, false) as TextView
            tv.text = article.title
            tv.setOnClickListener {
                val args = Bundle()
                args.putString(Keys.URL, article.link)
                baseActivity.navigation(Router.WEB, args)
            }
            binding.fbl.addView(tv)
        }
    }

}