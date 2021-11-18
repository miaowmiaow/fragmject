package com.example.fragment.module.user.fragment

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.view.OnLoadMoreListener
import com.example.fragment.library.base.view.OnRefreshListener
import com.example.fragment.library.base.view.PullRefreshLayout
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.adapter.CoinRecordAdapter
import com.example.fragment.module.user.databinding.FragmentMyCoinBinding
import com.example.fragment.module.user.model.UserViewModel

class MyCoinFragment : RouterFragment() {

    private val coinRecordAdapter = CoinRecordAdapter()

    private val viewModel: UserViewModel by viewModels()
    private var _binding: FragmentMyCoinBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyCoinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        binding.black.setOnClickListener { activity.onBackPressed() }
        binding.rank.setOnClickListener { activity.navigation(Router.MY_COIN_TO_COIN_RANK) }
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = coinRecordAdapter
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.userCoin()
                viewModel.myCoin(true)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.myCoin(false)
            }
        })
    }

    override fun initViewModel() {
        viewModel.userCoinResult.observe(viewLifecycleOwner) { result ->
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
                result.errorCode.isNotBlank() && result.errorMsg.isNotBlank() -> {
                    activity.showTips(result.errorMsg)
                }
            }
        }
        viewModel.myCoinResult.observe(viewLifecycleOwner) { result ->
            when {
                result.errorCode == "0" -> {
                    result.data?.datas?.let { list ->
                        if (viewModel.isRefresh) {
                            coinRecordAdapter.setNewData(list)
                        } else {
                            coinRecordAdapter.addData(list)
                        }
                    }
                }
                result.errorCode.isNotBlank() -> {
                    activity.showTips(result.errorMsg)
                }
            }
            binding.pullRefresh.finishRefresh()
            binding.pullRefresh.setLoadMore(viewModel.page < viewModel.pageCont)
        }
    }

    override fun onLoad() {
        if (viewModel.myCoinResult.value == null) {
            viewModel.userCoin()
            viewModel.myCoin(true)
        }
    }

}