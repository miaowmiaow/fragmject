package com.example.miaow.picture.selector.adapter

import android.annotation.SuppressLint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.viewbinding.ViewBinding
import coil.load
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.utils.dp2px
import com.example.fragment.library.base.utils.screenWidth
import com.example.miaow.picture.R
import com.example.miaow.picture.databinding.PictureSelectorItemBinding
import com.example.miaow.picture.selector.bean.MediaBean

class PictureSelectorAdapter : BaseAdapter<MediaBean>() {

    companion object {
        private const val TYPE_CAMERA = 220531
    }

    private val currSelectPosition: MutableList<Int> = ArrayList()
    private var onPictureClickListener: OnPictureClickListener? = null

    init {
        setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClick(holder: ViewBindHolder, position: Int) {
                if (position == 0) {
                    onPictureClickListener?.onCamera()
                } else {
                    val realSelectPosition = position - 1
                    onPictureClickListener?.onSelectClick(realSelectPosition)
                }
            }
        })
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.itemAnimator?.let {
            //通过关闭默认动画解决刷新闪烁问题
            (it as SimpleItemAnimator).supportsChangeAnimations = false
        }
    }

    override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
        return PictureSelectorItemBinding::inflate
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onItemView(holder: ViewBindHolder, position: Int, item: MediaBean) {
        val binding = holder.binding as PictureSelectorItemBinding
        binding.root.layoutParams.apply {
            height = screenWidth() / 4
        }
        if (getItemViewType(position) == TYPE_CAMERA) {
            binding.root.setBackgroundColor(
                ContextCompat.getColor(
                    binding.image.context,
                    R.color.black
                )
            )
            binding.image.layoutParams.apply {
                width = dp2px(36f).toInt()
                height = dp2px(30f).toInt()
            }
            binding.image.setBackgroundResource(R.drawable.ps_camera)
            binding.originalBox.visibility = View.GONE
        } else {
            binding.root.setBackgroundColor(
                ContextCompat.getColor(
                    binding.image.context,
                    R.color.transparent
                )
            )
            binding.image.layoutParams.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            binding.image.load(item.uri)
            binding.originalBox.visibility = View.VISIBLE
        }
        val realPosition = position - 1
        if (currSelectPosition.contains(realPosition)) {
            if (!currSelectPosition.contains(realPosition)) {
                currSelectPosition.add(realPosition)
            }
            binding.dim.alpha = 0.5f
            binding.serial.text = (currSelectPosition.indexOf(realPosition) + 1).toString()
            binding.originalBox.isSelected = true
        } else {
            if (currSelectPosition.contains(realPosition)) {
                currSelectPosition.remove(realPosition)
            }
            binding.dim.alpha = if (currSelectPosition.size < 9) 0f else 0.9f
            binding.serial.text = ""
            binding.originalBox.isSelected = false
        }
        binding.originalBox.setOnClickListener {
            if (currSelectPosition.contains(realPosition)) {
                currSelectPosition.remove(realPosition)
            } else if (currSelectPosition.size < 9) {
                currSelectPosition.add(realPosition)
            }
            notifyItemChanged(position)
            currSelectPosition.forEach {
                notifyItemChanged(it + 1)
            }
        }
        when {
            item.longImage() -> {
                binding.tag.visibility = View.VISIBLE
                binding.tag.text = "长图"
            }
            item.gifImage() -> {
                binding.tag.visibility = View.VISIBLE
                binding.tag.text = "动图"
            }
            else -> {
                binding.tag.visibility = View.GONE
                binding.tag.text = ""
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0)
            TYPE_CAMERA
        else super.getItemViewType(position)
    }

    fun setAlbumData(data: List<MediaBean>) {
        currSelectPosition.clear()
        val newData: MutableList<MediaBean> = ArrayList()
        newData.add(MediaBean("相机", Uri.EMPTY))
        newData.addAll(data)
        setNewData(newData)
    }

    fun setSelectPosition(selectPosition: List<Int>) {
        val notifyPosition: MutableList<Int> = ArrayList()
        notifyPosition.addAll(currSelectPosition)
        currSelectPosition.forEach {
            val realPosition = it + 1
            if (!notifyPosition.contains(realPosition)) {
                notifyPosition.add(realPosition)
            }
        }
        currSelectPosition.clear()
        selectPosition.forEach {
            currSelectPosition.add(it)
            val realPosition = it + 1
            if (!notifyPosition.contains(realPosition)) {
                notifyPosition.add(realPosition)
            }
        }
        notifyPosition.forEach {
            notifyItemChanged(it)
        }
    }

    fun getSelectPosition(): List<Int> {
        return currSelectPosition
    }

    fun getSelectPositionData(): List<MediaBean> {
        val data: MutableList<MediaBean> = ArrayList()
        currSelectPosition.forEach { position ->
            data.add(getItem(position + 1))
        }
        return data
    }

    fun setOnPictureClickListener(listener: OnPictureClickListener) {
        onPictureClickListener = listener
    }

}

interface OnPictureClickListener {
    fun onCamera()
    fun onSelectClick(position: Int)
}