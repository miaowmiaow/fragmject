package com.example.fragmject.feature.authenticator.utils

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle

/**
 * 复制验证码或密钥到剪贴板。
 *
 * - API 33+：标记 [ClipDescription.EXTRA_IS_SENSITIVE]，系统剪贴板预览不显示明文。
 * - 复制后 [CLEAR_DELAY_MS] 毫秒尝试清空：仅当剪贴板内容仍是本次复制的文本时才清除，
 *   避免覆盖用户之后复制的其他内容。
 *
 * 局限（平台行为）：Android 10+ 后台应用读取剪贴板得到 null，因此只有应用仍在前台时清除才会生效。
 * 用户切到其他应用粘贴后，若应用不在前台，本次清除会被跳过。这是最佳努力，不是保证。
 *
 * 这是应用安全（防止数据经剪贴板泄露），与加固（反逆向 / 反篡改）无关。
 */
internal object SensitiveClipboard {

    /** 复制后尝试清除的延迟。 */
    const val CLEAR_DELAY_MS = 30_000L

    private val handler = Handler(Looper.getMainLooper())
    private var pendingClear: Runnable? = null

    fun copy(context: Context, text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        val clip = ClipData.newPlainText("otp", text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard.setPrimaryClip(clip)

        pendingClear?.let(handler::removeCallbacks)
        val task = Runnable { clearIfUnchanged(clipboard, text) }
        pendingClear = task
        handler.postDelayed(task, CLEAR_DELAY_MS)
    }

    private fun clearIfUnchanged(clipboard: ClipboardManager, copied: String) {
        pendingClear = null
        val current = clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.text
            ?.toString()
        // clearPrimaryClip 自 API 28 起可用；minSdk 为 26，低版本跳过清除（与「最佳努力」的约定一致）。
        if (current == copied && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        }
    }
}
