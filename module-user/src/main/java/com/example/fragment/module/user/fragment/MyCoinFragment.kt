package com.example.fragment.module.user.fragment

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.view.pull.OnLoadMoreListener
import com.example.fragment.library.base.view.pull.OnRefreshListener
import com.example.fragment.library.base.view.pull.PullRefreshLayout
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.adapter.MyCoinAdapter
import com.example.fragment.module.user.databinding.MyCoinFragmentBinding
import com.example.fragment.module.user.model.MyCoinViewModel

class MyCoinFragment : RouterFragment() {

    private val viewModel: MyCoinViewModel by viewModels()
    private var _binding: MyCoinFragmentBinding? = null
    private val binding get() = _binding!!
    private var _coinRecordAdapter: MyCoinAdapter? = null
    private val coinRecordAdapter get() = _coinRecordAdapter!!
    private var _coinCountAnimator: ValueAnimator? = null
    private val coinCountAnimator get() = _coinCountAnimator!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MyCoinFragmentBinding.inflate(inflater, container, false)
        _coinRecordAdapter = MyCoinAdapter()
        _coinCountAnimator = ValueAnimator()
        coinCountAnimator.addUpdateListener {
            val value = it.animatedValue as Int
            binding.coinCount.text = String.format("%d", value)
        }
        coinCountAnimator.duration = 1000
        coinCountAnimator.interpolator = DecelerateInterpolator()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.listData = coinRecordAdapter.getData()
        viewModel.listScroll = binding.list.computeVerticalScrollOffset()
        coinCountAnimator.cancel()
        _coinCountAnimator = null
        _coinRecordAdapter = null
        _binding = null
    }

    override fun initView() {
        binding.black.setOnClickListener { activity.onBackPressed() }
        binding.rank.setOnClickListener { activity.navigation(Router.COIN2RANK) }
        //我的积分列表
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = coinRecordAdapter
        //下拉刷新
        binding.pullRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.getMyCoinHome()
            }
        })
        //加载更多
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
                viewModel.getMyCoinNext()
            }
        })
        binding.coordinator.post {
            binding.coordinator.setMaxScrollY(binding.coinCount.height)
            binding.pullRefresh.layoutParams.height = binding.coordinator.height
        }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.userCoinResult().observe(viewLifecycleOwner) { result ->
            httpParseSuccess(result) { bean ->
                bean.data?.let {
                    val from = binding.coinCount.text.toString().toInt()
                    val to = it.coinCount.toInt()
                    coinCountAnimator.setIntValues(from, to)
                    coinCountAnimator.start()
                }
            }
        }
        if (!viewModel.listData.isNullOrEmpty()) {
            coinRecordAdapter.setNewData(viewModel.listData)
            binding.list.scrollTo(0, viewModel.listScroll)
        }
        viewModel.myCoinResult().observe(viewLifecycleOwner) {
            httpParseSuccess(it) { result ->
                if (viewModel.isHomePage()) {
                    coinRecordAdapter.setNewData(result.data?.datas)
                } else {
                    coinRecordAdapter.addData(result.data?.datas)
                }
            }
            //结束下拉刷新状态
            binding.pullRefresh.finishRefresh()
            //设置加载更多状态
            binding.pullRefresh.setLoadMore(viewModel.hasNextPage())
        }
        return viewModel
    }

}