package com.lasthopesoftware.bluewater.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.lasthopesoftware.bluewater.android.ui.theme.ProjectBlueTheme

@Composable
fun ProjectBlueComposableApplication(
	darkTheme: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit
) {
	NavigableApplication {
		ProjectBlueTheme(darkTheme, content)
	}
}
