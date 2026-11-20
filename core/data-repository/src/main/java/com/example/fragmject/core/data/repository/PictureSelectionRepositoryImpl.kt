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

    override suspend fun loadPendingCapture(): PictureSelectionRepository.PendingCapture? {
        val encoded = local.observeValue(KEY_PENDING_CAPTURE_URI).first()
        if (encoded.isBlank()) return null
        val separator = encoded.indexOf(SEPARATOR)
        if (separator <= 0) {
            // 兼容尚未发布的纯 URI 格式记录。
            return PictureSelectionRepository.PendingCapture(
                encoded,
                PictureSelectionRepository.CaptureDisposition.AWAITING_RESULT,
            )
        }
        val disposition = runCatching {
            PictureSelectionRepository.CaptureDisposition.valueOf(encoded.substring(0, separator))
        }.getOrNull() ?: return null
        val uri = encoded.substring(separator + 1).takeIf { it.isNotBlank() } ?: return null
        return PictureSelectionRepository.PendingCapture(uri, disposition)
    }

    override suspend fun savePendingCapture(capture: PictureSelectionRepository.PendingCapture?): Boolean {
        val encoded = capture?.let { "${it.disposition.name}$SEPARATOR${it.uri}" }.orEmpty()
        return local.setValue(KEY_PENDING_CAPTURE_URI, encoded)
    }

    private companion object {
        const val KEY_SELECTED_URIS = "picture_selected_uris"
        const val KEY_PENDING_CAPTURE_URI = "picture_pending_capture_uri"
        const val SEPARATOR = "\n"
    }
}
