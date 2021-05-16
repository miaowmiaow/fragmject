package com.example.fragment.user.fragment

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.component.view.SimplePullRefreshLayout
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.databinding.FragmentMyCoinBinding
import com.example.fragment.user.adapter.CoinRecordAdapter
import com.example.fragment.user.model.UserModel

class MyCoinFragment : ViewModelFragment<FragmentMyCoinBinding, UserModel>() {

    private val coinRecordAdapter = CoinRecordAdapter()

    override fun setViewBinding(inflater: LayoutInflater): FragmentMyCoinBinding {
        return FragmentMyCoinBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
    }

    private fun setupView() {
        binding.black.setOnClickListener { baseActivity.onBackPressed() }
        binding.rank.setOnClickListener { baseActivity.navigation(Router.COIN_RANK) }
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = coinRecordAdapter
        binding.pullRefresh.setOnRefreshListener(object :
            SimplePullRefreshLayout.OnRefreshListener {
            override fun onRefresh(refreshLayout: SimplePullRefreshLayout) {
                viewModel.userCoin()
                viewModel.myCoin(true)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object :
            SimplePullRefreshLayout.OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: SimplePullRefreshLayout) {
                viewModel.myCoin(false)
            }
        })
        binding.pullRefresh.setRefreshing()
    }

    private fun update() {
        viewModel.userCoinResult.observe(viewLifecycleOwner, { result ->
            when {
                result.errorCode == "0" -> {
                    result.data?.let { coinBean ->
                        WanHelper.setCoin(coinBean)
                        val from = binding.coinCount.text.toString().toInt()
                        val to = coinBean.coinCount.toInt()
                        val animator = ValueAnimator.ofInt(from, to)
                        animator.addUpdateListener { animation ->
                            val value = animation.animatedValue as Int
                            binding.coinCount.text = String.format("%d", value)
                        }
                        animator.duration = 1000
                        animator.interpolator = DecelerateInterpolator()
                        animator.start()
                    }
                }
                result.errorCode == "-1001" -> {
                    baseActivity.showTips(result.errorMsg)
                    baseActivity.navigation(Router.LOGIN)
                }
                result.errorCode.isNotBlank() -> {
                    baseActivity.showTips(result.errorMsg)
                }
            }
        })
        viewModel.myCoinResult.observe(viewLifecycleOwner, { result ->
            when {
                result.errorCode == "0" -> {
                    result.data?.datas?.let { list ->
                        if (viewModel.isRefresh) {
                            coinRecordAdapter.setNewData(list)
                        } else {
                            coinRecordAdapter.addData(list)
                            binding.pullRefresh.setLoadMore(true)
                        }
                    }
                }
                result.errorCode == "-1001" -> {
                    baseActivity.showTips(result.errorMsg)
                    baseActivity.navigation(Router.LOGIN)
                }
                result.errorCode.isNotBlank() -> {
                    baseActivity.showTips(result.errorMsg)
                }
            }
            if (binding.pullRefresh.isRefresh()) {
                binding.pullRefresh.finishRefresh()
            }
            if (viewModel.page >= viewModel.pageCont) {
                binding.pullRefresh.setLoadMore(false)
            }
        })
    }

}