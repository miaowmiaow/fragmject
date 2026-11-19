package com.example.fragmject.feature.authenticator.utils

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle

/**
 * 复制验证码或密钥到剪贴板。
 *
 * - API 33+：标记 [ClipDescription.EXTRA_IS_SENSITIVE]，系统剪贴板预览不显示明文。
 *
 * 不做自动清空。原因（真机验证，2026-11-19）：Android 10+ 后台应用读取剪贴板得到 null，
 * 而用户通常是复制后切到其他应用粘贴，此时延迟清除会被跳过，实际不生效；
 * 保留它只会让人误以为有保护。因此只保留上面的敏感标记。
 *
 * 这是应用安全（防止数据经剪贴板泄露），与加固（反逆向 / 反篡改）无关。
 */
internal object SensitiveClipboard {

    fun copy(context: Context, text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        val clip = ClipData.newPlainText("otp", text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard.setPrimaryClip(clip)
    }
}
