package com.lasthopesoftware.bluewater.shared.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.shared.android.ui.navigable

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RatingBar(
	rating: Int,
	modifier: Modifier = Modifier,
	color: Color = Color.Yellow,
	backgroundColor: Color = Color.Gray,
	onRatingSelected: ((Int) -> Unit)? = null
) {
	BoxWithConstraints(modifier = modifier) {
		Row(
			modifier = Modifier
				.fillMaxHeight()
				.align(Alignment.Center),
			horizontalArrangement = Arrangement.SpaceEvenly,
		) {
			val padding = 1.dp

			repeat(rating) { r ->
				var starModifier = Modifier.padding(start = padding, end = padding).requiredSize(this@BoxWithConstraints.maxHeight)
				if (onRatingSelected != null)
					starModifier = starModifier
						.navigable(
							interactionSource = remember { MutableInteractionSource() },
							indication = null,
							onClick = { onRatingSelected(r + 1) }
						)

				Image(
					painter = painterResource(id = R.drawable.ic_star_36),
					colorFilter = ColorFilter.tint(color),
					contentDescription = "Rating value",
					contentScale = ContentScale.Fit,
					modifier = starModifier,
				)
			}

			repeat(5 - rating) { r ->
				var starModifier = Modifier.padding(start = padding, end = padding)
				if (onRatingSelected != null) {
					starModifier = starModifier
						.navigable(
							interactionSource = remember { MutableInteractionSource() },
							indication = null,
							onClick = { onRatingSelected(r + 1 + rating) }
						)
				}

				Box(modifier = starModifier) {
					Image(
						painter = painterResource(id = R.drawable.ic_star_36),
						colorFilter = ColorFilter.tint(backgroundColor),
						contentDescription = "Rating value",
						contentScale = ContentScale.Fit,
						modifier = Modifier.requiredSize(this@BoxWithConstraints.maxHeight),
					)

					Image(
						painter = painterResource(id = R.drawable.ic_star_border_36),
						colorFilter = ColorFilter.tint(color),
						contentDescription = "Rating value",
						contentScale = ContentScale.Fit,
						modifier = Modifier.requiredSize(this@BoxWithConstraints.maxHeight),
					)
				}
			}
		}
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
			4,
			modifier = Modifier.height(20.dp)
		)
	}
}
