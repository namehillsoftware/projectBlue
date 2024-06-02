package com.lasthopesoftware.bluewater.shared.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme

@Composable
fun ProjectBlueComposableApplication(
	darkTheme: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit
) {
	NavigableApplication {
		ProjectBlueTheme(darkTheme, content)
	}
}
