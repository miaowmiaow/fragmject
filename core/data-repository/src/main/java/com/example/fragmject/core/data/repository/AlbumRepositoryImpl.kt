package com.example.fragmject.core.data.repository

import android.content.Context
import com.example.fragmject.core.android.platform.media.MediaStoreUtils
import com.example.fragmject.core.domain.repository.AlbumGroup
import com.example.fragmject.core.domain.repository.AlbumImage
import com.example.fragmject.core.domain.repository.AlbumRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 相册查询 data 实现：消费 [MediaStoreUtils.queryImages] 的平台行数据，
 * 按 bucket 分组并映射为 domain 纯数据模型，不直接触碰 MediaStore / ContentUris。
 */
@Singleton
class AlbumRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AlbumRepository {

    companion object {
        private const val DEFAULT_BUCKET_NAME = "所有照片"
    }

    override suspend fun queryAlbums(): List<AlbumGroup> = withContext(Dispatchers.IO) {
        val rows = MediaStoreUtils.queryImages(context)

        // LinkedHashMap 保持插入顺序，确保「所有照片」始终在第一位。
        val mediaMap = LinkedHashMap<String, MutableList<AlbumImage>>()
        mediaMap[DEFAULT_BUCKET_NAME] = ArrayList()

        rows.forEach { row ->
            val image = AlbumImage(
                name = row.displayName,
                uri = row.uri,
                width = row.width,
                height = row.height,
                mimeType = row.mimeType,
            )
            if (row.bucket.isNotBlank()) {
                mediaMap.getOrPut(row.bucket) { ArrayList() }.add(image)
            }
            mediaMap.getValue(DEFAULT_BUCKET_NAME).add(image)
        }

        mediaMap.map { (key, value) ->
            AlbumGroup(
                name = key,
                coverUri = value.lastOrNull()?.uri ?: "",
                images = value.toList(),
            )
        }
    }
}
