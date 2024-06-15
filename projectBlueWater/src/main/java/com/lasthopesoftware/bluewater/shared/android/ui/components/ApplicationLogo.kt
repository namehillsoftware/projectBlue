package com.lasthopesoftware.bluewater.shared.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.R

@Composable
@Preview
fun ApplicationLogo(modifier: Modifier = Modifier) {
	Image(
		painter = painterResource(id = R.drawable.project_blue_logo),
		contentDescription = stringResource(R.string.app_logo, stringResource(id = R.string.app_name)),
		contentScale = ContentScale.Inside,
		alignment = Alignment.TopCenter,
		modifier = Modifier
			.shadow(elevation = 20.dp, shape = CircleShape, clip = true)
			.then(modifier),
	)
}

@Composable
@Preview
fun ApplicationInfoText(versionName: String = "4.0.0", versionCode: Int = 3500, modifier: Modifier = Modifier) {
	Text(
		stringResource(
			R.string.aboutAppText,
			stringResource(R.string.app_name),
			versionName,
			versionCode,
			stringResource(R.string.company_name),
			stringResource(R.string.copyright_year)
		),
		textAlign = TextAlign.Center,
		modifier = modifier,
	)
}
