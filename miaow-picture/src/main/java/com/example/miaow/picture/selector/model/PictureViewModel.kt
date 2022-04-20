package com.example.miaow.picture.selector.model

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.model.BaseViewModel
import com.example.miaow.picture.selector.bean.AlbumBean
import com.example.miaow.picture.selector.bean.MediaBean
import kotlinx.coroutines.launch

class PictureViewModel : BaseViewModel() {

    companion object {
        private const val ID = MediaStore.Files.FileColumns._ID
        private const val BUCKET_DISPLAY_NAME = "bucket_display_name"
        private const val DISPLAY_NAME = MediaStore.Files.FileColumns.DISPLAY_NAME
        private const val MEDIA_TYPE = MediaStore.Files.FileColumns.MEDIA_TYPE
        private const val MEDIA_TYPE_IMAGE = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
        private const val MEDIA_TYPE_VIDEO = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
    }

    private val uri = MediaStore.Files.getContentUri("external")
    private val projection = arrayOf(ID, BUCKET_DISPLAY_NAME, DISPLAY_NAME, MEDIA_TYPE)
    private val sortOrder = MediaStore.Files.FileColumns.DATE_ADDED + " DESC"

    val mediaResult = MutableLiveData<Map<String, List<MediaBean>>>()
    val albumResult = MutableLiveData<List<AlbumBean>>()
    val currAlbumResult = MutableLiveData<List<MediaBean>>()

    fun updateCurrAlbum(name: String) {
        mediaResult.value?.let {
            currAlbumResult.postValue(it[name])
        }
    }

    /**
     * 获取相册资源
     */
    fun queryAlbum(context: Context) {
        viewModelScope.launch {
            val mediaData = HashMap<String, MutableList<MediaBean>>().apply {
                this["最近项目"] = ArrayList()
            }
            context.contentResolver.query(uri, projection, null, null, sortOrder)?.apply {
                while (moveToNext()) {
                    val mediaType = getInt(getColumnIndex(MEDIA_TYPE))
                    if (mediaType == MEDIA_TYPE_IMAGE || mediaType == MEDIA_TYPE_VIDEO) {
                        val id = getLong(getColumnIndex(ID))
                        val contentUri = ContentUris.withAppendedId(uri, id)
                        val bucketName = getString(getColumnIndex(BUCKET_DISPLAY_NAME))
                        val name = getString(getColumnIndex(DISPLAY_NAME))
                        val media = MediaBean(name, contentUri)
                        if (!mediaData.containsKey(bucketName)) {
                            mediaData[bucketName] = ArrayList()
                        }
                        mediaData[bucketName]?.add(media)
                        mediaData["最近项目"]?.add(media)
                    }
                }
                close()
            }
            val albumData: MutableList<AlbumBean> = ArrayList()
            mediaData.onEach { (key, value) ->
                val album = AlbumBean(key, value[value.size - 1].uri, value.size.toString())
                if (key == "最近项目") albumData.add(0, album) else albumData.add(album)
            }
            mediaResult.postValue(mediaData)
            albumResult.postValue(albumData)
            currAlbumResult.postValue(mediaData[albumData[0].name])
        }
    }

}
