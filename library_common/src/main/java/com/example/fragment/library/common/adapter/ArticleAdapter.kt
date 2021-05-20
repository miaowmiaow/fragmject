package com.example.fragment.library.common.adapter

import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.component.adapter.BaseAdapter
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.utils.ImageLoader
import com.example.fragment.library.base.utils.SimpleBannerHelper
import com.example.fragment.library.common.R
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.bean.BannerBean
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.databinding.ItemArticleBannerBinding
import com.example.fragment.library.common.databinding.ItemArticleBinding
import com.example.fragment.library.common.utils.StringUtils
import com.example.fragment.library.common.utils.WanHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ArticleAdapter : BaseAdapter<ArticleBean>() {

    companion object {
        private const val ITEM_TYPE_BANNER = 0
        private const val ITEM_TYPE_ARTICLE = 1
    }

    private var collectJob: Job? = null
    private var unCollectJob: Job? = null

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
                binding.tvTag.tag = item.tags[0].url
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
                binding.tvTitle.isSingleLine = false
                binding.tvDesc.visibility = View.GONE
            } else {
                binding.tvTitle.isSingleLine = true
                var desc = Html.fromHtml(item.desc).toString()
                desc = StringUtils.removeAllBank(desc, 2)
                binding.tvDesc.text = desc
                binding.tvDesc.visibility = View.VISIBLE
            }
            binding.tvChapterName.text = Html.fromHtml(
                formatChapterName(item.superChapterName, item.chapterName)
            )
            val activity: RouterActivity = contextToActivity(binding.root.context)
            if (item.collect) {
                binding.ivCollect.setImageResource(R.drawable.ic_collect_checked)
            } else {
                binding.ivCollect.setImageResource(R.drawable.ic_collect_unchecked_stroke)
            }
            binding.ivCollect.setOnClickListener {
                if (item.collect) {
                    unCollect(item.id).observe(activity, { result ->
                        if (result.errorCode == "0") {
                            binding.ivCollect.setImageResource(R.drawable.ic_collect_unchecked_stroke)
                            item.collect = false
                        } else if (result.errorCode == "-1001") {
                            WanHelper.setUser(UserBean())
                            activity.navigation(Router.LOGIN)
                        }
                        if (result.errorMsg.isNotBlank()) {
                            activity.showTips(result.errorMsg)
                        }
                    })
                } else {
                    collect(item.id).observe(activity, { result ->
                        if (result.errorCode == "0") {
                            binding.ivCollect.setImageResource(R.drawable.ic_collect_checked)
                            item.collect = true
                        } else if (result.errorCode == "-1001") {
                            WanHelper.setUser(UserBean())
                            activity.navigation(Router.LOGIN)
                        }
                        if (result.errorMsg.isNotBlank()) {
                            activity.showTips(result.errorMsg)
                        }
                    })
                }
            }
            binding.root.setOnClickListener {
                val args = Bundle()
                args.putString(Keys.URL, item.link)
                activity.navigation(Router.WEB, args)
            }
            binding.tvTag.setOnClickListener {
                urlToSystemList(activity, binding.tvTag.tag.toString())
            }
            binding.tvChapterName.setOnClickListener {
                chapterIdToSystemList(activity, item.realSuperChapterId, item.chapterId)
            }
            binding.tvAuthor.setOnClickListener {
                val args = Bundle()
                args.putString(Keys.ID, item.userId)
                activity.navigation(Router.USER_SHARE, args)
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
        collectJob?.cancel()
        unCollectJob?.cancel()
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

    private fun urlToSystemList(activity: RouterActivity, url: String) {
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
                WanHelper.getTreeList().observe(activity, { list ->
                    list.forEach { treeBean ->
                        treeBean.children?.forEachIndexed { index, childrenTreeBean ->
                            if (childrenTreeBean.id == chapterId) {
                                treeBean.childrenSelectPosition = index
                                val args = Bundle()
                                args.putParcelable(Keys.BEAN, treeBean)
                                activity.navigation(Router.SYSTEM_LIST, args)
                                return@forEach
                            }
                        }
                    }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun chapterIdToSystemList(
        activity: RouterActivity,
        realSuperChapterId: String,
        chapterId: String
    ) {
        WanHelper.getTreeList().observe(activity, { list ->
            list.forEach { treeBean ->
                if (treeBean.id == realSuperChapterId) {
                    treeBean.children?.forEachIndexed { index, childrenTreeBean ->
                        if (childrenTreeBean.id == chapterId) {
                            treeBean.childrenSelectPosition = index
                        }
                    }
                    val args = Bundle()
                    args.putParcelable(Keys.BEAN, treeBean)
                    activity.navigation(Router.SYSTEM_LIST, args)
                    return@forEach
                }
            }
        })
    }

    private fun collect(id: String): MutableLiveData<HttpResponse> {
        val result = MutableLiveData<HttpResponse>()
        collectJob = CoroutineScope(Dispatchers.Main).launch {
            result.postValue(post(HttpRequest("lg/collect/{id}/json").putPath("id", id)))
        }
        return result
    }

    private fun unCollect(id: String): MutableLiveData<HttpResponse> {
        val result = MutableLiveData<HttpResponse>()
        unCollectJob = CoroutineScope(Dispatchers.Main).launch {
            val request = HttpRequest("lg/uncollect_originId/{id}/json")
            request.putPath("id", id)
            result.postValue(post(request))
        }
        return result
    }
}