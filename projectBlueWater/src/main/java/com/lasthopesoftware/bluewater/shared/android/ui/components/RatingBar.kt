package com.lasthopesoftware.bluewater.shared.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.R

@Composable
fun RatingBar(
	rating: Int,
	modifier: Modifier = Modifier,
	color: Color = Color.Yellow,
	backgroundColor: Color = Color.Gray,
) {
	Row(modifier = modifier.wrapContentSize()) {
		val padding = 1.dp

		repeat(rating) {
			Image(
				painter = painterResource(id = R.drawable.ic_star_36),
				colorFilter = ColorFilter.tint(color),
				contentDescription = "Rating value",
				contentScale = ContentScale.Fit,
				modifier = Modifier.padding(start = padding, end = padding),
			)
		}

		repeat(5 - rating) {
			Box(
				modifier = Modifier.padding(start = padding, end = padding),
			) {
				Image(
					painter = painterResource(id = R.drawable.ic_star_36),
					colorFilter = ColorFilter.tint(backgroundColor),
					contentDescription = "Rating value",
					contentScale = ContentScale.Fit,
				)

				Image(
					painter = painterResource(id = R.drawable.ic_star_border_36),
					colorFilter = ColorFilter.tint(color),
					contentDescription = "Rating value",
					contentScale = ContentScale.Fit,
				)
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
