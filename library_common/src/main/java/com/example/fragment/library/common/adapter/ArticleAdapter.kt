package com.example.fragment.library.common.adapter

import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.library.base.utils.ImageLoader
import com.example.fragment.library.base.utils.SimpleBannerHelper
import com.example.fragment.library.common.R
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.bean.BannerBean
import com.example.fragment.library.common.databinding.ItemArticleBannerBinding
import com.example.fragment.library.common.databinding.ItemArticleBinding
import com.example.fragment.library.common.utils.StringUtils

class ArticleAdapter : BaseAdapter<ArticleBean>() {

    companion object {
        private const val ITEM_TYPE_BANNER = 0
        private const val ITEM_TYPE_ARTICLE = 1
    }

    private var bannerHelper: SimpleBannerHelper? = null
    private val bannerAdapter = BannerAdapter()
    private var bannerData: MutableList<BannerBean> = ArrayList()

    override fun onCreateViewBinding(parent: ViewGroup, viewType: Int): ViewBinding {
        return if (viewType == 0) {
            ItemArticleBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        } else {
            ItemArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        }
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: ArticleBean) {
        if (getItemViewType(position) == ITEM_TYPE_BANNER) {
            val binding = holder.binding as ItemArticleBannerBinding
            binding.banner.adapter = bannerAdapter
            bannerHelper = SimpleBannerHelper(binding.banner)
            bannerHelper?.startTimerTask()
        } else {
            val binding = holder.binding as ItemArticleBinding
            binding.tvTop.visibility = if (item.top) View.VISIBLE else View.GONE
            binding.tvNew.visibility = if (item.fresh) View.VISIBLE else View.GONE
            binding.tvAuthor.text = item.author
            if (item.tags != null && item.tags.isNotEmpty()) {
                binding.tvTag.text = item.tags[0].name
                binding.tvTag.visibility = View.VISIBLE
            } else {
                binding.tvTag.visibility = View.GONE
            }
            binding.tvTime.text = item.niceDate
            if (item.envelopePic.isNotEmpty()) {
                ImageLoader.with(binding.ivImg.context).load(item.envelopePic).into(binding.ivImg)
                binding.ivImg.visibility = View.VISIBLE
            } else {
                binding.ivImg.visibility = View.GONE
            }
            binding.tvTitle.text = Html.fromHtml(item.title)
            if (TextUtils.isEmpty(item.desc)) {
                binding.tvDesc.visibility = View.GONE
                binding.tvTitle.isSingleLine = false
            } else {
                binding.tvDesc.visibility = View.VISIBLE
                binding.tvTitle.isSingleLine = true
                var desc = Html.fromHtml(item.desc).toString()
                desc = StringUtils.removeAllBank(desc, 2)
                binding.tvDesc.text = desc
            }
            binding.tvChapterName.text = Html.fromHtml(
                formatChapterName(
                    item.superChapterName,
                    item.chapterName
                )
            )
            if (item.collect) {
                binding.ivCollect.setImageResource(R.drawable.ic_collect_checked)
            } else {
                binding.ivCollect.setImageResource(R.drawable.ic_collect_unchecked_stroke)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0 && bannerData.size > 0) ITEM_TYPE_BANNER else ITEM_TYPE_ARTICLE
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        bannerHelper?.apply {
            stopTimerTask()
        }
        super.onDetachedFromRecyclerView(recyclerView)
    }

    private fun formatChapterName(vararg names: String): String? {
        val format = StringBuilder()
        for (name in names) {
            if (!TextUtils.isEmpty(name)) {
                if (format.isNotEmpty()) {
                    format.append("·")
                }
                format.append(name)
            }
        }
        return format.toString()
    }

    fun setBannerData(data: List<BannerBean>) {
        bannerData.addAll(data)
        bannerAdapter.setNewData(bannerData)
        notifyItemChanged(0)
    }
}