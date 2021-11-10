package com.example.fragment.module.home.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.fragment.library.base.adapter.SimplePagerAdapter
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.home.R
import com.example.fragment.module.home.databinding.FragmentProjectListBinding
import com.example.fragment.module.home.model.ProjectViewModel

class ProjectListFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): ProjectListFragment {
            return ProjectListFragment()
        }
    }

    private val viewModel: ProjectViewModel by viewModels()
    private var _binding: FragmentProjectListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.projectTreeResult.observe(viewLifecycleOwner) { result ->
            if (result.errorCode == "0") {
                result.data?.also { data ->
                    binding.viewpager.offscreenPageLimit = 2
                    binding.viewpager.adapter =
                        object : SimplePagerAdapter(childFragmentManager) {
                            override fun getCount(): Int {
                                return data.size
                            }

                            override fun getItem(position: Int): Fragment {
                                val fragment = ProjectArticleFragment.newInstance()
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
                        savedInstanceState?.getInt("PROJECT_CURRENT_POSITION") ?: 0
                }
            }
            if (result.errorCode.isNotBlank() && result.errorMsg.isNotBlank()) {
                baseActivity.showTips(result.errorMsg)
            }
            dismissDialog()
        }
        viewModel.getProjectTree()
        showDialog()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("PROJECT_CURRENT_POSITION", binding.viewpager.currentItem)
    }

    override fun onUserStatusUpdate(userBean: UserBean) {
        viewModel.getProjectTree()
        showDialog()
    }

}