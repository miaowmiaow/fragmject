package com.example.fragment.module.system.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.library.base.component.view.SimplePullRefreshLayout
import com.example.fragment.library.common.constant.Argument
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.system.adapter.SystemAdapter
import com.example.fragment.module.system.databinding.FragmentSystemBinding
import com.example.fragment.module.system.model.SystemViewModel

class SystemFragment : ViewModelFragment<FragmentSystemBinding, SystemViewModel>() {

    companion object {
        @JvmStatic
        fun newInstance(): SystemFragment {
            return SystemFragment()
        }
    }

    private val systemAdapter = SystemAdapter()

    override fun setViewBinding(inflater: LayoutInflater): FragmentSystemBinding {
        return FragmentSystemBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
    }

    private fun setupView() {
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = systemAdapter
        systemAdapter.setOnItemClickListener(object : BaseAdapter.OnItemClickListener {
            override fun onItemClick(holder: BaseAdapter.ViewBindHolder, position: Int) {
                systemAdapter.getItem(position)?.let { tree ->
                    val args = Bundle()
                    args.putParcelable(Argument.BEAN, tree)
                    baseActivity.navigation(Router.SYSTEM, args)
                }
            }
        })
        binding.pullRefresh.setLoadMore(false)
        binding.pullRefresh.setOnRefreshListener(object :
            SimplePullRefreshLayout.OnRefreshListener {
            override fun onRefresh(refreshLayout: SimplePullRefreshLayout) {
                viewModel.getTree()
            }
        })
        binding.pullRefresh.setRefreshing()
    }

    private fun update() {
        viewModel.treeResult.observe(viewLifecycleOwner, { result ->
            result.data?.apply {
                systemAdapter.setNewData(this)
            }
            if (binding.pullRefresh.isRefresh()) {
                binding.pullRefresh.finishRefresh()
            }
        })
    }

}