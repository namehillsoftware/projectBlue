package com.lasthopesoftware.bluewater.about

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.shared.android.ui.components.ApplicationInfoText
import com.lasthopesoftware.bluewater.shared.android.ui.components.ApplicationLogo
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface

@Composable
fun AboutView(applicationNavigation: NavigateApplication) {
	ControlSurface {
		BoxWithConstraints(modifier = Modifier
			.fillMaxSize()
			.padding(12.dp)
		) {
			if (maxWidth < maxHeight) AboutViewVertical(applicationNavigation)
			else AboutViewHorizontal(applicationNavigation)
		}
	}
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AboutViewVertical(
	applicationNavigation: NavigateApplication,
) {
	Column(
		Modifier.fillMaxSize()
	) {
		val hapticFeedback = LocalHapticFeedback.current
		ApplicationLogo(
			modifier = Modifier
				.combinedClickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					enabled = true,
					onLongClick = {
						hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
						applicationNavigation.viewHiddenApplicationSettings()
					},
					onClick = {}
				)
				.fillMaxWidth(),
		)

		ApplicationInfoText(
			modifier = Modifier
				.fillMaxSize()
				.align(Alignment.CenterHorizontally)
				.padding(top = 48.dp)
		)
	}
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AboutViewHorizontal(applicationNavigation: NavigateApplication) {
	Row(
		Modifier.fillMaxSize()
	) {
		val hapticFeedback = LocalHapticFeedback.current
		ApplicationLogo(
			modifier = Modifier
				.combinedClickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					enabled = true,
					onLongClick = {
						hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
						applicationNavigation.viewHiddenApplicationSettings()
					},
					onClick = {}
				)
				.fillMaxHeight(),
		)

		ApplicationInfoText(
			modifier = Modifier
				.fillMaxSize()
				.align(Alignment.CenterVertically)
		)
	}
}

