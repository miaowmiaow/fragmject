package com.example.fragmject.core.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * 密码输入框（安全文本）。
 *
 * 为什么不是 `WhiteTextField` + `PasswordVisualTransformation`：后者所属的
 * 「基于 `String` + `VisualTransformation` 的 TextField 家族」已废弃，官方迁移方向是
 * `SecureTextField` / `BasicSecureTextField`。两者的差别不只是 API 形状——
 * [SecureTextField] 用平台安全输入通道处理文本，密码不会被普通文本渲染管线接触
 * （不参与文本撤销栈、不进入无障碍节点文本、截图/输入法联想面更小）。
 *
 * 样式与 [WhiteTextField] 保持一致（透明容器、同色文本/光标/下划线/占位符），
 * 调用方只需把 `value`/`onValueChange` 换成 [TextFieldState]。
 *
 * **状态创建必须用 `remember` 而不是 `rememberTextFieldState`**：
 * 后者内部走 `rememberSaveable`，密码文本会进入 saved-state（进程重建时可被系统回填），
 * 与「凭据不落 saved-state」的口径冲突。
 *
 * [onKeyboardAction] 传 `null` 表示走框架默认动作（`ImeAction.Next` 会自动移动焦点）；
 * 需要「回车即提交」时传 `KeyboardActionHandler { 提交逻辑 }`。
 */
@Composable
fun WhiteSecureTextField(
    state: TextFieldState,
    modifier: Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
) {
    SecureTextField(
        state = state,
        modifier = modifier,
        textStyle = textStyle,
        placeholder = placeholder,
        keyboardOptions = keyboardOptions,
        // null 时执行 performDefaultAction，保留 Next/Go 等的默认行为
        onKeyboardAction = onKeyboardAction ?: KeyboardActionHandler { performDefaultAction ->
            performDefaultAction()
        },
        colors = TextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.onPrimaryContainer,
            errorCursorColor = MaterialTheme.colorScheme.onPrimaryContainer,
            focusedIndicatorColor = MaterialTheme.colorScheme.onPrimaryContainer,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.onPrimaryContainer,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onPrimaryContainer,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}
