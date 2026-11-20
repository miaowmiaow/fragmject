package com.example.fragmject.feature.picture.state

import com.example.fragmject.core.domain.repository.PictureEditRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 「旧图 uri → 编辑后新图 uri」解析器。
 *
 * 为什么需要它：编辑保存后原图会被删除，但预览页的路由参数里始终是**编辑前**的 uri
 * （返回栈由 Navigation 3 在进程重建后原样恢复）。若只把替换关系放在进程内，
 * 进程重建后预览就会去解码一张已被删除的图，表现为空白 / 加载失败。
 *
 * 因此替换关系以 [PictureEditRepository] 为准（持久），本类只做两件事：
 * 1. 记住本进程内刚产生的替换（[remember]），避免解析时再查一次库；
 * 2. 解析时沿链走到最终 uri，兼容「同一张图被连续编辑多次」（A→B→C）。
 *
 * [MAX_HOPS] 仅用于防御异常数据形成的环。
 *
 * 两个缓存都是普通集合：调用方（[PictureFlowState]）只在主线程访问本类，
 * 若将来引入后台写入，必须先换成并发容器。
 */
@Singleton
class EditedUriResolver @Inject constructor(
    private val pictureEditRepository: PictureEditRepository,
) {

    /** 进程内缓存；进程重建后为空，[lookup] 会自动回落到持久记录。 */
    private val recentReplacements = HashMap<String, String>()

    /**
     * 已确认「没有替换记录」的 uri。
     *
     * 链尾（自然也是未被编辑的图）必然查不到记录，不记住就会在每次进预览时
     * 为每张这样的图各打一次库。本进程的写入只发生在 [remember]，负缓存不会失效。
     */
    private val unreplacedUris = HashSet<String>()

    /** 记录本次进程内刚产生的替换（持久化由 [persist] 负责，调用顺序见 PictureFlowState）。 */
    fun remember(oldUri: String, newUri: String) {
        recentReplacements[oldUri] = newUri
        // 同一条 uri 可能先被判定「无替换」，随后才被编辑
        unreplacedUris -= oldUri
    }

    /** 落库：必须在删除原图**之前**调用，否则两者之间的进程退出会丢掉替换关系。 */
    suspend fun persist(oldUri: String, newUri: String) {
        pictureEditRepository.recordReplacement(oldUri, newUri)
    }

    /** 解析出 [rawUri] 最终应展示的 uri；没有替换记录时原样返回。 */
    suspend fun resolve(rawUri: String): String {
        var current = rawUri
        repeat(MAX_HOPS) {
            val next = lookup(current) ?: return current
            if (next == current) return current
            current = next
        }
        return current
    }

    /** 内存优先；未命中时查一次持久记录，并记住结果，避免每次进预览都重复查库。 */
    private suspend fun lookup(uri: String): String? {
        recentReplacements[uri]?.let { return it }
        if (uri in unreplacedUris) return null
        val persisted = pictureEditRepository.findReplacement(uri)
        if (persisted == null) {
            unreplacedUris += uri
        } else {
            recentReplacements[uri] = persisted
        }
        return persisted
    }

    private companion object {
        const val MAX_HOPS = 8
    }
}
