package com.lasthopesoftware.bluewater.shared.android.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.shared.android.ui.navigable
import com.lasthopesoftware.bluewater.shared.android.ui.theme.LocalControlColor

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
	Icon(
		Icons.AutoMirrored.Filled.ArrowBack,
		contentDescription = stringResource(R.string.navigate_back),
		tint = LocalControlColor.current,
		modifier = Modifier
			.navigable(
				onClick = onBack,
				indication = null,
				interactionSource = remember { MutableInteractionSource() }
			)
			.then(modifier)
	)
}
