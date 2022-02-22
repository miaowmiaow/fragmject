package com.example.fragment.library.base.view.pull

import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fragment.library.base.R

class LoadMoreAdapter(
    private val parentView: PullRefreshLayout,
    private val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_LOAD_MORE = 201225
        private const val PRELOADING_NUMBER = 5
    }

    init {
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onChanged() {
                super.onChanged()
                notifyDataSetChanged()
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)
                notifyItemRangeChanged(positionStart, itemCount)
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                notifyItemRangeInserted(positionStart, itemCount)
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                notifyItemRangeRemoved(positionStart, itemCount)
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_LOAD_MORE) {
            object : RecyclerView.ViewHolder(AppCompatTextView(parent.context).apply {
                id = TYPE_LOAD_MORE
                width = parent.width
                gravity = Gravity.CENTER
                setPadding(20, 20, 20, 20)
                setTextColor(ContextCompat.getColor(context, R.color.text_666))
            }) {}
        } else adapter.onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_LOAD_MORE) {
            val view = holder.itemView.findViewById<AppCompatTextView>(TYPE_LOAD_MORE)
            view.text = if (itemCount > 1) {
                if (parentView.canLoadMore()) {
                    "正在加载..."
                } else "没有更多了。"
            } else ""
        } else adapter.onBindViewHolder(holder, position)
        //TYPE_LOAD_MORE，所以itemCount > 1
        if (itemCount > 1 && position >= itemCount - PRELOADING_NUMBER) {
            if (parentView.canLoadMore()) {
                parentView.loadMoreListener?.onLoadMore(parentView)
                parentView.finishLoadMore()
            }
        }
    }

    override fun getItemCount(): Int {
        return adapter.itemCount + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1)
            TYPE_LOAD_MORE
        else adapter.getItemViewType(position)
    }

}
