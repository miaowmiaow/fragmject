package com.example.fragmject.core.data.impl.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.fragmject.core.domain.repository.AlbumGroup
import com.example.fragmject.core.domain.repository.AlbumImage
import com.example.fragmject.core.domain.repository.AlbumRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 相册查询 data 实现：通过 ContentResolver 查询 MediaStore，
 * 并按 bucket 分组，返回 domain 纯数据模型。
 */
@Singleton
class AlbumRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AlbumRepository {

    companion object {
        private const val DEFAULT_BUCKET_NAME = "所有照片"
        private const val ID = MediaStore.Images.ImageColumns._ID
        private const val BUCKET_DISPLAY_NAME = MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME
        private const val DATE_MODIFIED = MediaStore.Images.ImageColumns.DATE_MODIFIED
        private const val DISPLAY_NAME = MediaStore.Images.ImageColumns.DISPLAY_NAME
        private const val HEIGHT = MediaStore.Images.ImageColumns.HEIGHT
        private const val MIME_TYPE = MediaStore.Images.ImageColumns.MIME_TYPE
        private const val SIZE = MediaStore.Images.ImageColumns.SIZE
        private const val WIDTH = MediaStore.Images.ImageColumns.WIDTH
    }

    override suspend fun queryAlbums(): List<AlbumGroup> {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            ID, BUCKET_DISPLAY_NAME, DATE_MODIFIED, DISPLAY_NAME, HEIGHT, MIME_TYPE, SIZE, WIDTH,
        )
        val sortOrder = "$DATE_MODIFIED DESC"

        // LinkedHashMap 保持插入顺序，确保「所有照片」始终在第一位。
        val mediaMap = LinkedHashMap<String, MutableList<AlbumImage>>()
        mediaMap[DEFAULT_BUCKET_NAME] = ArrayList()

        val cursor = context.contentResolver.query(uri, projection, null, null, sortOrder)
            ?: return emptyList()
        cursor.use {
            while (cursor.moveToNext()) {
                val idIndex = cursor.getColumnIndex(ID)
                if (idIndex < 0) continue
                val id = cursor.getLong(idIndex)
                val contentUri: Uri = ContentUris.withAppendedId(uri, id)

                fun stringOr(column: String, fallback: String = ""): String {
                    val idx = cursor.getColumnIndex(column)
                    return if (idx >= 0) cursor.getString(idx) ?: fallback else fallback
                }

                fun intOr(column: String, fallback: Int = 0): Int {
                    val idx = cursor.getColumnIndex(column)
                    return if (idx >= 0) cursor.getInt(idx) else fallback
                }

                fun longOr(column: String, fallback: Long = 0L): Long {
                    val idx = cursor.getColumnIndex(column)
                    return if (idx >= 0) cursor.getLong(idx) else fallback
                }

                // 过滤掉未写入数据的空记录（如拍照预创建的 pending 记录），
                // 这类记录系统相册会忽略，app 侧也需保持一致，避免出现透明图
                val size = longOr(SIZE)
                if (size <= 0L) continue

                val bucketName = stringOr(BUCKET_DISPLAY_NAME)
                val name = stringOr(DISPLAY_NAME)
                val mimeType = stringOr(MIME_TYPE)
                val width = intOr(WIDTH)
                val height = intOr(HEIGHT)

                val image = AlbumImage(
                    name = name,
                    uri = contentUri.toString(),
                    width = width,
                    height = height,
                    mimeType = mimeType,
                )
                if (bucketName.isNotBlank()) {
                    mediaMap.getOrPut(bucketName) { ArrayList() }.add(image)
                }
                mediaMap.getValue(DEFAULT_BUCKET_NAME).add(image)
            }
        }

        return mediaMap.map { (key, value) ->
            AlbumGroup(
                name = key,
                coverUri = value.lastOrNull()?.uri ?: "",
                images = value.toList(),
            )
        }
    }
}
