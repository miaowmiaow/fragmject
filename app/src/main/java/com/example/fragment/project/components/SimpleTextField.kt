package com.example.fragment.project.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun WhiteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        placeholder = placeholder,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TextFieldColors = TextFieldDefaults.colors()
) {
    val textColor = textStyle.color.takeOrElse {
        val focused by interactionSource.collectIsFocusedAsState()
        when {
            !enabled -> colors.disabledTextColor
            isError -> colors.errorTextColor
            focused -> colors.focusedTextColor
            else -> colors.unfocusedTextColor
        }
    }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))
    BasicTextField(value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        modifier = modifier
            .clipToBounds()
            .indicatorLine(enabled, isError, interactionSource, colors),
        textStyle = mergedTextStyle,
        cursorBrush = SolidColor(mergedTextStyle.color),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        decorationBox = @Composable { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    leadingIcon()
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isBlank() && placeholder != null) {
                        placeholder()
                    }
                    innerTextField()
                }
                if (trailingIcon != null) {
                    trailingIcon()
                }
                if (value.isNotBlank()) {
                    IconButton(
                        modifier = Modifier.padding(vertical = 6.dp),
                        onClick = onClear
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = null,
                            tint = textStyle.color
                        )
                    }
                }
            }
        })
}