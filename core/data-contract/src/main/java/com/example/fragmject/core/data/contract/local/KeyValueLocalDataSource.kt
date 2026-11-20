package com.example.fragmject.core.data.contract.local

import kotlinx.coroutines.flow.Flow

/**
 * 通用键值本地端口（适配器侧）：一个 key 一个字符串的原始读写。
 *
 * ============================ 为什么是「通用 KV」而不是「宠物设置」============================
 * 早先叫 `PetSettingsLocalDataSource`，但签名是 `observeValue(key)` / `setValue(key, value)`，
 * key 完全由 repository 决定——名字说宠物、实际是任意 KV。两套口径会误导后来者
 * （以为新增字段要改端口，实际只要在 repository 加一个 key）。这里按真实语义改名。
 *
 * ============================ 谁负责字段语义 ============================
 * 本端口不解释任何字段：缺键回退默认值、范围夹紧都在
 * `PetSettingsRepository`（`core:data-repository`）里，key 也由它定义。
 * 这样 database 侧不需要业务知识，也就不必给「`core:database` 直接实现 domain 端口」
 * 的白名单加例外（规则 17）。
 *
 * 深色模式目前仍由 `ThemeLocalDataSourceImpl` 直接实现 domain 端口（规则 17 的存量例外），
 * 后续迁移时可直接复用本端口。
 */
interface KeyValueLocalDataSource {

    fun observeValue(key: String): Flow<String>

    /** 写入成功返回 true；存储层拒绝写入或发生可恢复的数据库错误时返回 false。 */
    suspend fun setValue(key: String, value: String): Boolean
}
