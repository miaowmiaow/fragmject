package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.adapter.SystemAdapter
import com.example.fragment.module.wan.databinding.NavigationSystemFragmentBinding
import com.example.fragment.module.wan.model.NavigationViewModel

class NavigationSystemFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): NavigationSystemFragment {
            return NavigationSystemFragment()
        }
    }

    private val viewModel: NavigationViewModel by activityViewModels()
    private var _binding: NavigationSystemFragmentBinding? = null
    private val binding get() = _binding!!

    private val systemAdapter = SystemAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NavigationSystemFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        //体系列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = systemAdapter
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.systemTreeResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> systemAdapter.setNewData(result.data)
                else -> activity.showTips(result.errorMsg)
            }
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.systemTreeResult.value == null) {
            viewModel.getSystemTree()
        }
    }

}