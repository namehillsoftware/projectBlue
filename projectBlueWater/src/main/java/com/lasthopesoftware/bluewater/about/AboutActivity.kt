package com.lasthopesoftware.bluewater.about

import android.os.Bundle
import android.view.View
import android.view.View.OnLongClickListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
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

class AboutActivity : ComponentActivity(), OnLongClickListener {
	private val aboutTitleBuilder by lazy { AboutTitleBuilder(this) }
	private val hiddenSettingsActivityIntentBuilder by lazy { HiddenSettingsActivityIntentBuilder(IntentFactory(this@AboutActivity)) }

	@ExperimentalFoundationApi
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		title = aboutTitleBuilder.buildTitle()

		setContent {
			ProjectBlueTheme {
				AboutView()
			}
		}
	}

	override fun onLongClick(v: View): Boolean {
		startActivity(hiddenSettingsActivityIntentBuilder.buildHiddenSettingsIntent())
		return true
	}
}

@ExperimentalFoundationApi
@Preview
@Composable
fun AboutView() {
	ProjectBlueTheme {
		Column {
			val context = LocalContext.current
			val hiddenSettingsActivityIntentBuilder = HiddenSettingsActivityIntentBuilder(IntentFactory(context))
			Image(
				painter = painterResource(id = R.drawable.project_blue_logo_circular),
				contentDescription = "Project Blue log",
				contentScale = ContentScale.Fit,
				alignment = Alignment.Center,
				modifier = Modifier.combinedClickable(
					enabled = true,
					onLongClick = {
						context.startActivity(
							hiddenSettingsActivityIntentBuilder.buildHiddenSettingsIntent()
						)
					}
				) {}
			)

			Text(
				context.getString(R.string.aboutAppText).format(
					context.getString(R.string.app_name),
					BuildConfig.VERSION_NAME,
					BuildConfig.VERSION_CODE,
					context.getString(R.string.company_name),
					context.getString(R.string.copyright_year)
				),
				textAlign = TextAlign.Center,
				modifier = Modifier.align(Alignment.CenterHorizontally)
			)
		}
	}
}
