package com.example.fragment.module.system.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.module.system.R
import com.example.fragment.library.common.bean.TreeBean
import com.example.fragment.module.system.databinding.ItemSystemBinding

class SystemAdapter : BaseAdapter<TreeBean>() {

    override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
        return ItemSystemBinding::inflate
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: TreeBean) {
        val binding = holder.binding as ItemSystemBinding
        val baseActivity: RouterActivity = contextToActivity(binding.root.context)
        binding.name.text = item.name
        binding.fbl.removeAllViews()
        item.children?.forEachIndexed { index, treeBean ->
            val inflater = LayoutInflater.from(binding.fbl.context)
            val tv: TextView =
                inflater.inflate(R.layout.fbl_system_children, binding.fbl, false) as TextView
            tv.text = treeBean.name
            tv.setOnClickListener {
                item.childrenSelectPosition = index
                val args = Bundle()
                args.putParcelable(Keys.BEAN, item)
                baseActivity.navigation(Router.SYSTEM_LIST, args)
            }
            binding.fbl.addView(tv)
        }

        binding.root.setOnClickListener {
            val args = Bundle()
            args.putParcelable(Keys.BEAN, item)
            baseActivity.navigation(Router.SYSTEM_LIST, args)
        }
    }

}