package com.lasthopesoftware.bluewater.android.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions

@Composable
fun LabeledSelection(
	label: String,
	selected: Boolean,
	onSelected: () -> Unit,
	role: Role? = null,
	enabled: Boolean = true,
	selectableContent: @Composable () -> Unit,
) {
	Row(
		modifier = Modifier
			.selectable(
				selected = selected,
				onClick = onSelected,
				enabled = enabled,
				role = role,
			),
	) {
		selectableContent()

		Text(
			text = label,
			modifier = Modifier.padding(start = Dimensions.viewPaddingUnit * 4),
		)
	}
}
