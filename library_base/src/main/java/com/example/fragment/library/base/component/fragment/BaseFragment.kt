package com.example.fragment.library.base.component.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {

    private var firstLoadFragment = true

    abstract fun onFirstLoad()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.isClickable = true
        view.isFocusable = true
    }

    override fun onResume() {
        super.onResume()
        if (firstLoadFragment) {
            onFirstLoad()
            firstLoadFragment = !firstLoadFragment
        }
        hiddenFragment(true)
    }

    override fun onPause() {
        super.onPause()
        hiddenFragment(false)
    }

    open fun hiddenFragment(hidden: Boolean) {}

}