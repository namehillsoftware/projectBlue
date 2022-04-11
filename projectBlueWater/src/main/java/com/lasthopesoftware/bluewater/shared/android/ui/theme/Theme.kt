package com.lasthopesoftware.bluewater.shared.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColors(
	primary = Dark.ProjectBlue,
	primaryVariant = Dark.ProjectBlueLight,
	secondary = Dark.ProjectBlueDark
)

private val LightColorPalette = lightColors(
	primary = Light.ProjectBlue,
	primaryVariant = Light.ProjectBlueLight,
	secondary = Light.ProjectBlueDark,

	/* Other default colors to override
	background = Color.White,
	surface = Color.White,
	onPrimary = Color.White,
	onSecondary = Color.Black,
	onBackground = Color.Black,
	onSurface = Color.Black,
	*/
)

@Composable
fun ProjectBlueTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit
) {
	val colors = if (darkTheme) DarkColorPalette else LightColorPalette

	MaterialTheme(
		colors = colors,
		typography = Typography,
		shapes = Shapes,
		content = content
	)
}
