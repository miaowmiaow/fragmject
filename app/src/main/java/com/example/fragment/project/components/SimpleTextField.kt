package com.example.fragment.project.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.fragment.project.R

@Composable
fun WhiteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = TextFieldDefaults.textFieldColors(
            textColor = colorResource(id = R.color.text_fff),
            backgroundColor = colorResource(id = R.color.transparent),
            cursorColor = colorResource(id = R.color.white),
            focusedIndicatorColor = colorResource(id = R.color.white),
            unfocusedIndicatorColor = colorResource(id = R.color.white),
            leadingIconColor = colorResource(id = R.color.white),
            focusedLabelColor = colorResource(id = R.color.white),
            unfocusedLabelColor = colorResource(id = R.color.white),
            placeholderColor = colorResource(id = R.color.text_ccc),
        ),
        singleLine = true,
    )
}

@OptIn(ExperimentalMaterialApi::class)
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
    colors: TextFieldColors = TextFieldDefaults.textFieldColors()
) {
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(enabled).value
    }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))
    BasicTextField(value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        modifier = modifier.indicatorLine(enabled, isError, interactionSource, colors),
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
                    modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isBlank()) {
                        if (placeholder != null) {
                            placeholder()
                        }
                    }
                    innerTextField()
                }
                if (trailingIcon != null) {
                    trailingIcon()
                }
                if (value.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(10.dp, 6.dp, 10.dp, 6.dp)
                            .clickable { onClear() },
                        tint = textStyle.color
                    )
                }
            }
        })
}