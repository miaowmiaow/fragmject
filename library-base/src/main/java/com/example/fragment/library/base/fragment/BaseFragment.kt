package com.example.fragment.library.base.fragment

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import coil.dispose
import coil.load
import com.example.fragment.library.base.R
import com.example.fragment.library.base.model.BaseViewModel

/**
 * 注意：Fragment 的存在时间比其视图长。请务必在 Fragment 的 onDestroyView() 方法中清除对视图的所有引用。
 */
abstract class BaseFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.isClickable = true
        view.isFocusable = true
        val loadGif = ImageView(view.context)
        val layout = view as RelativeLayout
        layout.addView(loadGif, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).also {
            it.addRule(RelativeLayout.CENTER_IN_PARENT)
        })
        initView()
        initViewModel()?.progress(viewLifecycleOwner, {
            loadGif.load(R.drawable.icons8_monkey)
            loadGif.visibility = View.VISIBLE
        }, {
            loadGif.dispose()
            loadGif.visibility = View.GONE
        })
    }

    /**
     * Fragment 的存在时间比其视图长，所有视图的引用都必须在此进行清除
     */
    override fun onDestroyView() {
        super.onDestroyView()
    }

    abstract fun initView()

    abstract fun initViewModel(): BaseViewModel?

    fun hideInputMethod() {
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus ?: return
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

}