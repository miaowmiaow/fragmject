package com.example.fragment.module.user.fragment

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.view.OnLoadMoreListener
import com.example.fragment.library.base.view.OnRefreshListener
import com.example.fragment.library.base.view.PullRefreshLayout
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.adapter.CoinRankAdapter
import com.example.fragment.module.user.databinding.FragmentCoinRankBinding
import com.example.fragment.module.user.model.CoinRankViewModel

class CoinRankFragment : RouterFragment() {

    private val viewModel: CoinRankViewModel by viewModels()
    private var _binding: FragmentCoinRankBinding? = null
    private val binding get() = _binding!!

    private val coinRankAdapter = CoinRankAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoinRankBinding.inflate(inflater, container, false)
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
        binding.rule.setOnClickListener {
            val args = bundleOf(Keys.URL to "https://www.wanandroid.com/blog/show/2653")
            activity.navigation(Router.WEB, args)
        }
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = coinRankAdapter
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getCoinRank()
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getCoinRankNext()
            }
        })
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.coinRankResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> {
                    val names = arrayListOf(binding.name1, binding.name2, binding.name3)
                    val coins = arrayListOf(binding.coin1, binding.coin2, binding.coin3)
                    val data = result.data?.datas
                    if (viewModel.isHomePage() && !data.isNullOrEmpty()) {
                        val size = if (data.size < 3) data.size else 3
                        for (i in 0 until size) {
                            names[i].text = data[i].username
                            numberAnimator(coins[i], data[i].coinCount)
                        }
                        if (data.size > 3) {
                            coinRankAdapter.setNewData(data.subList(2, data.size))
                        }
                    } else {
                        coinRankAdapter.addData(data)
                    }
                }
                else -> activity.showTips(result.errorMsg)
            }
            binding.pullRefresh.finishRefresh()
            binding.pullRefresh.setLoadMore(viewModel.hasNextPage())
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.coinRankResult.value == null) {
            viewModel.getCoinRank()
        }
    }

    private fun numberAnimator(view: TextView, number: String) {
        val from = view.text.toString().toInt()
        val to = number.toInt()
        val animator = ValueAnimator.ofInt(from, to)
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            view.text = String.format("%d", value)
        }
        animator.duration = 1000
        animator.interpolator = DecelerateInterpolator()
        animator.start()
    }

}