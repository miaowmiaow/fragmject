package com.example.fragment.library.common.adapter

import android.net.Uri
import android.os.Build
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.viewbinding.ViewBinding
import coil.load
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.post
import com.example.fragment.library.common.R
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.databinding.ArticleItemBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class ArticleAdapter : BaseAdapter<ArticleBean>() {

    private var avatarList: List<Int> = listOf(
        R.drawable.avatar_1_raster,
        R.drawable.avatar_2_raster,
        R.drawable.avatar_3_raster,
        R.drawable.avatar_4_raster,
        R.drawable.avatar_5_raster,
        R.drawable.avatar_6_raster,
    )

    override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
        return ArticleItemBinding::inflate
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: ArticleBean) {
        val binding = holder.binding as ArticleItemBinding
        val activity: RouterActivity = contextToActivity(binding.root.context)
        binding.root.setOnClickListener {
            val url = Uri.encode(item.link)
            activity.navigation(Router.WEB, bundleOf(Keys.URL to url))
        }
        binding.author.text = if (item.author.isNotBlank()) item.author else "匿名"
        binding.avatar.load(avatarList[position % 6]) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
        binding.avatar.setOnClickListener {
            activity.navigation(Router.SHARE_ARTICLE, bundleOf(Keys.UID to item.userId))
        }
        binding.time.text = item.niceDate
        binding.newest.visibility = if (item.fresh) View.VISIBLE else View.GONE
        binding.tag.visibility = if (!item.tags.isNullOrEmpty()) {
            binding.tag.text = item.tags[0].name
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.tag.setOnClickListener {
            item.tags?.let {
                val url = Uri.encode(it[0].url)
                activity.navigation(Router.SYSTEM_URL, bundleOf(Keys.URL to url))
            }
        }
        binding.tag.visibility = if (!item.tags.isNullOrEmpty()) View.VISIBLE else View.GONE
        binding.top.visibility = if (item.top) View.VISIBLE else View.GONE
        binding.image.visibility = if (item.envelopePic.isNotEmpty()) {
            binding.image.load(item.envelopePic) {
                crossfade(true)
                transformations(RoundedCornersTransformation(15f))
            }
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.title.text = fromHtml(item.title)
        if (TextUtils.isEmpty(item.desc)) {
            binding.title.isSingleLine = false
            binding.desc.visibility = View.GONE
        } else {
            binding.title.isSingleLine = true
            var desc = fromHtml(item.desc)
            desc = removeAllBank(desc, 2)
            binding.desc.text = desc
            binding.desc.visibility = View.VISIBLE
        }
        binding.chapterName.text = fromHtml(
            formatChapterName(item.superChapterName, item.chapterName)
        )
        binding.chapterName.setOnClickListener {
            val args = bundleOf(Keys.CID to item.chapterId)
            activity.navigation(Router.SYSTEM, args)
        }
        if (item.collect) {
            binding.collect.load(R.drawable.ic_collect_checked)
        } else {
            binding.collect.load(R.drawable.ic_collect_unchecked_stroke)
        }
        binding.collect.setOnClickListener {
            if (item.collect) {
                binding.collect.load(R.drawable.ic_collect_unchecked_stroke)
                unCollect(item.id)
            } else {
                binding.collect.load(R.drawable.ic_collect_checked)
                collect(item.id)
            }
            item.collect = !item.collect
        }
    }

    private fun fromHtml(str: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(str, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            Html.fromHtml(str).toString()
        }
    }

    private fun formatChapterName(vararg names: String): String {
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

    private fun removeAllBank(str: String?, count: Int): String {
        var s = ""
        if (str != null) {
            val p = Pattern.compile("\\s{$count,}|\t|\r|\n")
            val m = p.matcher(str)
            s = m.replaceAll(" ")
        }
        return s
    }

    private fun collect(id: String) {
        CoroutineScope(Dispatchers.Main).launch {
            post<HttpResponse>(HttpRequest("lg/collect/{id}/json").putPath("id", id))
        }
    }

    private fun unCollect(id: String) {
        CoroutineScope(Dispatchers.Main).launch {
            post<HttpResponse>(HttpRequest("lg/uncollect_originId/{id}/json").putPath("id", id))
        }
    }
}