package com.lasthopesoftware.bluewater.about

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsActivityIntentBuilder
import com.lasthopesoftware.bluewater.shared.android.ui.theme.theme.ProjectBlueTheme
import com.lasthopesoftware.resources.intents.IntentFactory

class AboutActivity : ComponentActivity() {
	private val aboutTitleBuilder by lazy { AboutTitleBuilder(this) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		title = aboutTitleBuilder.buildTitle()

		setContent {
			ProjectBlueTheme {
				AboutView()
			}
		}
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun AboutView() {
	ProjectBlueTheme {
		Box(Modifier.fillMaxSize()) {
			with (LocalContext.current) {
				val hiddenSettingsActivityIntentBuilder = HiddenSettingsActivityIntentBuilder(IntentFactory(this))
				Image(
					painter = painterResource(id = R.drawable.project_blue_logo_circular),
					contentDescription = "Project Blue logo",
					contentScale = ContentScale.Fit,
					alignment = Alignment.TopCenter,
					modifier = Modifier.combinedClickable(
						enabled = true,
						onLongClick = {
							startActivity(
								hiddenSettingsActivityIntentBuilder.buildHiddenSettingsIntent()
							)
						},
						onClick = {}
					).then(Modifier.fillMaxSize())
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
