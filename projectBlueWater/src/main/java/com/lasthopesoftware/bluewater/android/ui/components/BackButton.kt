package com.lasthopesoftware.bluewater.android.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.navigable
import com.lasthopesoftware.bluewater.android.ui.theme.LocalControlColor

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier, focusRequester: FocusRequester? = null) {
	Icon(
		painter = painterResource(id = R.drawable.arrow_left_24dp),
		contentDescription = stringResource(R.string.navigate_back),
		tint = LocalControlColor.current,
		modifier = Modifier
			.navigable(
				onClick = onBack,
				indication = null,
				interactionSource = remember { MutableInteractionSource() },
				focusRequester = focusRequester
			)
			.then(modifier)
	)
}
