package com.example.fragmject.feature.authenticator.ui.export

/**
 * 一个待导出的二维码条目。
 *
 * 放在 `ui.export` 而非 `components`：它不是可复用组件，
 * 只服务于导出展示页（[AuthenticatorExportQrScreen] 的数据契约）。
 */
data class QrCodeItem(
    /** 二维码上方的主标题（如发行方，或批量导出的「第 x/y 张」）。 */
    val caption: String,
    /** 主标题下的副标题（如账户名）；为空则不显示。 */
    val name: String = "",
    /** 二维码内容，通常是 `otpauth://` 或 `otpauth-migration://` URI。 */
    val content: String,
    /**
     * 渲染边长（CSS 像素）。
     *
     * 内容越长二维码版本越高、模块越密，同样边长下就越难扫，
     * 因此批量码要画得更大（[BATCH_QR_SIZE]）才能保证每个模块有足够像素。
     */
    val size: Int = DEFAULT_QR_SIZE,
) {
    companion object {
        /** 单个 `otpauth://` 内容短，240px 足够。 */
        const val DEFAULT_QR_SIZE = 240

        /** 批量码内容长（见 `OtpMigrationUri.MAX_PAYLOAD_CHARS`），需要更大画布。 */
        const val BATCH_QR_SIZE = 300
    }
}
