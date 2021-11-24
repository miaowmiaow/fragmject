package com.example.fragment.project.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.adapter.FragmentPagerAdapter
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.BannerHelper
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.fragment.UserFragment
import com.example.fragment.module.wan.fragment.HomeFragment
import com.example.fragment.module.wan.fragment.NavigationFragment
import com.example.fragment.module.wan.fragment.ProjectFragment
import com.example.fragment.module.wan.fragment.QAFragment
import com.example.fragment.project.R
import com.example.fragment.project.adapter.HotKeyAdapter
import com.example.fragment.project.databinding.FragmentMainBinding
import com.example.fragment.project.databinding.ItemTabMainBinding
import com.example.fragment.project.model.MainViewModel
import com.google.android.material.tabs.TabLayout

class MainFragment : RouterFragment() {

    private val viewModel: MainViewModel by viewModels()
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val tabTexts = arrayOf("首页", "导航", "问答", "项目", "我的")
    private val tabDrawable = intArrayOf(
        R.drawable.ic_bottom_bar_home,
        R.drawable.ic_bottom_bar_navigation,
        R.drawable.ic_bottom_bar_faq,
        R.drawable.ic_bottom_bar_system,
        R.drawable.ic_bottom_bar_project
    )
    private val fragments = arrayListOf(
        HomeFragment.newInstance(),
        NavigationFragment.newInstance(),
        QAFragment.newInstance(),
        ProjectFragment.newInstance(),
        UserFragment.newInstance()
    )

    private lateinit var bannerHelper: BannerHelper
    private val hotKeyAdapter = HotKeyAdapter()
    private val hotKeyClickListener = object : BaseAdapter.OnItemClickListener {
        override fun onItemClick(holder: BaseAdapter.ViewBindHolder, position: Int) {
            search()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        bannerHelper.startTimerTask()
    }

    override fun onPause() {
        super.onPause()
        bannerHelper.stopTimerTask()
    }

    override fun initView() {
        binding.search.setOnClickListener { search() }
        binding.shareArticle.setOnClickListener { activity.navigation(Router.SHARE_ARTICLE) }
        //滚动热词
        binding.hotKey.adapter = hotKeyAdapter
        hotKeyAdapter.setOnItemClickListener(hotKeyClickListener)
        bannerHelper = BannerHelper(binding.hotKey, RecyclerView.VERTICAL)
        //TabBar与ViewPager
        binding.viewpager.offscreenPageLimit = 1
        binding.viewpager.adapter = FragmentPagerAdapter(childFragmentManager, fragments)
        binding.tabBar.setupWithViewPager(binding.viewpager)
        binding.tabBar.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                setColorFilter(tab.customView, R.color.text_fff)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                setColorFilter(tab.customView, R.color.gray_alpha)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })
        var currentItem = binding.tabBar.selectedTabPosition
        if (currentItem == -1) currentItem = 0
        binding.tabBar.removeAllTabs()
        for (i in fragments.indices) {
            val item = ItemTabMainBinding.inflate(LayoutInflater.from(binding.root.context))
            item.icon.setImageResource(tabDrawable[i])
            item.icon.setColorFilter(ContextCompat.getColor(item.icon.context, R.color.gray_alpha))
            item.name.text = tabTexts[i]
            item.name.setTextColor(ContextCompat.getColor(item.name.context, R.color.gray_alpha))
            binding.tabBar.addTab(binding.tabBar.newTab().setCustomView(item.root))
        }
        binding.viewpager.currentItem = currentItem
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.hotKeyResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> {
                    hotKeyAdapter.setNewData(result.data)
                    bannerHelper.startTimerTask()
                    WanHelper.setHotKey(result.data)
                }
                else -> activity.showTips(result.errorMsg)
            }
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.hotKeyResult.value == null) {
            viewModel.getHotKey()
        }
    }

    private fun search() {
        val position = bannerHelper.findItemPosition()
        val title = hotKeyAdapter.getItem(position).name
        val args = bundleOf(Keys.VALUE to title)
        activity.navigation(Router.SEARCH, args)
    }

    private fun setColorFilter(view: View?, id: Int){
        view?.apply {
            val icon = findViewById<ImageView>(R.id.icon)
            val name = findViewById<TextView>(R.id.name)
            icon.setColorFilter(ContextCompat.getColor(icon.context, id))
            name.setTextColor(ContextCompat.getColor(name.context, id))
        }
    }
}