package com.lasthopesoftware.bluewater.android.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun StandardTextField(
	placeholder: String,
	value: String,
	onValueChange: (String) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	visualTransformation: VisualTransformation = VisualTransformation.None,
	keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
	val focusManager = LocalFocusManager.current
	TextField(
		modifier = Modifier
			.fillMaxWidth()
			.onPreviewKeyEvent {
				if (it.key == Key.Tab && it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
					focusManager.moveFocus(FocusDirection.Down)
					true
				} else {
					false
				}
			}
			.then(modifier),
		value = value,
		placeholder = { Text(placeholder) },
		onValueChange = onValueChange,
		enabled = enabled,
		singleLine = true,
		visualTransformation = visualTransformation,
		keyboardOptions = keyboardOptions.copy(imeAction = ImeAction.Next),
		keyboardActions = KeyboardActions(
			onNext = { focusManager.moveFocus(FocusDirection.Down) }
		)
	)
}
