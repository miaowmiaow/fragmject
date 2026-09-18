package com.example.fragmject.feature.user.ui.mycoin

import com.example.fragmject.core.model.MyCoin

/**
 * MyCoin 的 UI 展示扩展：从 desc 中解析出时间与标题。
 *
 * 保持在 core:ui 层（而非 core:model），避免领域模型承载字符串解析展示逻辑。
 */
fun MyCoin.getTime(): String {
    return desc.substring(0, getSecondSpace())
}

fun MyCoin.getTitle(): String {
    return desc.substring(getSecondSpace() + 1)
}

private fun MyCoin.getFirstSpace(): Int {
    return desc.indexOf(" ")
}

private fun MyCoin.getSecondSpace(): Int {
    return desc.indexOf(" ", getFirstSpace() + 1)
}
