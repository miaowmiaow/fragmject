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
 * 写入结果透传给 repository，由业务层决定是否需要中断流程；这里再补一条带 key 的 warn，
 * 避免「设置页点了没反应」时无从排查。
 */
@Singleton
class KeyValueLocalDataSourceImpl @Inject constructor(
    private val kvDatabase: KVDatabase,
) : KeyValueLocalDataSource {

    override fun observeValue(key: String): Flow<String> = kvDatabase.observeValue(key)

    override suspend fun setValue(key: String, value: String): Boolean {
        val saved = kvDatabase.setValue(key, value)
        if (!saved) {
            Log.w(TAG, "写入失败（key=$key）：KVDatabase 返回 false，本次设置不会生效")
        }
        return saved
    }

    private companion object {
        const val TAG = "KeyValueLocalDataSource"
    }
}
