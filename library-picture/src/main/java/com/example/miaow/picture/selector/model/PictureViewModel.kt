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
        private const val DEFAULT_BUCKET_NAME = "所有照片"
        private const val ID = MediaStore.Files.FileColumns._ID
        private const val BUCKET_DISPLAY_NAME = "bucket_display_name"
        private const val DATE_MODIFIED = MediaStore.Files.FileColumns.DATE_MODIFIED
        private const val DISPLAY_NAME = MediaStore.Files.FileColumns.DISPLAY_NAME
        private const val HEIGHT = MediaStore.Files.FileColumns.HEIGHT
        private const val MEDIA_TYPE = MediaStore.Files.FileColumns.MEDIA_TYPE
        private const val MEDIA_TYPE_IMAGE = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
        private const val MIME_TYPE = MediaStore.Files.FileColumns.MIME_TYPE
        private const val WIDTH = MediaStore.Files.FileColumns.WIDTH
    }

    private val uri = MediaStore.Files.getContentUri("external")
    private val projection = arrayOf(
        ID, BUCKET_DISPLAY_NAME, DATE_MODIFIED, DISPLAY_NAME, HEIGHT, MEDIA_TYPE, MIME_TYPE, WIDTH
    )
    private val sortOrder = "$DATE_MODIFIED DESC"

    private val mediaMap = HashMap<String, MutableList<MediaBean>>().apply {
        this[DEFAULT_BUCKET_NAME] = ArrayList()
    }
    val albumResult = MutableLiveData<List<AlbumBean>>()
    val currAlbumResult = MutableLiveData<List<MediaBean>>()

    fun updateMediaMap(bean: MediaBean) {
        mediaMap.forEach {
            it.value.add(0, bean)
        }
    }

    fun updateCurrAlbum(name: String) {
        currAlbumResult.postValue(mediaMap[name])
    }

    /**
     * 获取相册资源
     */
    fun queryAlbum(context: Context) {
        viewModelScope.launch {
            try {
                context.contentResolver.query(uri, projection, null, null, sortOrder)?.apply {
                    while (moveToNext()) {
                        val mediaTypeIndex = getColumnIndex(MEDIA_TYPE)
                        val mediaType = getInt(mediaTypeIndex)
                        if (mediaType == MEDIA_TYPE_IMAGE) {
                            val idIndex = getColumnIndex(ID)
                            val id = getLong(idIndex)
                            val contentUri = ContentUris.withAppendedId(uri, id)
                            val bucketNameIndex = getColumnIndex(BUCKET_DISPLAY_NAME)
                            val bucketName = getString(bucketNameIndex)
                            val nameIndex = getColumnIndex(DISPLAY_NAME)
                            val name = getString(nameIndex)
                            val mimeTypeIndex = getColumnIndex(MIME_TYPE)
                            val mimeType = getString(mimeTypeIndex)
                            val widthIndex = getColumnIndex(WIDTH)
                            val width = getInt(widthIndex)
                            val heightIndex = getColumnIndex(HEIGHT)
                            val height = getInt(heightIndex)
                            val media = MediaBean(name, contentUri, width, height, mimeType)
                            if(bucketName != null){
                                if (!mediaMap.containsKey(bucketName)) {
                                    mediaMap[bucketName] = ArrayList()
                                }
                                mediaMap[bucketName]?.add(media)
                            }
                            mediaMap[DEFAULT_BUCKET_NAME]?.add(media)
                        }
                    }
                    close()
                }
                val albumData: MutableList<AlbumBean> = ArrayList()
                mediaMap.onEach { (key, value) ->
                    val album = AlbumBean(key, value[value.size - 1].uri, value.size.toString())
                    if (key == DEFAULT_BUCKET_NAME)
                        albumData.add(0, album)
                    else albumData.add(album)
                }
                albumResult.postValue(albumData)
                currAlbumResult.postValue(mediaMap[albumData[0].name])
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
