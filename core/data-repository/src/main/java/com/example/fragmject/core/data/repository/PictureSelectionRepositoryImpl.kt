package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.local.KeyValueLocalDataSource
import com.example.fragmject.core.domain.repository.PictureSelectionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PictureSelectionRepository] 的 KV 适配器：整个选中列表存成一条记录。
 *
 * 编码用换行分隔（URI 取自相册，`content://` 不含换行）：比「一个 uri 一条记录」省一次
 * 全表遍历/清理——清空只需覆盖写入空串，而 KV 端口没有删除能力。
 *
 * @see PictureEditRepositoryImpl 同一张 KV 表里的编辑替换记录
 */
@Singleton
class PictureSelectionRepositoryImpl @Inject constructor(
    private val local: KeyValueLocalDataSource,
) : PictureSelectionRepository {

    override suspend fun loadSelectedUris(): List<String> =
        local.observeValue(KEY_SELECTED_URIS)
            .first()
            .lineSequence()
            .filter { it.isNotBlank() }
            .toList()

    override suspend fun saveSelectedUris(uris: List<String>) {
        local.setValue(KEY_SELECTED_URIS, uris.joinToString(SEPARATOR))
    }

    private companion object {
        const val KEY_SELECTED_URIS = "picture_selected_uris"
        const val SEPARATOR = "\n"
    }
}
