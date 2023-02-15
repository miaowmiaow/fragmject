package com.example.fragment.module.user.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.library.base.view.pull.OnLoadMoreListener
import com.example.fragment.library.base.view.pull.OnRefreshListener
import com.example.fragment.library.base.view.pull.PullRefreshLayout
import com.example.fragment.library.common.bean.CoinBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.dialog.StandardDialog
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.adapter.CoinRankAdapter
import com.example.fragment.module.user.databinding.CoinRankFragmentBinding
import com.example.fragment.module.user.vm.CoinRankViewModel

class CoinRankFragment : RouterFragment() {

    private val viewModel: CoinRankViewModel by viewModels()
    private var _binding: CoinRankFragmentBinding? = null
    private val binding get() = _binding!!
    private val coinRankAdapter = CoinRankAdapter()
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            backPressedDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CoinRankFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedCallback
        )
    }

    private fun backPressedDialog() {
        StandardDialog.newInstance()
            .setContent("直接回到首页吗？")
            .setOnDialogClickListener(object : StandardDialog.OnDialogClickListener {
                override fun onConfirm(dialog: StandardDialog) {
                    backPressedCallback.isEnabled = true
                    navigation(Router.MAIN)
                }

                override fun onCancel(dialog: StandardDialog) {
                    backPressedCallback.isEnabled = false
                    onBackPressed()
                }
            })
            .show(childFragmentManager)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearCoinRankResult()
        binding.pullRefresh.recycler()
        binding.list.adapter = null
        _binding = null
    }

    override fun initView() {
        binding.black.setOnClickListener {
            onBackPressed()
        }
        binding.rule.setOnClickListener {
            val url = Uri.encode("https://www.wanandroid.com/blog/show/2653")
            navigation(Router.WEB, bundleOf(Keys.URL to url))
        }
        //积分排行榜列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = coinRankAdapter
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getCoinRankHome()
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getCoinRankNext()
            }
        })
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.coinRankResult().observe(viewLifecycleOwner) { result ->
            httpParseSuccess(result) {
                it.data?.datas?.apply {
                    updateView(this)
                }
            }
            //结束下拉刷新状态
            binding.pullRefresh.finishRefresh()
            //设置加载更多状态
            binding.pullRefresh.setLoadMore(viewModel.hasNextPage())
        }
        return viewModel
    }

    private fun updateView(data: List<CoinBean>) {
        val names = arrayListOf(binding.name1, binding.name2, binding.name3)
        val coins = arrayListOf(binding.coin1, binding.coin2, binding.coin3)
        if (viewModel.isHomePage()) {
            val size = if (data.size < 3) data.size else 3
            for (i in 0 until size) {
                names[i].text = data[i].username
                coins[i].text = data[i].coinCount
            }
            if (data.size > 3) {
                coinRankAdapter.setNewData(data.subList(2, data.size))
            }
        } else {
            coinRankAdapter.addData(data)
        }
    }

}