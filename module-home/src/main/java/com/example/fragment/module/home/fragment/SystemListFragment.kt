package com.example.fragment.module.home.fragment

import android.annotation.SuppressLint
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
import com.example.fragment.module.home.R
import com.example.fragment.module.home.databinding.FragmentSystemListBinding

class SystemListFragment : RouterFragment() {

    private var tree: TreeBean? = null

    private var _binding: FragmentSystemListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSystemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.apply {
            tree = this.getParcelable(Keys.BEAN)
        }
        binding.black.setOnClickListener {
            baseActivity.onBackPressed()
        }

        tree?.let { tree ->
            binding.title.text = tree.name
            tree.children?.let { data ->
                binding.viewpager.offscreenPageLimit = 2
                binding.viewpager.adapter = object : SimplePagerAdapter(childFragmentManager) {

                    override fun getCount(): Int {
                        return data.size
                    }

                    override fun getItem(position: Int): Fragment {
                        val fragment = SystemArticleFragment.newInstance()
                        val args = Bundle()
                        args.putString(Keys.CID, data[position].id)
                        fragment.arguments = args
                        return fragment
                    }
                }
                binding.tab.setupWithViewPager(binding.viewpager)
                binding.tab.removeAllTabs()
                data.forEach {
                    val layoutInflater = LayoutInflater.from(binding.root.context)
                    val tabView: View = layoutInflater.inflate(R.layout.tab_item_top, null)
                    tabView.findViewById<TextView>(R.id.tv_tab).text = it.name
                    val tab = binding.tab.newTab()
                    tab.customView = tabView
                    binding.tab.addTab(tab)
                }
                binding.viewpager.currentItem =
                    savedInstanceState?.getInt("SYSTEM_CURRENT_POSITION")
                        ?: tree.childrenSelectPosition
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("SYSTEM_CURRENT_POSITION", binding.viewpager.currentItem)
    }

}