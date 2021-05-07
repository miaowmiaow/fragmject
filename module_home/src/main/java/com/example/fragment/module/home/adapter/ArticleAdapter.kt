package com.example.fragment.module.home.adapter

import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.library.base.utils.ImageLoader
import com.example.fragment.library.common.utils.StringUtils
import com.example.fragment.module.home.R
import com.example.fragment.module.home.bean.ArticleDataBean
import com.example.fragment.module.home.databinding.ItemArticleBinding

class ArticleAdapter : BaseAdapter<ArticleDataBean>() {

    companion object {
        private const val ITEM_TYPE_BANNER = 0
        private const val ITEM_TYPE_ARTICLE = 1
    }

    override fun onCreateViewBinding(parent: ViewGroup, viewType: Int): ViewBinding {
        return ItemArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: ArticleDataBean) {
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
        if (!TextUtils.isEmpty(item.envelopePic)) {
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
        if (item.collect){
            binding.ivCollect.setImageResource(R.drawable.ic_collect_checked)
        }else{
            binding.ivCollect.setImageResource(R.drawable.ic_collect_unchecked)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
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
}