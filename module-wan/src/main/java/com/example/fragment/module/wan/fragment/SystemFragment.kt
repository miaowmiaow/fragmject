package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.fragment.library.base.adapter.SimplePagerAdapter
import com.example.fragment.library.common.bean.TreeBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.R
import com.example.fragment.module.wan.databinding.FragmentSystemBinding

class SystemFragment : RouterFragment() {

    private var tree: TreeBean? = null

    private var _binding: FragmentSystemBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSystemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        binding.black.setOnClickListener {
            activity.onBackPressed()
        }
    }

    override fun initViewModel() {
        arguments?.apply {
            tree = this.getParcelable(Keys.BEAN)
        }
        tree?.let { tree ->
            binding.title.text = tree.name
            tree.children?.let { data ->
                binding.viewpager.offscreenPageLimit = 1
                binding.viewpager.adapter = object : SimplePagerAdapter(childFragmentManager) {

                    override fun getCount(): Int {
                        return data.size
                    }

                    override fun getItem(position: Int): Fragment {
                        val args = Bundle()
                        args.putString(Keys.CID, data[position].id)
                        val fragment = SystemArticleFragment.newInstance()
                        fragment.arguments = args
                        return fragment
                    }
                }
                binding.tabBar.setupWithViewPager(binding.viewpager)
                binding.tabBar.removeAllTabs()
                data.forEach {
                    val layoutInflater = LayoutInflater.from(binding.root.context)
                    val tabView: View = layoutInflater.inflate(R.layout.tab_item_top, null)
                    tabView.findViewById<TextView>(R.id.tv_tab).text = it.name
                    val tab = binding.tabBar.newTab()
                    tab.customView = tabView
                    binding.tabBar.addTab(tab)
                }
                binding.viewpager.currentItem = tree.childrenSelectPosition
            }
        }
    }

    override fun onLoad() {
    }

}