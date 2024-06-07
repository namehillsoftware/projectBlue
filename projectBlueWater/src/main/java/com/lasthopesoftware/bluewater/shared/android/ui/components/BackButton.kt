package com.lasthopesoftware.bluewater.shared.android.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import com.lasthopesoftware.bluewater.shared.android.ui.navigable
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.LocalControlColor

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
	Icon(
		Icons.AutoMirrored.Filled.ArrowBack,
		contentDescription = "",
		tint = LocalControlColor.current,
		modifier = Modifier
			.navigable(
				onClick = onBack,
				indication = null,
				interactionSource = remember { MutableInteractionSource() }
			)
			.padding(Dimensions.viewPaddingUnit * 4)
			.then(modifier)
	)
}
