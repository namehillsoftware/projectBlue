package com.lasthopesoftware.bluewater.about

import android.os.Bundle
import android.view.View
import android.view.View.OnLongClickListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
				Image(
					painter = painterResource(id = R.drawable.project_blue_logo_circular),
					contentDescription = "Project Blue log",
					contentScale = ContentScale.Fit,
					alignment = Alignment.Center,
					modifier = Modifier.combinedClickable(
						enabled = true,
						onLongClick = { startActivity(hiddenSettingsActivityIntentBuilder.buildHiddenSettingsIntent()) }
					) {}
				)

				Text(
					getString(R.string.aboutAppText).format(
						getString(R.string.app_name),
						BuildConfig.VERSION_NAME,
						BuildConfig.VERSION_CODE,
						getString(R.string.company_name),
						getString(R.string.copyright_year)
					)
				)
			}
		}
	}

	override fun onLongClick(v: View): Boolean {
		startActivity(hiddenSettingsActivityIntentBuilder.buildHiddenSettingsIntent())
		return true
	}
}
