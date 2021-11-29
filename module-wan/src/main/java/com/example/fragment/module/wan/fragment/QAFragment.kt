package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.databinding.FragmentQaBinding
import com.google.android.material.tabs.TabLayoutMediator

class QAFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): QAFragment {
            return QAFragment()
        }
    }

    private var _binding: FragmentQaBinding? = null
    private val binding get() = _binding!!

    private val tabTexts = arrayOf("问答", "广场")
    private val fragments = arrayListOf(
        QAQuizFragment.newInstance(),
        QASquareFragment.newInstance()
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        //TabLayout与ViewPager2
        binding.viewpager2.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return fragments.size
            }

            override fun createFragment(position: Int): Fragment {
                return fragments[position]
            }
        }
        TabLayoutMediator(binding.tabLayout, binding.viewpager2) { tab, position ->
            tab.text = tabTexts[position]
        }.attach()
    }

    override fun initViewModel(): BaseViewModel? {
        return null
    }

    override fun initLoad() {}

}