package com.example.fragmject.feature.picture.state

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.example.fragmject.core.domain.repository.AlbumRepository
import com.example.fragmject.core.domain.repository.MediaRepository
import com.example.fragmject.core.domain.repository.PictureSelectionRepository
import com.example.fragmject.feature.picture.model.Album
import com.example.fragmject.feature.picture.model.MediaItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 图片选择/预览/编辑流程共享状态（进程级单例）。
 *
 * 降级自 navigation-runtime 的通用流程作用域机制（NavFlowScope 三件套）：项目仅 picture
 * 一个流程，通用抽象属过度设计，故收敛为 Hilt @Singleton 由 picture 内部持有。跨流程的
 * 状态残留由 [reset] 在进入选择器时归零；查询挂靠独立 [flowScope]（进程级），
 * 重复查询通过 [queryJob] 取消 + [queryVersion] 校验丢弃过期结果。
 */
@Singleton
class PictureFlowState @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val albumRepository: AlbumRepository,
    private val editedUriResolver: EditedUriResolver,
    private val selectionRepository: PictureSelectionRepository,
) {

    // 两者只在 Main.immediate 上访问（flowScope 为 Main.immediate，对外方法也在主线程调用），
    // 因此用普通 HashMap 即可；若将来引入后台写入，必须先换成 ConcurrentHashMap。
    private val mediaMap = HashMap<String, MutableList<MediaItem>>()

    /**
     * 与 [mediaMap] 同步维护的 uri 索引：避免 [startPreview] 在主线程做
     * `values.flatten()` + `all.find {}` 的 O(N×M) 查找（5000 图 × 9 张选中 ≈ 45000 次比较）。
     * 所有 [mediaMap] 的写入/清空处都必须同步维护本索引。
     */
    private val mediaIndex = HashMap<String, MediaItem>()

    private val flowScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var queryJob: Job? = null
    private var queryVersion = 0

    private val _albumResult = MutableStateFlow<List<Album>>(emptyList())
    val albumResult: StateFlow<List<Album>> = _albumResult.asStateFlow()
    private val _currAlbumResult = MutableStateFlow<List<MediaItem>>(emptyList())
    val currAlbumResult: StateFlow<List<MediaItem>> = _currAlbumResult.asStateFlow()

    private val _selectedUris = MutableStateFlow<List<String>>(emptyList())
    val selectedUris: StateFlow<List<String>> = _selectedUris.asStateFlow()
    private val _selectedUriSet = MutableStateFlow<Set<String>>(emptySet())
    val selectedUriSet: StateFlow<Set<String>> = _selectedUriSet.asStateFlow()

    private val _currAlbumName = MutableStateFlow("")
    val currAlbumName: StateFlow<String> = _currAlbumName.asStateFlow()
    private val _albumMenuExpanded = MutableStateFlow(false)
    val albumMenuExpanded: StateFlow<Boolean> = _albumMenuExpanded.asStateFlow()
    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()
    private val _queryAttempted = MutableStateFlow(false)
    val queryAttempted: StateFlow<Boolean> = _queryAttempted.asStateFlow()
    private val _queryError = MutableStateFlow(false)
    val queryError: StateFlow<Boolean> = _queryError.asStateFlow()
    private val _takePictureUri = MutableStateFlow<Uri?>(null)
    val takePictureUri: StateFlow<Uri?> = _takePictureUri.asStateFlow()

    /** 预览快照：进入预览时由 [startPreview] 一次性解析，预览页只读此快照，不随相册切换漂移。 */
    private val _previewSource = MutableStateFlow<List<MediaItem>>(emptyList())
    val previewSource: StateFlow<List<MediaItem>> = _previewSource.asStateFlow()

    fun toggleSelection(uri: String, maxSelection: Int = 9) {
        if (uri in _selectedUriSet.value) {
            _selectedUris.value -= uri
            _selectedUriSet.value -= uri
        } else if (_selectedUris.value.size < maxSelection) {
            _selectedUris.value += uri
            _selectedUriSet.value += uri
        } else {
            return
        }
        persistSelection()
    }

    /**
     * 从持久记录恢复选中态。
     *
     * 只在内存为空时生效：进程内返回/再次进入时内存里已经是更新的状态，不能覆盖。
     * 调用点见 `PicturePreviewNavKey` 的渲染（返回栈重建）与选择器的
     * `SelectorPermissionEffect`（重建后回到选择器）。
     */
    suspend fun restorePersistedSelection() {
        if (_selectedUris.value.isNotEmpty()) return
        val persisted = selectionRepository.loadSelectedUris()
        if (persisted.isEmpty()) return
        _selectedUris.value = persisted
        _selectedUriSet.value = persisted.toSet()
    }

    /**
     * 选中态落库。
     *
     * 选中列表是纯 UI 状态，但预览页返回栈在进程重建后会恢复：不落库就会退回「未选中」，
     * 预览底部按钮从「取消选择」变回「选择」、缩略图条没有勾选标记，
     * 且「完成」会把空列表报给调用方。
     */
    private fun persistSelection() {
        val snapshot = _selectedUris.value
        flowScope.launch { selectionRepository.saveSelectedUris(snapshot) }
    }

    fun setAlbumMenuExpanded(expanded: Boolean) {
        _albumMenuExpanded.value = expanded
    }

    fun setHasPermission(granted: Boolean) {
        _hasPermission.value = granted
    }

    suspend fun createTakePictureUri(): Uri? {
        val uriString = mediaRepository.createImageUri()
        if (uriString.isEmpty()) return null
        val uri = uriString.toUri()
        _takePictureUri.value = uri
        return uri
    }

    /**
     * 拍照成功：清除 pending 状态，让照片对系统相册与查询可见。
     *
     * @return 是否发布成功。失败时**立即删除**该记录：pending 记录在相册里不可见，
     * 而 uri 引用马上要被清空，不在这里删就再也没有机会清理，只会永久残留。
     */
    suspend fun finishTakePictureUri(): Boolean {
        val uri = _takePictureUri.value ?: return false
        val published = mediaRepository.finishImageUri(uri.toString())
        if (!published) {
            Log.e("PictureFlowState", "finishTakePictureUri failed, drop pending record: $uri")
            mediaRepository.deleteImageUri(uri.toString())
        }
        _takePictureUri.value = null
        return published
    }

    suspend fun deleteTakePictureUri() {
        val uri = _takePictureUri.value ?: return
        mediaRepository.deleteImageUri(uri.toString())
        _takePictureUri.value = null
    }

    /**
     * 编辑保存成功后的收尾：更新内存态 → 持久化替换关系 → 删除原图。
     *
     * 三步顺序不可调换：替换关系必须先落库再删原图。反过来的话，进程若在两者之间被杀，
     * 重启后返回栈仍带原始 uri、替换关系却已随进程丢失，预览就会指向一张已删除的原图。
     *
     * 落库与删除都挂在 [flowScope]：保存成功后编辑器页随即出栈，`rememberCoroutineScope`
     * 随之取消，删除会被中断、原图残留。
     */
    fun applyEditResult(oldUri: Uri, newUri: Uri) {
        // 内存态同步更新：预览页在 onFinish 后立刻可见，新图必须当场生效
        updateMediaUri(oldUri, newUri)
        flowScope.launch {
            // 先落库（进程重建后仍能解析）再删原图（删除不可逆）
            editedUriResolver.persist(oldUri.toString(), newUri.toString())
            deleteMedia(oldUri)
        }
    }

    private fun updateMediaUri(oldUri: Uri, newUri: Uri) {
        val oldStr = oldUri.toString()
        val newStr = newUri.toString()
        mediaMap.forEach { (_, list) ->
            val idx = list.indexOfFirst { it.uri.toString() == oldStr }
            if (idx >= 0) {
                list[idx] = list[idx].clone().apply { uri = newUri }
            }
        }
        val list = _currAlbumResult.value.toMutableList()
        val index = list.indexOfFirst { it.uri.toString() == oldStr }
        if (index >= 0) {
            list[index] = list[index].clone().apply { uri = newUri }
            _currAlbumResult.value = list
        }
        _selectedUris.value = _selectedUris.value.map { if (it == oldStr) newStr else it }
        _selectedUriSet.value = _selectedUriSet.value.map { if (it == oldStr) newStr else it }.toSet()
        // 选中项也要跟着换成新 uri，否则重建后勾选标记落在已删除的原图上
        persistSelection()
        // 记录替换关系：预览页若因返回栈重建而再次 startPreview，也能解析到新图
        editedUriResolver.remember(oldStr, newStr)
        // 同步更新预览快照，使编辑保存后返回预览页仍显示新图
        _previewSource.value = _previewSource.value.map { item ->
            if (item.uri.toString() == oldStr) item.clone().apply { uri = newUri } else item
        }
    }

    private suspend fun deleteMedia(uri: Uri) {
        mediaRepository.deleteImageUri(uri.toString())
    }

    fun updateCurrAlbum(name: String) {
        _currAlbumName.value = name
        _currAlbumResult.value = mediaMap[name] ?: emptyList()
    }

    /**
     * 进入预览：按 [uris] 顺序解析出 [MediaItem] 快照，预览页只读此快照。
     *
     * - 命中本地媒体（选图流程）：直接取相册里的 MediaItem，跨相册也能正确命中；
     * - 未命中：按原样回退成一个只带 uri 的 MediaItem——**不要**换成空占位 uri。
     *   预览页不止用于本地相册，信息流的封面是网络 URL，回退成占位会显示一片空白。
     *   只有确实是空串时才用占位，避免 coil 拿到空 model。
     *
     * 解析前先沿替换链把原图 uri 换成编辑后的 uri：编辑保存会删掉原图，而返回栈里的
     * NavKey 始终保留原始 uri（进程重建后尤其如此），不替换就会去解码一张不存在的图。
     *
     * @param restoreSelection 选图流程态在**返回栈重建**（进程重建）后传 true：先按持久记录
     *   恢复选中态，避免底部按钮与缩略图勾选退回「未选中」。进程内返回时内存里是最新状态，
     *   传 false 不打扰它。
     */
    suspend fun startPreview(uris: List<String>, restoreSelection: Boolean = false) {
        if (restoreSelection) restorePersistedSelection()
        val resolved = ArrayList<MediaItem>(uris.size)
        // 走 uri 索引直查（O(M)），不再 flatten 全量后线性查找
        uris.forEachIndexed { index, rawUri ->
            val uriStr = editedUriResolver.resolve(rawUri)
            resolved += mediaIndex[uriStr]
                ?: MediaItem("", uriStr.takeIf { it.isNotBlank() }?.toUri() ?: "empty://preview/$index".toUri())
        }
        _previewSource.value = resolved
    }

    fun queryAlbum() {
        queryJob?.cancel()
        val version = ++queryVersion
        queryJob = flowScope.launch {
            try {
                val groups = albumRepository.queryAlbums()
                if (version != queryVersion) return@launch
                mediaMap.clear()
                mediaIndex.clear()
                // 映射放到 Default：flowScope 是 Main.immediate，而这里要为每张图做一次
                // Uri.parse + MediaItem 分配（5000 张时是 5000 次），留在主线程会明显卡顿
                val beansByGroup = withContext(Dispatchers.Default) {
                    groups.associate { group ->
                        group.name to group.images.map { image ->
                            MediaItem(
                                name = image.name,
                                uri = image.uri.toUri(),
                                width = image.width,
                                height = image.height,
                                mimeType = image.mimeType,
                            )
                        }
                    }
                }
                val albumData = mutableListOf<Album>()
                var totalCount = 0
                groups.forEach { group ->
                    val beans = beansByGroup[group.name].orEmpty()
                    totalCount += beans.size
                    mediaMap[group.name] = beans.toMutableList()
                    // 同步 uri 索引（见 [mediaIndex] 注释：写入点必须与 mediaMap 成对维护）
                    beans.forEach { item -> mediaIndex[item.uri.toString()] = item }
                    albumData.add(
                        Album(group.name, group.coverUri.toUri(), beans.size.toString())
                    )
                }
                if (totalCount == 0 && !_hasPermission.value) {
                    _albumResult.value = emptyList()
                    _currAlbumResult.value = emptyList()
                    _currAlbumName.value = ""
                } else {
                    _albumResult.value = albumData
                    val first = albumData.firstOrNull()
                    if (first != null) {
                        _currAlbumName.value = first.name
                        _currAlbumResult.value = mediaMap[first.name] ?: emptyList()
                    }
                }
                _queryError.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("PictureFlowState", "loadAlbum failed", e)
                _queryError.value = true
            } finally {
                if (version == queryVersion) {
                    _queryAttempted.value = true
                }
            }
        }
    }

    /** 进入图片流程时归零跨流程残留状态（等价于原「每次流程新建实例」的语义）。 */
    fun reset() {
        _albumResult.value = emptyList()
        _currAlbumResult.value = emptyList()
        _currAlbumName.value = ""
        _selectedUris.value = emptyList()
        _selectedUriSet.value = emptySet()
        _albumMenuExpanded.value = false
        _hasPermission.value = false
        _queryAttempted.value = false
        _queryError.value = false
        _takePictureUri.value = null
        _previewSource.value = emptyList()
        // 持久记录一并归零：否则上一次流程的选中态会在下次进程重建时被恢复出来
        flowScope.launch { selectionRepository.saveSelectedUris(emptyList()) }
    }
}
