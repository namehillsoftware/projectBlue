package com.lasthopesoftware.bluewater.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.ScreenDimensionsScope

@Composable
fun PaddedSystemScreenBox(
	content: @Composable ScreenDimensionsScope.() -> Unit,
) {
	val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
	val layoutDirection = LocalLayoutDirection.current
	val systemBarsWithoutTop = PaddingValues(
		start = systemBarsPadding.calculateStartPadding(layoutDirection),
		end = systemBarsPadding.calculateEndPadding(layoutDirection),
		bottom = systemBarsPadding.calculateBottomPadding(),
	)
	val topPadding = systemBarsPadding.calculateTopPadding()
	BoxWithConstraints(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colors.surface)
			.padding(top = topPadding)
			.background(Color.Black)
			.padding(systemBarsWithoutTop)
			.clipToBounds()
	) screenBox@{
		BoxWithConstraints(
			modifier = Modifier.fillMaxSize()
		) nestedBox@{
			val screenScope = ScreenDimensionsScope(
				screenHeight = this@screenBox.maxHeight,
				screenWidth = this@screenBox.maxWidth,
				innerBoxScope = this@nestedBox
			)

			ControlSurface(modifier = Modifier.fillMaxSize()) {
				screenScope.content()
			}
		}
	}
}
