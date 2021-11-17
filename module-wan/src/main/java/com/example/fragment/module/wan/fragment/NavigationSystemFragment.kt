package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.adapter.SystemAdapter
import com.example.fragment.module.wan.databinding.FragmentNavigationSystemBinding
import com.example.fragment.module.wan.model.SystemViewModel

class NavigationSystemFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): NavigationSystemFragment {
            return NavigationSystemFragment()
        }
    }

    private val systemAdapter = SystemAdapter()

    private val viewModel: SystemViewModel by viewModels()
    private var _binding: FragmentNavigationSystemBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNavigationSystemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = systemAdapter
    }

    override fun initViewModel() {
        viewModel.treeResult.observe(viewLifecycleOwner) { result ->
            when {
                result.errorCode == "0" -> {
                    result.data?.apply {
                        systemAdapter.setNewData(this)
                    }
                }
                result.errorCode.isNotBlank() -> {
                    activity.showTips(result.errorMsg)
                }
            }
        }
    }

    override fun onLoad() {
        if(viewModel.treeResult.value == null){
            viewModel.getTree()
        }
    }

}