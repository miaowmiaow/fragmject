package com.example.fragment.library.base.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

open class BaseViewPagerAdapter(
    fragment: Fragment,
    private var data: ArrayList<out Fragment>
) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int {
        return data.size
    }

    override fun createFragment(position: Int): Fragment {
        return data[position]
    }
}
