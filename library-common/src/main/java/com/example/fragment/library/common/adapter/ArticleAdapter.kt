package com.example.fragment.library.common.adapter

import android.os.Build
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import coil.load
import coil.transform.CircleCropTransformation
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.utils.BannerHelper
import com.example.fragment.library.common.R
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.bean.BannerBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.databinding.ItemArticleBannerBinding
import com.example.fragment.library.common.databinding.ItemArticleBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class ArticleAdapter : BaseAdapter<ArticleBean>() {

    companion object {
        private const val ITEM_TYPE_BANNER = 0
        private const val ITEM_TYPE_ARTICLE = 1
    }

    private var collectJob: Job? = null
    private var unCollectJob: Job? = null

    private var bannerHelper: BannerHelper? = null
    private val bannerAdapter = BannerAdapter()
    private var bannerData: MutableList<BannerBean> = ArrayList()

    private var avatarList: List<Int> = listOf(
        R.drawable.avatar_1_raster,
        R.drawable.avatar_2_raster,
        R.drawable.avatar_3_raster,
        R.drawable.avatar_4_raster,
        R.drawable.avatar_5_raster,
        R.drawable.avatar_6_raster,
    )

    fun setBannerData(data: List<BannerBean>? = null) {
        if (data != null) {
            bannerData.addAll(data)
            bannerAdapter.setNewData(bannerData)
            notifyItemChanged(0)
        }
    }

    override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
        return if (viewType == 0) {
            ItemArticleBannerBinding::inflate
        } else {
            ItemArticleBinding::inflate
        }
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: ArticleBean) {
        if (getItemViewType(position) == ITEM_TYPE_BANNER) {
            val binding = holder.binding as ItemArticleBannerBinding
            binding.banner.adapter = bannerAdapter
            bannerHelper = BannerHelper(binding.banner)
            bannerHelper?.startTimerTask()
        } else {
            val binding = holder.binding as ItemArticleBinding
            val activity: RouterActivity = contextToActivity(binding.root.context)
            binding.root.setOnClickListener {
                val args = bundleOf(Keys.URL to item.link)
                activity.navigation(Router.WEB, args)
            }
            binding.author.text = if (item.author.isNotBlank()) {
                item.author
            } else {
                "匿名"
            }
            binding.avatar.load(avatarList[position % 6]) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
            binding.avatar.setOnClickListener {
                val args = bundleOf(Keys.UID to item.userId)
                activity.navigation(Router.USER_SHARE, args)
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
                    val args = bundleOf(Keys.URL to it[0].url)
                    activity.navigation(Router.SYSTEM_URL, args)
                }
            }
            binding.top.visibility = if (item.top) View.VISIBLE else View.GONE
            binding.image.visibility = if (item.envelopePic.isNotEmpty()) {
                binding.image.load(item.envelopePic)
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
                binding.collect.setImageResource(R.drawable.ic_collect_checked)
            } else {
                binding.collect.setImageResource(R.drawable.ic_collect_unchecked_stroke)
            }
            binding.collect.setOnClickListener {
                if (item.collect) {
                    unCollect(item.id).observe(activity, { result ->
                        if (result.errorCode == "0") {
                            item.collect = false
                            binding.collect.setImageResource(R.drawable.ic_collect_unchecked_stroke)
                        } else if (result.errorCode.isNotBlank() && result.errorMsg.isNotBlank()) {
                            activity.showTips(result.errorMsg)
                        }
                    })
                } else {
                    collect(item.id).observe(activity, { result ->
                        if (result.errorCode == "0") {
                            item.collect = true
                            binding.collect.setImageResource(R.drawable.ic_collect_checked)
                        } else if (result.errorCode.isNotBlank() && result.errorMsg.isNotBlank()) {
                            activity.showTips(result.errorMsg)
                        }
                    })
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0 && bannerData.size > 0) ITEM_TYPE_BANNER else ITEM_TYPE_ARTICLE
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        bannerHelper?.stopTimerTask()
        collectJob?.cancel()
        unCollectJob?.cancel()
        super.onDetachedFromRecyclerView(recyclerView)
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

    private fun collect(id: String): MutableLiveData<HttpResponse> {
        val result = MutableLiveData<HttpResponse>()
        collectJob = CoroutineScope(Dispatchers.Main).launch {
            val request = HttpRequest("lg/collect/{id}/json").putPath("id", id)
            val response = post<HttpResponse>(request)
            result.postValue(response)
        }
        return result
    }

    private fun unCollect(id: String): MutableLiveData<HttpResponse> {
        val result = MutableLiveData<HttpResponse>()
        unCollectJob = CoroutineScope(Dispatchers.Main).launch {
            val request = HttpRequest("lg/uncollect_originId/{id}/json").putPath("id", id)
            val response = post<HttpResponse>(request)
            result.postValue(response)
        }
        return result
    }
}