package com.example.fragment.user.fragment

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.view.PullRefreshLayout
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.databinding.FragmentMyCoinBinding
import com.example.fragment.user.adapter.CoinRecordAdapter
import com.example.fragment.user.model.UserViewModel

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
        coinRecordAdapter.setOnItemClickListener(object : BaseAdapter.OnItemClickListener {
            override fun onItemClick(holder: BaseAdapter.ViewBindHolder, position: Int) {
                Log.i("----------", "----------onItemClick")
            }
        })
        coinRecordAdapter.setOnItemChildClickListener(object :
            BaseAdapter.OnItemChildClickListener {
            override fun onItemChildClick(
                view: View,
                holder: BaseAdapter.ViewBindHolder,
                position: Int
            ) {
                Log.i("----------", "----------onItemChildClick")
            }
        })
        binding.pullRefresh.setOnRefreshListener(object :
            PullRefreshLayout.OnRefreshListener {
            override fun onRefresh(refreshLayout: PullRefreshLayout) {
                viewModel.userCoin()
                viewModel.myCoin(true)
            }
        })
        binding.pullRefresh.setOnLoadMoreListener(binding.list, object :
            PullRefreshLayout.OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: PullRefreshLayout) {
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
                    WanHelper.setUser(UserBean())
                    baseActivity.showTips(result.errorMsg)
                    baseActivity.navigation(Router.LOGIN)
                }
                result.errorCode.isNotBlank() && result.errorMsg.isNotBlank() -> {
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
                    WanHelper.setUser(UserBean())
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