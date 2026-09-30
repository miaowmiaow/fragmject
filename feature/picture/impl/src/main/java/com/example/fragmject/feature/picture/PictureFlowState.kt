package com.example.fragmject.feature.picture

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.example.fragmject.core.domain.repository.AlbumRepository
import com.example.fragmject.core.domain.repository.MediaRepository
import com.example.fragmject.core.navigation.runtime.NavFlowScope
import com.example.fragmject.feature.picture.model.Album
import com.example.fragmject.feature.picture.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 图片选择/预览/编辑流程共享状态（一次图片流程对应一个实例）。
 *
 * 实现 [NavFlowScope]：由 [PictureFlowScopeContributor] 创建，app 组合根在图片流程
 * 全部退出时调用 [close] 销毁；查询挂靠独立 [flowScope]，随 [close] 取消，
 * 重复查询通过 [queryJob] 取消 + [queryVersion] 校验丢弃过期结果。
 */
class PictureFlowState(
    private val mediaRepository: MediaRepository,
    private val albumRepository: AlbumRepository,
) : NavFlowScope {

    private val mediaMap = HashMap<String, MutableList<MediaItem>>()

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
    private val _takePictureUri = MutableStateFlow<Uri?>(null)
    val takePictureUri: StateFlow<Uri?> = _takePictureUri.asStateFlow()

    fun toggleSelection(uri: String, maxSelection: Int = 9) {
        if (uri in _selectedUriSet.value) {
            _selectedUris.value -= uri
            _selectedUriSet.value -= uri
        } else if (_selectedUris.value.size < maxSelection) {
            _selectedUris.value += uri
            _selectedUriSet.value += uri
        }
    }

    fun initSelection(uris: List<String>) {
        _selectedUris.value = uris
        _selectedUriSet.value = uris.toSet()
    }

    fun setAlbumMenuExpanded(expanded: Boolean) {
        _albumMenuExpanded.value = expanded
    }

    fun setHasPermission(granted: Boolean) {
        _hasPermission.value = granted
    }

    fun createTakePictureUri(): Uri? {
        val uriString = mediaRepository.createImageUri()
        if (uriString.isEmpty()) return null
        val uri = uriString.toUri()
        _takePictureUri.value = uri
        return uri
    }

    fun finishTakePictureUri() {
        val uri = _takePictureUri.value ?: return
        mediaRepository.finishImageUri(uri.toString())
        _takePictureUri.value = null
    }

    fun deleteTakePictureUri() {
        val uri = _takePictureUri.value ?: return
        mediaRepository.deleteImageUri(uri.toString())
        _takePictureUri.value = null
    }

    fun updateMediaUri(oldUri: Uri, newUri: Uri) {
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
    }

    fun deleteMedia(uri: Uri) {
        mediaRepository.deleteImageUri(uri.toString())
    }

    fun updateCurrAlbum(name: String) {
        _currAlbumName.value = name
        _currAlbumResult.value = mediaMap[name] ?: emptyList()
    }

    fun queryAlbum() {
        queryJob?.cancel()
        val version = ++queryVersion
        queryJob = flowScope.launch {
            try {
                val groups = albumRepository.queryAlbums()
                if (version != queryVersion) return@launch
                mediaMap.clear()
                val albumData = mutableListOf<Album>()
                var totalCount = 0
                groups.forEach { group ->
                    val beans = group.images.map { image ->
                        MediaItem(
                            name = image.name,
                            uri = image.uri.toUri(),
                            width = image.width,
                            height = image.height,
                            mimeType = image.mimeType,
                        )
                    }
                    totalCount += beans.size
                    mediaMap[group.name] = beans.toMutableList()
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
            } catch (e: Exception) {
                Log.e("PictureFlowState", "loadAlbum failed", e)
            } finally {
                if (version == queryVersion) {
                    _queryAttempted.value = true
                }
            }
        }
    }

    override fun close() {
        flowScope.cancel()
        queryJob?.cancel()
        queryJob = null
    }
}
