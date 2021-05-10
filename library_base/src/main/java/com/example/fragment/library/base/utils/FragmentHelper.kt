package com.example.fragment.library.base.utils

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle

class FragmentHelper {

    companion object {

        fun replace(
            manager: FragmentManager,
            resId: Int,
            clazz: Class<out Fragment>,
            bundle: Bundle?,
            addToBackStack: Boolean
        ): Fragment? {
            val transaction = manager.beginTransaction()
            val tag = clazz.simpleName
            var fragment = manager.findFragmentByTag(tag)
            if (fragment == null) {
                try {
                    fragment = clazz.newInstance()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (fragment != null) {
                if (bundle != null && !bundle.isEmpty) {
                    val args = fragment.arguments
                    if (args != null) {
                        args.putAll(bundle)
                    } else {
                        fragment.arguments = bundle
                    }
                }
                transaction.replace(resId, fragment, tag)
                if (addToBackStack) {
                    transaction.addToBackStack(tag)
                }
                transaction.commitAllowingStateLoss()
            }
            return fragment
        }

        fun switcher(
            manager: FragmentManager,
            resId: Int,
            curClazz: Class<out Fragment>?,
            clazz: Class<out Fragment>,
            bundle: Bundle?,
            addToBackStack: Boolean
        ): Class<out Fragment>? {
            val transaction = manager.beginTransaction()
            //        transaction.setCustomAnimations(
//                R.anim.slide_right_in,
//                R.anim.slide_left_out,
//                R.anim.slide_left_in,
//                R.anim.slide_right_out
//        );
            if (curClazz != null) {
                val tag = curClazz.simpleName
                val curFragment = manager.findFragmentByTag(tag)
                if(curFragment != null){
                    transaction.hide(curFragment)
                    transaction.setMaxLifecycle(curFragment, Lifecycle.State.STARTED)
                }
            }
            val tag = clazz.simpleName
            var fragment = manager.findFragmentByTag(tag)
            if (fragment == null) {
                try {
                    fragment = clazz.newInstance()
                    transaction.add(resId, fragment, tag)
                    transaction.setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            } else {
                transaction.show(fragment)
                transaction.setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
            }
            if (fragment != null) {
                if (bundle != null && !bundle.isEmpty) {
                    val arguments = fragment.arguments
                    if (arguments != null) {
                        arguments.putAll(bundle)
                    } else {
                        fragment.arguments = bundle
                    }
                }
                if (addToBackStack) {
                    transaction.addToBackStack(tag)
                }
                transaction.commitAllowingStateLoss()
            }
            return clazz
        }

        fun pop(manager: FragmentManager, clazz: Class<out Fragment>): Class<out Fragment>? {
            manager.popBackStackImmediate(clazz.simpleName, 0)
            return clazz
        }
    }

}