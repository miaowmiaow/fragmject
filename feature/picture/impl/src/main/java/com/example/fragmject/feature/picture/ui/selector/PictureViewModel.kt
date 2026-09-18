package com.example.fragmject.feature.picture.ui.selector

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import com.example.fragmject.core.domain.repository.AlbumRepository
import com.example.fragmject.core.domain.repository.MediaRepository
import com.example.fragmject.feature.picture.model.AlbumBean
import com.example.fragmject.feature.picture.model.MediaBean
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.net.toUri

@HiltViewModel
class PictureViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val albumRepository: AlbumRepository,
) : BaseViewModel() {

    private val mediaMap = HashMap<String, MutableList<MediaBean>>()

    // 相册数据
    private val _albumResult = MutableStateFlow<List<AlbumBean>>(emptyList())
    val albumResult: StateFlow<List<AlbumBean>> = _albumResult.asStateFlow()
    private val _currAlbumResult = MutableStateFlow<List<MediaBean>>(emptyList())
    val currAlbumResult: StateFlow<List<MediaBean>> = _currAlbumResult.asStateFlow()

    // 选择状态（Selector 与 Preview 共享同一份，用 Uri 字符串绑定，避免索引错位）
    private val _selectedUris = MutableStateFlow<List<String>>(emptyList())
    val selectedUris: StateFlow<List<String>> = _selectedUris.asStateFlow()
    private val _selectedUriSet = MutableStateFlow<Set<String>>(emptySet())
    val selectedUriSet: StateFlow<Set<String>> = _selectedUriSet.asStateFlow()

    // UI 状态
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

    /** 切换指定 Uri 的选中状态；最多 [maxSelection] 个。 */
    fun toggleSelection(uri: String, maxSelection: Int = 9) {
        if (uri in _selectedUriSet.value) {
            _selectedUris.value -= uri
            _selectedUriSet.value -= uri
        } else if (_selectedUris.value.size < maxSelection) {
            _selectedUris.value += uri
            _selectedUriSet.value += uri
        }
    }

    fun clearSelection() {
        _selectedUris.value = emptyList()
        _selectedUriSet.value = emptySet()
    }

    /** 由 Preview 进入时同步选择状态（与 Selector 保持一致）。 */
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

    /** 拍照前预创建 MediaStore Uri，并缓存供回调消费。失败返回 null。 */
    fun createTakePictureUri(): Uri? {
        val uriString = mediaRepository.createImageUri()
        if (uriString.isEmpty()) return null
        val uri = uriString.toUri()
        _takePictureUri.value = uri
        return uri
    }

    /** 拍照成功后清除 pending 状态，使照片对系统相册与查询可见。 */
    fun finishTakePictureUri() {
        val uri = _takePictureUri.value ?: return
        mediaRepository.finishImageUri(uri.toString())
        _takePictureUri.value = null
    }

    /** 删除拍照前预创建但未写入数据的图片记录，避免残留透明图。 */
    fun deleteTakePictureUri() {
        val uri = _takePictureUri.value ?: return
        mediaRepository.deleteImageUri(uri.toString())
        _takePictureUri.value = null
    }

    fun updateMediaUri(oldUri: Uri, newUri: Uri) {
        val oldStr = oldUri.toString()
        val newStr = newUri.toString()
        // 同步更新 mediaMap：所有相册中匹配 oldUri 的项替换为 newUri，
        // 否则切换相册后会重新出现旧图，导致选中项与新图脱节
        mediaMap.forEach { (_, list) ->
            val idx = list.indexOfFirst { it.uri.toString() == oldStr }
            if (idx >= 0) {
                list[idx] = list[idx].clone().apply { uri = newUri }
            }
        }
        // 同步更新当前相册结果
        val list = _currAlbumResult.value.toMutableList()
        val index = list.indexOfFirst { it.uri.toString() == oldStr }
        if (index >= 0) {
            list[index] = list[index].clone().apply { uri = newUri }
            _currAlbumResult.value = list
        }
        // 同步更新选择状态：把选中的旧 Uri 替换为新 Uri，保持选中项不变
        _selectedUris.value = _selectedUris.value.map { if (it == oldStr) newStr else it }
        _selectedUriSet.value = _selectedUriSet.value.map { if (it == oldStr) newStr else it }.toSet()
    }

    /** 删除指定 Uri 对应的媒体记录（编辑另存为新图后清理旧图，避免相册重复）。 */
    fun deleteMedia(uri: Uri) {
        mediaRepository.deleteImageUri(uri.toString())
    }

    fun updateCurrAlbum(name: String) {
        _currAlbumName.value = name
        _currAlbumResult.value = mediaMap[name] ?: emptyList()
    }

    /**
     * 获取相册资源
     */
    fun queryAlbum() {
        viewModelScope.launch {
            try {
                val groups = albumRepository.queryAlbums()
                mediaMap.clear()
                val albumData = mutableListOf<AlbumBean>()
                var totalCount = 0
                groups.forEach { group ->
                    val beans = group.images.map { image ->
                        MediaBean(
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
                        AlbumBean(group.name, group.coverUri.toUri(), beans.size.toString())
                    )
                }
                // 无权限时 queryAlbums 会返回仅含空「所有照片」组的列表（非空），
                // 导致权限提示条件 albumResult.isEmpty() 不成立。这里识别该情况并清空结果。
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
                Log.e("PictureViewModel", "loadAlbum failed", e)
            } finally {
                _queryAttempted.value = true
            }
        }
    }

}