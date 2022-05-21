package com.lasthopesoftware.bluewater.shared.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RatingBar(
	rating: Float,
	modifier: Modifier = Modifier,
	color: Color = Color.Yellow,
	backgroundColor: Color = Color.Gray,
) {
	Row(modifier = modifier.wrapContentSize()) {
		(1..5).forEach { step ->
			val stepRating = when {
				rating > step -> 1f
				step.rem(rating) < 1 -> rating - (step - 1f)
				else -> 0f
			}
			RatingStar(stepRating, color, backgroundColor)
		}
	}
}

@Composable
private fun RatingStar(
	rating: Float,
	ratingColor: Color = Color.Yellow,
	backgroundColor: Color = Color.Gray
) {
	BoxWithConstraints(modifier = Modifier.padding(horizontal = 2.dp)) {
		BoxWithConstraints(
			modifier = Modifier
				.fillMaxHeight()
				.aspectRatio(1f)
				.clip(starShape)
				.border(width = 1.dp, color = ratingColor, shape = starShape)
		) {
			Canvas(modifier = Modifier.size(maxHeight)) {
				drawRect(
					brush = SolidColor(backgroundColor),
					size = Size(
						height = size.height * 1.1f,
						width = size.width
					)
				)

				if (rating > 0) {
					drawRect(
						brush = SolidColor(ratingColor),
						size = Size(
							height = size.height * 1.1f,
							width = size.width * rating
						)
					)
				}
			}
		}
	}
}

private val starShape = GenericShape { size, _ ->
	addPath(starPath(size.height))
}

private val starPath = { size: Float ->
	Path().apply {
		val outerRadius: Float = size / 1.8f
		val innerRadius: Double = outerRadius / 2.5
		var rot: Double = Math.PI / 2 * 3
		val cx: Float = size / 2
		val cy: Float = size / 20 * 11
		val step = Math.PI / 5

		moveTo(cx, cy - outerRadius)
		repeat(5) {
			var x = (cx + cos(rot) * outerRadius).toFloat()
			var y = (cy + sin(rot) * outerRadius).toFloat()
			lineTo(x, y)
			rot += step

			x = (cx + cos(rot) * innerRadius).toFloat()
			y = (cy + sin(rot) * innerRadius).toFloat()
			lineTo(x, y)
			rot += step
		}
		close()
	}
}

@Preview
@Composable
fun RatingBarPreview() {
	Column(
		Modifier
			.fillMaxSize()
			.background(Color.White)
	) {
		RatingBar(
			3.8f,
			modifier = Modifier.height(20.dp)
		)
	}
}
