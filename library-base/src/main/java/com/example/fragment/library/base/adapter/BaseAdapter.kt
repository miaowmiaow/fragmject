package com.example.fragment.library.base.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * Adapter简单封装，使其支持ViewBinding
 */
abstract class BaseAdapter<T> : RecyclerView.Adapter<BaseAdapter.ViewBindHolder>() {

    companion object {
        private const val INVALID_POSITION = -1
    }

    private var currentPosition = INVALID_POSITION

    private val ids: MutableList<Int> = ArrayList()
    private var data: MutableList<T> = ArrayList()
    private var onItemClickListener: OnItemClickListener? = null
    private var onItemChildClickListener: OnItemChildClickListener? = null
    private var onItemSelectedListener: OnItemSelectedListener? = null

    fun addOnClickListener(id: Int) {
        ids.add(id)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    fun getOnItemClickListener(): OnItemClickListener? {
        return onItemClickListener
    }

    fun setOnItemChildClickListener(listener: OnItemChildClickListener) {
        onItemChildClickListener = listener
    }

    fun setOnItemSelectedListener(listener: OnItemSelectedListener) {
        onItemSelectedListener = listener
    }

    fun selectItem(position: Int) {
        currentPosition = position
        notifyItemRangeChanged(position, 1)
    }

    fun setNewData(newData: List<T>? = null) {
        this.data.clear()
        if (newData != null) {
            this.data.addAll(newData)
        }
        notifyDataSetChanged()
    }

    fun addOneData(data: T? = null) {
        if (data != null) {
            this.data.add(data)
            notifyItemRangeChanged(this.data.size - 1, 1)
        }
    }

    fun addData(newData: List<T>? = null) {
        if (newData != null) {
            this.data.addAll(newData)
            notifyItemRangeChanged(data.size - newData.size, newData.size)
        }
    }

    fun addData(index: Int, newData: List<T>) {
        this.data.addAll(index, newData)
        notifyItemRangeChanged(index, newData.size)
    }

    fun removeData(position: Int) {
        if (position < 0 || position >= data.size) return
        data.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, data.size - position)
    }

    fun clearData() {
        this.data.clear()
        notifyDataSetChanged()
    }

    fun getData(): MutableList<T> {
        return data
    }

    fun getItem(position: Int): T {
        return data[position]
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> contextToActivity(context: Context): T {
        return context as T
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBindHolder {
        return ViewBindHolder(
            onCreateViewBinding(viewType).invoke(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewBindHolder, position: Int) {
        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(holder, position)
            if (position != INVALID_POSITION && currentPosition != position) {
                onItemSelectedListener?.onItemSelected(holder.itemView, position)
                onItemSelectedListener?.onItemUnselected(holder.itemView, currentPosition)
                currentPosition = position
            }
        }
        onItemChildClickListener?.let { listener ->
            for (id in ids) {
                holder.getView<View>(id)?.setOnClickListener {
                    listener.onItemChildClick(it, holder, position)
                }
            }
        }
        onItemView(holder, position, data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }

    abstract fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding

    abstract fun onItemView(holder: ViewBindHolder, position: Int, item: T)

    class ViewBindHolder(var binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun <T : View> getView(id: Int): T? {
            return itemView.findViewById(id)
        }
    }

    interface OnItemClickListener {
        fun onItemClick(holder: ViewBindHolder, position: Int)
    }

    interface OnItemChildClickListener {
        fun onItemChildClick(view: View, holder: ViewBindHolder, position: Int)
    }

    interface OnItemSelectedListener {
        fun onItemSelected(itemView: View, position: Int)
        fun onItemUnselected(itemView: View, position: Int)
    }
}
