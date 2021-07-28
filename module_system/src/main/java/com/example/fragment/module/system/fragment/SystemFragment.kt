package com.example.fragment.module.system.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.component.view.SimplePullRefreshLayout
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.fragment.ViewBindingFragment
import com.example.fragment.module.system.adapter.SystemAdapter
import com.example.fragment.module.system.databinding.FragmentSystemBinding
import com.example.fragment.module.system.model.SystemViewModel

class SystemFragment : ViewBindingFragment<FragmentSystemBinding>() {

    companion object {
        @JvmStatic
        fun newInstance(): SystemFragment {
            return SystemFragment()
        }
    }

    private val viewModel: SystemViewModel by viewModels()
    private val systemAdapter = SystemAdapter()

    override fun setViewBinding(): (LayoutInflater, ViewGroup?, Boolean) -> FragmentSystemBinding {
        return FragmentSystemBinding::inflate
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
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = systemAdapter
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
            if (result.errorCode == "0") {
                result.data?.apply {
                    systemAdapter.setNewData(this)
                }
            } else if (result.errorCode.isNotBlank()) {
                baseActivity.showTips(result.errorMsg)
            }
            if (binding.pullRefresh.isRefresh()) {
                binding.pullRefresh.finishRefresh()
            }
        })
    }

}