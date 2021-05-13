package com.example.fragment.user.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.library.common.bean.CoinBean
import com.example.fragment.module.user.R
import com.example.fragment.module.user.databinding.ItemCoinRankBinding

class CoinRankAdapter : BaseAdapter<CoinBean>() {

    private var medalList: List<Int> = listOf(
        R.drawable.ic_medal_de,
        R.drawable.ic_medal_zhi,
        R.drawable.ic_medal_ti,
        R.drawable.ic_medal_mei,
        R.drawable.ic_medal_lao,
    )

    override fun onCreateViewBinding(parent: ViewGroup, viewType: Int): ViewBinding {
        return ItemCoinRankBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: CoinBean) {
        val binding = holder.binding as ItemCoinRankBinding
        binding.medal.setImageResource(medalList[(0..4).random()])
        binding.name.text = item.username
        binding.coinCount.text = item.coinCount
    }

}