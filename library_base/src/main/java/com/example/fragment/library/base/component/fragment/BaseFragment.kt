package com.example.fragment.library.base.component.fragment

import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {

    private var firstLoadFragment = true
    var hiddenFragment = false

    abstract fun onFirstLoad()

    override fun onResume() {
        super.onResume()
        if (firstLoadFragment) {
            onFirstLoad()
            firstLoadFragment = !firstLoadFragment
        }
        hiddenFragment = true
    }

    override fun onPause() {
        super.onPause()
        hiddenFragment = false
    }

}