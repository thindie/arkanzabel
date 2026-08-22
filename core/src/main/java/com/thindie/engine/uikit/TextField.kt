package com.thindie.engine.uikit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

private const val CONTENT_ALPHA_DISABLED = 0.3f

@Composable
fun TextField(
  modifier: Modifier = Modifier,
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String = "",
  enabled: Boolean = true,
  singleLine: Boolean = false,
  maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
  textStyle: TextStyle = TextStyle.Default,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  imeOptions: ImeAction? = null,
  leadingContent: (@Composable () -> Unit)? = null,
  trailingContent: (@Composable () -> Unit)? = null,
) {
  val containerColor =
    when {
      !enabled -> AppTheme.colors.backgroundSecondary.copy(alpha = CONTENT_ALPHA_DISABLED)
      else -> AppTheme.colors.backgroundSecondary
    }

  var textFieldValue by remember { mutableStateOf(TextFieldValue(text = value)) }

  // Preserve cursor position on external value changes.
  LaunchedEffect(value) {
    if (textFieldValue.text != value) {
      val prefixLen = commonPrefixLength(textFieldValue.text, value)
      val oldSelEnd = textFieldValue.selection.end.coerceAtMost(textFieldValue.text.length)
      val offsetInCommon = (oldSelEnd - prefixLen).coerceAtLeast(0)
      val newPos = (prefixLen + offsetInCommon).coerceIn(0, value.length)

      textFieldValue =
        TextFieldValue(
          text = value,
          selection = TextRange(newPos),
        )
    }
  }

  BasicTextField(
    modifier = modifier,
    value = textFieldValue,
    onValueChange = { newValue ->
      textFieldValue = newValue
      onValueChange(newValue.text)
    },
    enabled = enabled,
    singleLine = singleLine,
    maxLines = maxLines,
    textStyle = textStyle.copy(color = AppTheme.colors.contentPrimary),
    keyboardOptions =
      if (imeOptions != null) {
        KeyboardOptions(
          imeAction = imeOptions,
          keyboardType = keyboardOptions.keyboardType,
          autoCorrect = keyboardOptions.autoCorrect,
          capitalization = keyboardOptions.capitalization,
        )
      } else {
        keyboardOptions
      },
    decorationBox = { innerText ->
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .then(
              if (singleLine) {
                Modifier.height(56.dp)
              } else {
                Modifier.heightIn(min = 56.dp)
              },
            )
            .background(
              color = containerColor,
              shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (leadingContent != null) {
          leadingContent()
          HSpacer(8.dp)
        }
        Box(
          modifier = Modifier.weight(1f),
          contentAlignment = Alignment.CenterStart,
        ) {
          if (textFieldValue.text.isEmpty() && placeholder.isNotEmpty()) {
            Text(
              text = placeholder,
              style = AppTheme.typography.placeholder,
              color = AppTheme.colors.contentTertiary,
            )
          } else {
            innerText()
          }
        }
        if (trailingContent != null) {
          HSpacer(8.dp)
          trailingContent()
        }
      }
    },
  )
}

private fun commonPrefixLength(
  a: String,
  b: String,
): Int {
  var i = 0
  while (i < a.length && i < b.length && a[i] == b[i]) {
    i++
  }
  return i
}
