package com.example.fragment.module.system.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.module.system.R
import com.example.fragment.module.system.bean.TreeBean
import com.example.fragment.module.system.databinding.ItemSystemBinding
import com.google.android.flexbox.FlexboxLayout

class SystemAdapter : BaseAdapter<TreeBean>() {

    override fun onCreateViewBinding(parent: ViewGroup, viewType: Int): ViewBinding {
        return ItemSystemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: TreeBean) {
        val binding = holder.binding as ItemSystemBinding
        binding.name.text = item.name
        fillFlexboxLayout(binding.fbl, item.children)
    }

    private fun fillFlexboxLayout(fbl: FlexboxLayout, data: List<TreeBean>? = null) {
        fbl.removeAllViews()
        data?.forEach {
            val inflater = LayoutInflater.from(fbl.context)
            val tv: TextView =
                inflater.inflate(R.layout.item_system_children, fbl, false) as TextView
            tv.text = it.name
            fbl.addView(tv)
        }
    }

}