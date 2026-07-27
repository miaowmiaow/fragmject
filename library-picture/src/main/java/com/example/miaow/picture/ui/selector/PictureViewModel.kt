package com.example.miaow.picture.ui.selector

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.miaow.base.vm.BaseViewModel
import com.example.miaow.picture.data.AlbumBean
import com.example.miaow.picture.data.MediaBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PictureViewModel : BaseViewModel() {

    companion object {
        private const val DEFAULT_BUCKET_NAME = "所有照片"
        private const val ID = MediaStore.Images.ImageColumns._ID
        private const val BUCKET_DISPLAY_NAME = MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME
        private const val DATE_MODIFIED = MediaStore.Images.ImageColumns.DATE_MODIFIED
        private const val DISPLAY_NAME = MediaStore.Images.ImageColumns.DISPLAY_NAME
        private const val HEIGHT = MediaStore.Images.ImageColumns.HEIGHT
        private const val MIME_TYPE = MediaStore.Images.ImageColumns.MIME_TYPE
        private const val WIDTH = MediaStore.Images.ImageColumns.WIDTH
    }

    private val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    private val projection = arrayOf(
        ID, BUCKET_DISPLAY_NAME, DATE_MODIFIED, DISPLAY_NAME, HEIGHT, MIME_TYPE, WIDTH
    )
    private val sortOrder = "$DATE_MODIFIED DESC"

    private val mediaMap = HashMap<String, MutableList<MediaBean>>()
    private val _albumResult = MutableStateFlow<List<AlbumBean>>(emptyList())
    val albumResult: StateFlow<List<AlbumBean>> = _albumResult.asStateFlow()
    private val _currAlbumResult = MutableStateFlow<List<MediaBean>>(emptyList())
    val currAlbumResult: StateFlow<List<MediaBean>> = _currAlbumResult.asStateFlow()

    fun updateMediaMap(bean: MediaBean) {
        mediaMap.forEach {
            it.value.add(0, bean)
        }
    }

    fun updateMediaUri(oldUri: Uri, newUri: Uri) {
        val list = _currAlbumResult.value.toMutableList()
        val index = list.indexOfFirst { it.uri == oldUri }
        if (index >= 0) {
            list[index] = list[index].clone().apply { uri = newUri }
            _currAlbumResult.value = list
        }
    }

    fun updateCurrAlbum(name: String) {
        _currAlbumResult.value = mediaMap[name] ?: emptyList()
    }

    /**
     * 获取相册资源
     */
    fun queryAlbum(context: Context) {
        viewModelScope.launch {
            try {
                mediaMap.clear()
                mediaMap[DEFAULT_BUCKET_NAME] = ArrayList()
                val cursor = context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    sortOrder
                ) ?: throw Exception("Query could not be executed")
                cursor.use {
                    while (cursor.moveToNext()) {
                        val idIndex = cursor.getColumnIndex(ID)
                        val id = cursor.getLong(idIndex)
                        val contentUri: Uri = ContentUris.withAppendedId(uri, id)
                        val bucketNameIndex = cursor.getColumnIndex(BUCKET_DISPLAY_NAME)
                        val bucketName = cursor.getString(bucketNameIndex)
                        val nameIndex = cursor.getColumnIndex(DISPLAY_NAME)
                        val name = cursor.getString(nameIndex)
                        val mimeTypeIndex = cursor.getColumnIndex(MIME_TYPE)
                        val mimeType = cursor.getString(mimeTypeIndex)
                        val widthIndex = cursor.getColumnIndex(WIDTH)
                        val width = cursor.getInt(widthIndex)
                        val heightIndex = cursor.getColumnIndex(HEIGHT)
                        val height = cursor.getInt(heightIndex)
                        val media = MediaBean(name, contentUri, width, height, mimeType)
                        if (bucketName != null) {
                            if (!mediaMap.containsKey(bucketName)) {
                                mediaMap[bucketName] = ArrayList()
                            }
                            mediaMap[bucketName]?.add(media)
                        }
                        mediaMap[DEFAULT_BUCKET_NAME]?.add(media)
                    }
                }
                val albumData: MutableList<AlbumBean> = ArrayList()
                mediaMap.onEach { (key, value) ->
                    val album = AlbumBean(key, value[value.size - 1].uri, value.size.toString())
                    if (key == DEFAULT_BUCKET_NAME)
                        albumData.add(0, album)
                    else albumData.add(album)
                }
                _albumResult.value = albumData
                _currAlbumResult.value = mediaMap[albumData[0].name] ?: emptyList()
            } catch (e: Exception) {
                Log.e("PictureViewModel", "loadAlbum failed", e)
            }
        }
    }

}