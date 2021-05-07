package com.example.fragment.library.base.component.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseAdapter<T> : RecyclerView.Adapter<BaseAdapter.ViewBindHolder>() {

    private val ids: MutableList<Int> = ArrayList()
    private var data: MutableList<T> = ArrayList()
    private var onItemClickListener: OnItemClickListener? = null
    private var onItemChildClickListener: OnItemChildClickListener? = null

    fun addOnClickListener(id: Int) {
        ids.add(id)
    }

    fun setNewData(newData: List<T>) {
        this.data.clear()
        this.data.addAll(newData)
        notifyDataSetChanged()
    }

    fun addOneData(data: T) {
        this.data.add(data)
        notifyItemRangeChanged(this.data.size - 1, 1)
    }

    fun addData(newData: List<T>) {
        this.data.addAll(newData)
        notifyItemRangeChanged(data.size - newData.size, newData.size)
    }

    fun addData(index: Int, newData: List<T>) {
        this.data.addAll(index, newData)
        notifyItemRangeChanged(index, newData.size)
    }

    fun remove(position: Int) {
        if (position < 0 || position >= data.size) return
        data.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, data.size - position)
    }

    fun clearData() {
        this.data.clear()
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    fun setOnItemChildClickListener(listener: OnItemChildClickListener) {
        onItemChildClickListener = listener
    }

    fun getData(): MutableList<T> {
        return data
    }

    fun getItem(position: Int): T? {
        return if (position >= 0 && position < data.size) data[position] else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBindHolder {
        return ViewBindHolder(onCreateViewBinding(parent, viewType))
    }

    override fun onBindViewHolder(holder: ViewBindHolder, position: Int) {
        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(holder, position)
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

    abstract fun onCreateViewBinding(parent: ViewGroup, viewType: Int): ViewBinding

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
}
