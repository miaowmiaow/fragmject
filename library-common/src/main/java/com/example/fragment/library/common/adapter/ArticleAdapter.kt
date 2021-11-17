package com.example.fragment.library.common.adapter

import android.net.Uri
import android.os.Build
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.utils.BannerHelper
import com.example.fragment.library.base.utils.ImageLoader
import com.example.fragment.library.common.R
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.bean.BannerBean
import com.example.fragment.library.common.bean.TreeBean
import com.example.fragment.library.common.databinding.ItemArticleBannerBinding
import com.example.fragment.library.common.databinding.ItemArticleBinding
import com.example.fragment.library.common.utils.WanHelper
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

    init {
        addOnClickListener(R.id.rl_item)
        addOnClickListener(R.id.tv_author)
        addOnClickListener(R.id.tv_tag)
        addOnClickListener(R.id.tv_chapter_name)
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
            binding.tvTitle.text = fromHtml(item.title)
            if (TextUtils.isEmpty(item.desc)) {
                binding.tvTitle.isSingleLine = false
                binding.tvDesc.visibility = View.GONE
            } else {
                binding.tvTitle.isSingleLine = true
                var desc = fromHtml(item.desc).toString()
                desc = removeAllBank(desc, 2)
                binding.tvDesc.text = desc
                binding.tvDesc.visibility = View.VISIBLE
            }
            binding.tvChapterName.text = fromHtml(
                formatChapterName(item.superChapterName, item.chapterName)
            )
            val activity: RouterActivity = contextToActivity(binding.root.context)
            if (item.collect) {
                binding.ivCollect.setImageResource(R.drawable.ic_collect_checked)
                binding.ivCollect.setOnClickListener {
                    unCollect(item.id).observe(activity, { result ->
                        if (result.errorCode == "0") {
                            item.collect = false
                            binding.ivCollect.setImageResource(R.drawable.ic_collect_unchecked_stroke)
                        } else if (result.errorCode.isNotBlank() && result.errorMsg.isNotBlank()) {
                            activity.showTips(result.errorMsg)
                        }
                    })
                }
            } else {
                binding.ivCollect.setImageResource(R.drawable.ic_collect_unchecked_stroke)
                binding.ivCollect.setOnClickListener {
                    collect(item.id).observe(activity, { result ->
                        if (result.errorCode == "0") {
                            item.collect = true
                            binding.ivCollect.setImageResource(R.drawable.ic_collect_checked)
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

    fun setBannerData(data: List<BannerBean>) {
        bannerData.addAll(data)
        bannerAdapter.setNewData(bannerData)
        notifyItemChanged(0)
    }

    fun urlToSystemList(activity: RouterActivity, url: String, callback: (TreeBean) -> Unit) {
        try {
            val uri = Uri.parse("https://www.wanandroid.com/$url")
            var chapterId = uri.getQueryParameter("cid")
            if (chapterId.isNullOrBlank()) {
                val paths = uri.pathSegments
                if (paths != null && paths.size >= 3) {
                    chapterId = paths[2]
                }
            }
            if (chapterId != null) {
                WanHelper.getTreeList().observe(activity) { list ->
                    list.forEach { treeBean ->
                        treeBean.children?.forEachIndexed { index, childrenTreeBean ->
                            if (childrenTreeBean.id == chapterId) {
                                treeBean.childrenSelectPosition = index
                                callback.invoke(treeBean)
                                return@forEach
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun chapterIdToSystemList(
        activity: RouterActivity,
        item: ArticleBean,
        callback: (TreeBean) -> Unit
    ) {
        WanHelper.getTreeList().observe(activity) { list ->
            list.forEach { treeBean ->
                if (treeBean.id == item.realSuperChapterId) {
                    treeBean.children?.forEachIndexed { index, childrenTreeBean ->
                        if (childrenTreeBean.id == item.chapterId) {
                            treeBean.childrenSelectPosition = index
                        }
                    }
                    callback.invoke(treeBean)
                    return@forEach
                }
            }
        }
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