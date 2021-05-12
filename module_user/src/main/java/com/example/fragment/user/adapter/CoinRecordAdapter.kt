package com.example.fragment.user.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.module.user.databinding.ItemCoinRecordBinding
import com.example.fragment.user.bean.MyCoinBean

class CoinRecordAdapter : BaseAdapter<MyCoinBean>(){

    override fun onCreateViewBinding(parent: ViewGroup, viewType: Int): ViewBinding {
        return ItemCoinRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: MyCoinBean) {
        val binding = holder.binding as ItemCoinRecordBinding
        val desc: String = item.desc
        val firstSpace = desc.indexOf(" ")
        val secondSpace = desc.indexOf(" ", firstSpace + 1)
        val time = desc.substring(0, secondSpace)
        val title = desc.substring(secondSpace + 1)
            .replace(",", "")
            .replace("：", "")
            .replace(" ", "")
        binding.coinCount.text = "+" + item.coinCount
        binding.title.text = title
        binding.time.text = time
    }

}