package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.local.KeyValueLocalDataSource
import com.example.fragmject.core.domain.repository.PictureEditRepository
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PictureEditRepository] 的 KV 适配器：一条「旧 uri → 新 uri」对应一条 KV 记录。
 *
 * 一个旧 uri 一条记录（而不是把整张表塞进一条记录）的好处是查询只走一次按键查询，
 * 不需要序列化/反序列化整表；代价是记录只增不减——本类不做过期清理，因为旧 uri
 * 仍可能来自其它页面的缓存或信息流数据，解析到编辑后的新图也好过解析到已删除的原图。
 */
@Singleton
class PictureEditRepositoryImpl @Inject constructor(
    private val local: KeyValueLocalDataSource,
) : PictureEditRepository {

    override suspend fun recordReplacement(oldUri: String, newUri: String) {
        if (!local.setValue(keyOf(oldUri), newUri)) {
            throw IOException("Unable to persist edited image URI replacement")
        }
    }

    override suspend fun findReplacement(oldUri: String): String? =
        local.observeValue(keyOf(oldUri)).first().ifBlank { null }

    private fun keyOf(oldUri: String): String = "$KEY_PREFIX$oldUri"

    private companion object {
        /** 加前缀避免与设置项等业务键混在同一张 KV 表里。 */
        const val KEY_PREFIX = "picture_edit_replacement:"
    }
}
