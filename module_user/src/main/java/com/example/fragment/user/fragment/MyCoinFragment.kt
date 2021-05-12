package com.example.fragment.user.fragment

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.component.view.SimplePullRefreshLayout
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.library.common.utils.UserHelper
import com.example.fragment.module.user.databinding.FragmentMyCoinBinding
import com.example.fragment.user.adapter.CoinRecordAdapter
import com.example.fragment.user.model.CoinModel

class MyCoinFragment : ViewModelFragment<FragmentMyCoinBinding, CoinModel>() {

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
        binding.black.setOnClickListener {
            baseActivity.onBackPressed()
        }
        binding.rank.setOnClickListener {

        }
        binding.rule.setOnClickListener {
            val args = Bundle()
            args.putString(Keys.URL, "https://www.wanandroid.com/blog/show/2653")
            baseActivity.navigation(Router.WEB, args)
        }
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = coinRecordAdapter
        binding.pullRefresh.setOnRefreshListener(object :
            SimplePullRefreshLayout.OnRefreshListener {
            override fun onRefresh(refreshLayout: SimplePullRefreshLayout) {
                viewModel.getUserCoin()
                viewModel.getMyCoinList(true)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object :
            SimplePullRefreshLayout.OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: SimplePullRefreshLayout) {
                viewModel.getMyCoinList(false)
            }
        })
        binding.pullRefresh.setRefreshing()
    }

    private fun update() {
        viewModel.userCoinResult.observe(viewLifecycleOwner, { result ->
            if (result.errorCode == "0") {
                result.data?.let { coinBean ->
                    UserHelper.setCoin(coinBean)
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
            } else {
                baseActivity.showTips(result.errorMsg)
            }
        })
        viewModel.myCoinListResult.observe(viewLifecycleOwner, { result ->
            if (result.errorCode == "0") {
                result.data?.datas?.let { list ->
                    if (viewModel.isRefresh) {
                        coinRecordAdapter.setNewData(list)
                    } else {
                        coinRecordAdapter.addData(list)
                        binding.pullRefresh.setLoadMore(true)
                    }
                }
            } else {
                baseActivity.showTips(result.errorMsg)
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