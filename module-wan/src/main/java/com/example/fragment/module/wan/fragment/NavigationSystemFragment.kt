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
import com.example.fragment.module.wan.model.SystemTreeViewModel

class NavigationSystemFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): NavigationSystemFragment {
            return NavigationSystemFragment()
        }
    }

    private val systemTreeViewModel: SystemTreeViewModel by activityViewModels()
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
        binding.list.adapter = null
        _binding = null
    }

    override fun initView() {
        //体系列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = systemAdapter
    }

    override fun initViewModel(): BaseViewModel {
        systemTreeViewModel.systemTreeResult().observe(viewLifecycleOwner) {
            systemAdapter.setNewData(it)
        }
        return systemTreeViewModel
    }

}