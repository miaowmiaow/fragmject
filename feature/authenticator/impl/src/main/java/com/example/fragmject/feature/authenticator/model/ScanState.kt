package com.example.fragmject.feature.authenticator.model

/**
 * 扫码状态机：与参考项目（HugeRivers/Authenticator）一致的状态流转。
 *
 * 放在 `model/` 中立包：`ui.add` 的扫码页持有并推进它，`components` 的取景框与
 * 状态指示器消费它。两边都不是它的「所有者」，因此不能挂在任一方包下。
 */
sealed interface ScanState {
    /** 正在取景识别。 */
    data object Scanning : ScanState

    /** 已识别到内容，等待后续处理（业务层）。 */
    data object Detected : ScanState

    /** 识别成功（已填表或已导入）。 */
    data object Success : ScanState

    /** 内容无法识别；[message] 展示给用户。 */
    data class Error(val message: String) : ScanState
}
