package com.example.fragment.library.base.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * Adapter简单封装，使其支持ViewBinding
 */
abstract class BaseAdapter<T>(newData: List<T>? = null) :
    RecyclerView.Adapter<BaseAdapter.ViewBindHolder>() {

    companion object {
        private const val INVALID_POSITION = -1
    }

    private var currentPosition = INVALID_POSITION

    private val ids: MutableList<Int> = ArrayList()
    private var data: MutableList<T> = ArrayList()
    private var _onItemClickListener: OnItemClickListener? = null
    private var _onItemChildClickListener: OnItemChildClickListener? = null
    private var _onItemSelectedListener: OnItemSelectedListener? = null

    init {
        setNewData(newData)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        _onItemClickListener = null
        _onItemChildClickListener = null
        _onItemSelectedListener = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBindHolder {
        return ViewBindHolder(
            onCreateViewBinding(viewType).invoke(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewBindHolder, position: Int) {
        holder.itemView.setOnClickListener {
            _onItemClickListener?.onItemClick(holder, position)
            if (position != INVALID_POSITION && currentPosition != position) {
                _onItemSelectedListener?.onItemSelected(holder.itemView, position)
                _onItemSelectedListener?.onItemUnselected(holder.itemView, currentPosition)
                currentPosition = holder.adapterPosition
            }
        }
        _onItemChildClickListener?.let { listener ->
            for (id in ids) {
                holder.getView<View>(id)?.setOnClickListener {
                    listener.onItemChildClick(it, holder, position)
                }
            }
        }
        onItemView(holder, position, this.data[position])
    }

    override fun getItemCount(): Int {
        return this.data.size
    }

    fun addOnClickListener(id: Int) {
        ids.add(id)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        _onItemClickListener = listener
    }

    fun getOnItemClickListener(): OnItemClickListener? {
        return _onItemClickListener
    }

    fun setOnItemChildClickListener(listener: OnItemChildClickListener) {
        _onItemChildClickListener = listener
    }

    fun setOnItemSelectedListener(listener: OnItemSelectedListener) {
        _onItemSelectedListener = listener
    }

    fun selectItem(position: Int) {
        currentPosition = position
        notifyItemChanged(position)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setNewData(newData: List<T>? = null) {
        if (!newData.isNullOrEmpty()) {
            this.data.clear()
            this.data.addAll(newData)
            notifyDataSetChanged()
        }
    }

    fun addData(newData: List<T>? = null) {
        if (!newData.isNullOrEmpty()) {
            this.data.addAll(newData)
            notifyItemRangeChanged(this.data.size - newData.size, newData.size)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addData(index: Int, newData: List<T>? = null) {
        if (newData != null) {
            this.data.addAll(index, newData)
            notifyDataSetChanged()
        }
    }

    fun removeData(position: Int) {
        if (position < 0 || position >= this.data.size) return
        this.data.removeAt(position)
        notifyItemRemoved(position)
    }

    fun clearData() {
        val itemCount = this.data.size
        this.data.clear()
        notifyItemRangeRemoved(0, itemCount)
    }

    fun getData(): MutableList<T> {
        return ArrayList(this.data)
    }

    fun getItem(position: Int): T {
        return this.data[position]
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> contextToT(context: Context): T {
        return context as T
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
