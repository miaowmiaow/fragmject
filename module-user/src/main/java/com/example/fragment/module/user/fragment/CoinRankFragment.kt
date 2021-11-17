package com.example.fragment.module.user.fragment

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.view.OnLoadMoreListener
import com.example.fragment.library.base.view.OnRefreshListener
import com.example.fragment.library.base.view.PullRefreshLayout
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.R
import com.example.fragment.module.user.adapter.CoinRankAdapter
import com.example.fragment.module.user.databinding.FragmentCoinRankBinding
import com.example.fragment.module.user.model.UserViewModel

class CoinRankFragment : RouterFragment() {

    private val coinRankAdapter = CoinRankAdapter()

    private val viewModel: UserViewModel by viewModels()
    private var _binding: FragmentCoinRankBinding? = null
    private val binding get() = _binding!!

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
            val args = Bundle()
            args.putString(Keys.URL, "https://www.wanandroid.com/blog/show/2653")
            activity.navigation(R.id.action_coin_rank_to_web, args)
        }
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = coinRankAdapter
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.coinRank(true)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.coinRank(false)
            }
        })
    }

    override fun initViewModel() {
        viewModel.coinRankResult.observe(viewLifecycleOwner) { result ->
            when {
                result.errorCode == "0" -> {
                    result.data?.datas?.let { list ->
                        if (viewModel.isRefresh && list.isNotEmpty()) {
                            binding.name1.text = list[0].username
                            numberAnimator(binding.coin1, list[0].coinCount)
                            if (list.size > 1) {
                                binding.name2.text = list[1].username
                                numberAnimator(binding.coin2, list[1].coinCount)
                            }
                            if (list.size > 2) {
                                binding.name3.text = list[2].username
                                numberAnimator(binding.coin3, list[2].coinCount)
                            }
                            if (list.size > 3) {
                                coinRankAdapter.setNewData(list.subList(2, list.size))
                            }
                        } else {
                            coinRankAdapter.addData(list)
                        }
                    }
                }
                result.errorCode.isNotBlank() && result.errorMsg.isNotBlank() -> {
                    activity.showTips(result.errorMsg)
                }
            }
            binding.pullRefresh.finishRefresh()
            binding.pullRefresh.setLoadMore(viewModel.page < viewModel.pageCont)
        }
    }

    override fun onLoad() {
        if (viewModel.coinRankResult.value == null) {
            viewModel.coinRank(true)
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