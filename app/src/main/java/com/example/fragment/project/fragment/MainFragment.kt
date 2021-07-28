package com.example.fragment.project.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.fragment.library.base.component.activity.OnBackPressedListener
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.library.base.utils.SimpleBannerHelper
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.ViewBindingFragment
import com.example.fragment.library.common.utils.TestAnnotation
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.home.fragment.SquareFragment
import com.example.fragment.project.adapter.HotKeyAdapter
import com.example.fragment.project.databinding.FragmentMainBinding
import com.example.fragment.project.model.MainViewModel

class MainFragment : ViewBindingFragment<FragmentMainBinding>(), OnBackPressedListener {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var bannerHelper: SimpleBannerHelper
    private val hotKeyAdapter = HotKeyAdapter()

    private val fragments = arrayListOf(
        SquareFragment.newInstance(),
        WanFragment.newInstance()
    )

    override fun setViewBinding(): (LayoutInflater, ViewGroup?, Boolean) -> FragmentMainBinding {
        return FragmentMainBinding::inflate
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(savedInstanceState)
        update()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("TAB_CURRENT_POSITION", binding.viewpager.currentItem)
    }

    override fun onResume() {
        super.onResume()
        baseActivity.registerOnBackPressedListener(this::class.java.simpleName, this)
        bannerHelper.startTimerTask()
    }

    override fun onPause() {
        super.onPause()
        baseActivity.removerOnBackPressedListener(this::class.java.simpleName)
        bannerHelper.stopTimerTask()
    }

    override fun onBackPressed(): Boolean {
        return if (binding.viewpager.currentItem == 1) {
            false
        } else {
            binding.viewpager.currentItem = 1
            true
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onUserStatusUpdate(userBean: UserBean) {
        if (userBean.id.isNotEmpty()) {
            binding.logo.setOnClickListener(null)
            binding.username.setOnClickListener(null)
            binding.username.text = "欢迎回来！${userBean.username}"
        } else {
            binding.logo.setOnClickListener { baseActivity.navigation(Router.LOGIN) }
            binding.username.setOnClickListener { baseActivity.navigation(Router.LOGIN) }
            binding.username.text = "去登录"
        }
    }

    @TestAnnotation(code = 10086, message = "MainFragment.setupView")
    private fun setupView(savedInstanceState: Bundle?) {
        binding.menu.setOnClickListener {
            if (binding.drawer.isDrawerOpen(GravityCompat.START)) {
                binding.drawer.closeDrawer(GravityCompat.START)
            } else {
                binding.drawer.openDrawer(GravityCompat.START)
            }
        }
        binding.add.setOnClickListener { baseActivity.navigation(Router.SHARE_ARTICLE) }
        binding.logo.setOnClickListener { baseActivity.navigation(Router.LOGIN) }
        binding.username.setOnClickListener { baseActivity.navigation(Router.LOGIN) }
        binding.coin.setOnClickListener { baseActivity.navigation(Router.MY_COIN) }
        binding.myCollection.setOnClickListener { baseActivity.navigation(Router.MY_COLLECT_ARTICLE) }
        binding.myShare.setOnClickListener { baseActivity.navigation(Router.MY_SHARE_ARTICLE) }
        binding.setting.setOnClickListener { baseActivity.navigation(Router.SETTING) }
        binding.search.setOnClickListener { search() }
        hotKeyAdapter.setOnItemClickListener(object : BaseAdapter.OnItemClickListener {
            override fun onItemClick(holder: BaseAdapter.ViewBindHolder, position: Int) {
                search()
            }
        })
        bannerHelper = SimpleBannerHelper(binding.hotKey, RecyclerView.VERTICAL)
        binding.hotKey.adapter = hotKeyAdapter
        binding.viewpager.offscreenPageLimit = 1
        binding.viewpager.adapter = object :
            FragmentPagerAdapter(childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

            override fun getItem(position: Int): Fragment {
                return fragments[position]
            }

            override fun getCount(): Int {
                return fragments.size
            }
        }
        binding.viewpager.currentItem = savedInstanceState?.getInt("TAB_CURRENT_POSITION") ?: 1
    }

    private fun update() {
        viewModel.hotKeyResult.observe(viewLifecycleOwner, { result ->
            result.data?.apply {
                if (result.errorCode == "0") {
                    hotKeyAdapter.setNewData(this)
                    WanHelper.setHotKey(this)
                    bannerHelper.startTimerTask()
                }
                if (result.errorCode.isNotBlank() && result.errorMsg.isNotBlank()) {
                    baseActivity.showTips(result.errorMsg)
                }
            }
        })
        viewModel.getHotKey()
        viewModel.getTree()
    }

    private fun search() {
        val title = hotKeyAdapter.getItem(bannerHelper.findLastVisibleItemPosition()).name
        val args = Bundle()
        args.putString(Keys.TITLE, title)
        baseActivity.navigation(Router.SEARCH, args)
    }

}