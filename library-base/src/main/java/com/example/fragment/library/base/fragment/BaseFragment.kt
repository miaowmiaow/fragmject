package com.example.fragment.library.base.fragment

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import coil.clear
import com.example.fragment.library.base.R
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.load

abstract class BaseFragment : Fragment() {

    /**
     * 在转场动画结束后加载数据，
     * 用于解决过度动画卡顿问题，
     * 建议大于等于转场动画时间。
     */
    private var loadDelayMillis = 250L

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
        }, {
            loadGif.clear()
        })
        view.postDelayed({ initLoad() }, loadDelayMillis)
    }

    abstract fun initView()

    /**
     * 用于ViewModel中的数据更新
     */
    abstract fun initViewModel(): BaseViewModel?

    /**
     * 本地或网络数据请在这里加载
     */
    abstract fun initLoad()

    fun hideInputMethod() {
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus ?: return
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

}