package com.example.fragmject.feature.authenticator.model

import androidx.compose.runtime.Immutable
import com.example.fragmject.core.domain.otp.OtpAccount
import com.example.fragmject.core.domain.otp.OtpType

/**
 * 列表条目：账户 + 当前验证码 + 倒计时进度。
 *
 * 放在 `model/` 中立包而不是某个 `ui/<功能>/`：`ui.list`（ViewModel 产出、Screen 消费）
 * 与 `components`（[com.example.fragmject.feature.authenticator.components.AccountCard]）
 * 都要引用它。若留在 `ui.list`，共享层 `components` 就会反向依赖具体页面包。
 */
/**
 * `account` 所在的 [OtpAccount] 位于 core:domain（无 compose 依赖，无法收录进
 * compose_stability_config.conf），故在此显式标注 [Immutable]：其字段全为 val、实际不可变。
 * 这样账户列表每 500ms 重建 List 时，内容未变的条目可以跳过重组。
 */
@Immutable
data class OtpCodeItem(
    val account: OtpAccount,
    val code: String,
    /** 当前时间片剩余毫秒；HOTP 无时间片，恒为 0。 */
    val remainingMillis: Long,
    val periodMillis: Long,
) {
    /** 是否按时间滚动（决定要不要显示倒计时环）。 */
    val isTimeBased: Boolean get() = account.type == OtpType.TOTP

    /** 展示用验证码：三位/四位分组（396 822），复制时由 UI 去掉空格。 */
    val displayCode: String = when (code.length) {
        6 -> "${code.take(3)} ${code.takeLast(3)}"
        8 -> "${code.take(4)} ${code.takeLast(4)}"
        else -> code
    }
}
