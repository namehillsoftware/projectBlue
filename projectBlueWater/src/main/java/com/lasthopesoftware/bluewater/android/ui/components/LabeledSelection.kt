package com.lasthopesoftware.bluewater.android.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions

@Composable
fun LabeledSelection(
	label: String,
	selected: Boolean,
	onSelected: () -> Unit,
	selectableContent: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	role: Role? = null,
	enabled: Boolean = true,
) {
	Row(
		modifier = Modifier
			.selectable(
				selected = selected,
				onClick = onSelected,
				enabled = enabled,
				role = role,
			)
			.then(modifier),
		verticalAlignment = Alignment.CenterVertically,
	) {
		selectableContent()

		Text(
			text = label,
			modifier = Modifier.padding(start = Dimensions.viewPaddingUnit * 4),
		)
	}
}
