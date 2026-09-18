package com.example.fragmject.core.domain.repository

/**
 * 相册图片（纯数据模型，uri 用 String 保持 domain 纯净）。
 */
data class AlbumImage(
    val name: String,
    val uri: String,
    val width: Int,
    val height: Int,
    val mimeType: String,
)

/**
 * 相册分组（纯数据模型）。
 *
 * [images] 按时间降序排列（最新在前），[coverUri] 为封面图 uri。
 */
data class AlbumGroup(
    val name: String,
    val coverUri: String,
    val images: List<AlbumImage>,
)

/**
 * 相册查询领域端口。
 *
 * 抽象「查询系统相册图片并按相册分组」的能力，供相册选择器使用。
 */
interface AlbumRepository {
    suspend fun queryAlbums(): List<AlbumGroup>
}
