package com.lasthopesoftware.bluewater.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

// Originally sourced from https://stackoverflow.com/a/68980032, modified to use the built-in marquee modifier

@Composable
fun MarqueeText(
	text: String,
	modifier: Modifier = Modifier,
	textModifier: Modifier = Modifier,
	gradientEdgeColor: Color = Color.White,
	color: Color = Color.Unspecified,
	fontSize: TextUnit = TextUnit.Unspecified,
	fontStyle: FontStyle? = null,
	fontWeight: FontWeight? = null,
	fontFamily: FontFamily? = null,
	letterSpacing: TextUnit = TextUnit.Unspecified,
	textDecoration: TextDecoration? = null,
	textAlign: TextAlign? = null,
	lineHeight: TextUnit = TextUnit.Unspecified,
	overflow: TextOverflow = TextOverflow.Clip,
	softWrap: Boolean = true,
	onTextLayout: (TextLayoutResult) -> Unit = {},
	style: TextStyle = LocalTextStyle.current,
	gradientSides: Set<GradientSide> = setOf(GradientSide.Start, GradientSide.End),
	isMarqueeEnabled: Boolean = true,
) {
	val createText = @Composable { localModifier: Modifier ->
		Text(
			text,
			textAlign = textAlign,
			modifier = if (isMarqueeEnabled) localModifier.basicMarquee() else localModifier,
			color = color,
			fontSize = fontSize,
			fontStyle = fontStyle,
			fontWeight = fontWeight,
			fontFamily = fontFamily,
			letterSpacing = letterSpacing,
			textDecoration = textDecoration,
			lineHeight = lineHeight,
			overflow = overflow,
			softWrap = softWrap,
			maxLines = 1,
			onTextLayout = onTextLayout,
			style = style,
		)
	}

	SubcomposeLayout(
		modifier = modifier.clipToBounds()
	) { constraints ->
		val mainText = subcompose(MarqueeLayers.MainText) {
			createText(textModifier.fillMaxWidth())
		}.first().measure(constraints)

		val gradient = if (mainText.width > constraints.maxWidth) {
			subcompose(MarqueeLayers.EdgesGradient) {
				Row {
					val transparent = gradientEdgeColor.copy(alpha = 0f)

					if (gradientSides.contains(GradientSide.Start))
						GradientEdge(gradientEdgeColor, transparent)

					Spacer(Modifier.weight(1f))

					if (gradientSides.contains(GradientSide.End))
						GradientEdge(transparent, gradientEdgeColor)
				}
			}.first().measure(constraints.copy(maxHeight = mainText.height))
		} else null

		layout(
			width = constraints.maxWidth,
			height = mainText.height
		) {
			mainText.place(0, 0)
			gradient?.place(0, 0)
		}
	}
}

@Composable
private fun GradientEdge(
	startColor: Color, endColor: Color,
) {
	Box(
		modifier = Modifier
			.width(10.dp)
			.fillMaxHeight()
			.background(
				brush = Brush.horizontalGradient(
					0f to startColor, 1f to endColor,
				)
			)
	)
}

enum class GradientSide { Start, End }
private enum class MarqueeLayers { MainText, EdgesGradient }
