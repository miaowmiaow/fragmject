package com.example.fragmject.core.database.local

import android.util.Log
import com.example.fragmject.core.data.contract.local.KeyValueLocalDataSource
import com.example.fragmject.core.database.KVDatabase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [KeyValueLocalDataSource] 的 KV 适配器：只转发读写，不做任何字段解释。
 *
 * 写入结果**不再丢弃**：[KVDatabase.setValue] 用 `id > -1` 表示成功（失败时它已记一条 error），
 * 这里再补一条 warn 并带上 key，避免「设置页点了没反应」时无从排查——
 * 上层是 fire-and-forget 调用（设置项不阻塞 UI），没有返回值可传，日志是唯一线索。
 */
@Singleton
class KeyValueLocalDataSourceImpl @Inject constructor(
    private val kvDatabase: KVDatabase,
) : KeyValueLocalDataSource {

    override fun observeValue(key: String): Flow<String> = kvDatabase.observeValue(key)

    override suspend fun setValue(key: String, value: String) {
        if (!kvDatabase.setValue(key, value)) {
            Log.w(TAG, "写入失败（key=$key）：KVDatabase 返回 false，本次设置不会生效")
        }
    }

    private companion object {
        const val TAG = "KeyValueLocalDataSource"
    }
}
