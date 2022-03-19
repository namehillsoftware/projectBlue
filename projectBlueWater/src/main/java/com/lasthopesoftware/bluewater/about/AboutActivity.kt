package com.lasthopesoftware.bluewater.about

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsActivityIntentBuilder
import com.lasthopesoftware.bluewater.shared.android.ui.theme.theme.ProjectBlueTheme
import com.lasthopesoftware.resources.intents.IntentFactory
import com.lasthopesoftware.resources.strings.StringResources

class AboutActivity : ComponentActivity() {
	private val stringResources by lazy { StringResources(this) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		title = stringResources.aboutTitle

		setContent { ProjectBlueTheme { AboutView() } }
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun AboutView() {
	ProjectBlueTheme {
		Box(Modifier.fillMaxSize().padding(Dp(10f))) {
			with (LocalContext.current) {
				val hapticFeedback = LocalHapticFeedback.current
				val hiddenSettingsActivityIntentBuilder = HiddenSettingsActivityIntentBuilder(IntentFactory(this))
				Image(
					painter = painterResource(id = R.drawable.project_blue_logo_circular),
					contentDescription = "Project Blue logo",
					contentScale = ContentScale.FillWidth,
					alignment = Alignment.TopCenter,
					modifier = Modifier
						.combinedClickable(
							enabled = true,
							onLongClick = {
								hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
								startActivity(
									hiddenSettingsActivityIntentBuilder.buildHiddenSettingsIntent()
								)
							},
							onClick = {}
						)
						.fillMaxWidth()
				)

				Text(
					getString(R.string.aboutAppText).format(
						getString(R.string.app_name),
						BuildConfig.VERSION_NAME,
						BuildConfig.VERSION_CODE,
						getString(R.string.company_name),
						getString(R.string.copyright_year)
					),
					textAlign = TextAlign.Center,
					modifier = Modifier.align(Alignment.BottomCenter)
				)
			}
		}
	}
}
