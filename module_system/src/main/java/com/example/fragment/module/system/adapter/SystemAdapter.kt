package com.example.fragment.module.system.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.get
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.module.system.R
import com.example.fragment.module.system.bean.TreeBean
import com.example.fragment.module.system.databinding.ItemSystemBinding

class SystemAdapter : BaseAdapter<TreeBean>() {

    override fun onCreateViewBinding(parent: ViewGroup, viewType: Int): ViewBinding {
        return ItemSystemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: TreeBean) {
        val binding = holder.binding as ItemSystemBinding
        binding.name.text = item.name
        binding.fbl.removeAllViews()
        item.children?.forEachIndexed { index, treeBean ->
            val inflater = LayoutInflater.from(binding.fbl.context)
            val tv: TextView =
                inflater.inflate(R.layout.item_system_children, binding.fbl, false) as TextView
            tv.text = treeBean.name
            tv.setOnClickListener {
                item.childrenSelectPosition = index
                getOnItemClickListener()?.onItemClick(holder, position)
            }
            binding.fbl.addView(tv)
        }
    }

}