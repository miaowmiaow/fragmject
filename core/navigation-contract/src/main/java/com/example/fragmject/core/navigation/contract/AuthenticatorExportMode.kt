package com.example.fragmject.core.navigation.contract

/**
 * 验证器导出二维码的两种格式。
 *
 * 放在契约层是因为它是 `openAuthenticatorExportQr` 的**语义参数**：调用方只表达
 * 「要单账户码还是批量码」，不必知道 `otpauth://` / `otpauth-migration://` 这些实现细节。
 * 不要给默认值——调用方必须显式选择，避免误用默认项把不想批量的账户批量导出。
 */
enum class AuthenticatorExportMode {
    /** 每个账户一张 `otpauth://`（通用，任何验证器 App 都能扫）。 */
    SINGLE,

    /** 一张 `otpauth-migration://` 装多个账户（Google Authenticator 的批量迁移格式）。 */
    BATCH,
}
