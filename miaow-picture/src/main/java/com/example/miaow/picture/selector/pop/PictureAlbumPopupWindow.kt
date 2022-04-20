package com.example.miaow.picture.selector.pop

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import android.widget.RelativeLayout.LayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.miaow.picture.databinding.PictureBucketPopupWindowBinding
import com.example.miaow.picture.selector.adapter.PictureBucketAdapter
import com.example.miaow.picture.selector.bean.Bucket


class PictureBucketPopupWindow(context: Context) : PopupWindow(context) {

    private var _binding: PictureBucketPopupWindowBinding? = null
    private val binding get() = _binding!!
    private val bucketAdapter = PictureBucketAdapter()

    private var onBucketSelectedListener: OnBucketSelectedListener? = null

    init {
        _binding = PictureBucketPopupWindowBinding.inflate(LayoutInflater.from(context))
        contentView = binding.root
        this.width = LayoutParams.MATCH_PARENT
        this.isFocusable = false
        this.isOutsideTouchable = false
        setBackgroundDrawable(ColorDrawable())
        initView()
    }

    override fun dismiss() {
        super.dismiss()
        _binding = null
    }

    private fun initView() {
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)
        binding.list.adapter = bucketAdapter
        bucketAdapter.setOnItemClickListener(object : BaseAdapter.OnItemClickListener {
            override fun onItemClick(holder: BaseAdapter.ViewBindHolder, position: Int) {
                bucketAdapter.setSelectedPosition(position)
                onBucketSelectedListener?.onBucketSelected(bucketAdapter.getItem(position).name)
                dismiss()
            }
        })
    }

    fun setBucketData(data: List<Bucket>, position: Int) {
        bucketAdapter.setNewData(data)
        bucketAdapter.setSelectedPosition(position)
    }

    fun show(parent: View) {
        showAsDropDown(parent)
    }

    fun setOnBucketSelectedListener(listener: OnBucketSelectedListener) {
        this.onBucketSelectedListener = listener
    }

    interface OnBucketSelectedListener {
        fun onBucketSelected(name: String)
    }

}