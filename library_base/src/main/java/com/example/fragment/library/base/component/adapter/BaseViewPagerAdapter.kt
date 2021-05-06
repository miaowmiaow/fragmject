package com.example.fragment.library.base.component.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

open class BaseViewPagerAdapter(fm: FragmentManager, private var clazzList: ArrayList<Fragment>) :
    FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getCount(): Int {
        return clazzList.size
    }

    override fun getItem(position: Int): Fragment {
        return clazzList[position]
    }

}
