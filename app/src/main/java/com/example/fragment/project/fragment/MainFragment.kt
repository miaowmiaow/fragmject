package com.example.fragment.project.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.BannerHelper
import com.example.fragment.library.common.adapter.HotKeyAdapter
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.fragment.UserFragment
import com.example.fragment.module.wan.fragment.HomeFragment
import com.example.fragment.module.wan.fragment.NavigationFragment
import com.example.fragment.module.wan.fragment.ProjectFragment
import com.example.fragment.module.wan.fragment.QAFragment
import com.example.fragment.module.wan.model.HotKeyViewModel
import com.example.fragment.project.R
import com.example.fragment.project.databinding.MainFragmentBinding
import com.example.fragment.project.databinding.MainTabItemBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainFragment : RouterFragment() {

    private val viewModel: HotKeyViewModel by activityViewModels()
    private var _binding: MainFragmentBinding? = null
    private val binding get() = _binding!!
    private var _hotKeyHelper: BannerHelper? = null
    private val hotKeyHelper get() = _hotKeyHelper!!
    private val hotKeyAdapter = HotKeyAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MainFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.hotKey.adapter = null
        _hotKeyHelper = null
        _binding = null
    }

    override fun initView() {
        binding.hotKey.adapter = hotKeyAdapter
        _hotKeyHelper = BannerHelper(
            binding.hotKey, RecyclerView.VERTICAL, viewLifecycleOwner.lifecycle
        )
        binding.search.setOnClickListener { search() }
        binding.userShare.setOnClickListener { activity.navigation(Router.USER_SHARE) }
        //滚动热词
        hotKeyAdapter.setOnItemClickListener(object : BaseAdapter.OnItemClickListener {
            override fun onItemClick(holder: BaseAdapter.ViewBindHolder, position: Int) {
                search()
            }
        })
        //TabLayout与ViewPager2
        val tabName = arrayOf("首页", "导航", "问答", "项目", "我的")
        val tabIcon = intArrayOf(
            R.drawable.ic_bottom_bar_home,
            R.drawable.ic_bottom_bar_navigation,
            R.drawable.ic_bottom_bar_faq,
            R.drawable.ic_bottom_bar_system,
            R.drawable.ic_bottom_bar_project
        )
        binding.viewpager2.adapter = object : FragmentStateAdapter(
            childFragmentManager,
            viewLifecycleOwner.lifecycle
        ) {
            override fun getItemCount(): Int {
                return tabName.size
            }

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> HomeFragment.newInstance()
                    1 -> NavigationFragment.newInstance()
                    2 -> QAFragment.newInstance()
                    3 -> ProjectFragment.newInstance()
                    4 -> UserFragment.newInstance()
                    else -> throw ArrayIndexOutOfBoundsException("length=${itemCount}; index=$position")
                }
            }
        }
        binding.viewpager2.isUserInputEnabled = false
        binding.viewpager2.offscreenPageLimit = tabName.size
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                setColorFilter(tab.customView, R.color.three_nine_gray)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                setColorFilter(tab.customView, R.color.text_theme)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        TabLayoutMediator(binding.tabLayout, binding.viewpager2, false, false) { tab, position ->
            val item = MainTabItemBinding.inflate(LayoutInflater.from(binding.root.context))
            item.name.text = tabName[position]
            item.name.setTextColor(ContextCompat.getColor(item.name.context, R.color.text_theme))
            item.icon.setImageResource(tabIcon[position])
            item.icon.setColorFilter(ContextCompat.getColor(item.icon.context, R.color.text_theme))
            tab.customView = item.root
        }.attach()
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.hotKeyResult().observe(viewLifecycleOwner) {
            hotKeyAdapter.setNewData(it)
            hotKeyHelper.start()
        }
        return viewModel
    }

    /**
     * 跳转搜索界面
     */
    private fun search() {
        val position = hotKeyHelper.findItemPosition()
        if (position == RecyclerView.NO_POSITION) return
        val title = hotKeyAdapter.getItem(position).name
        activity.navigation(Router.SEARCH, bundleOf(Keys.VALUE to title))
    }

    private fun setColorFilter(view: View?, iconColor: Int, nameColor: Int = R.color.text_theme) {
        view?.apply {
            val icon = findViewById<ImageView>(R.id.icon)
            val name = findViewById<TextView>(R.id.name)
            icon.setColorFilter(ContextCompat.getColor(icon.context, iconColor))
            name.setTextColor(ContextCompat.getColor(name.context, nameColor))
        }
    }
}