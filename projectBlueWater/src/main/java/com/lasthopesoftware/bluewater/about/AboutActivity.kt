package com.lasthopesoftware.bluewater.about

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsActivityIntentBuilder
import com.lasthopesoftware.bluewater.shared.android.ui.components.ApplicationInfoText
import com.lasthopesoftware.bluewater.shared.android.ui.components.ApplicationLogo
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.resources.intents.IntentFactory
import com.lasthopesoftware.resources.strings.StringResources

class AboutActivity : ComponentActivity() {
	private val stringResources by lazy { StringResources(this) }
	private val hiddenSettingsActivityIntentBuilder by lazy { HiddenSettingsActivityIntentBuilder(IntentFactory(this)) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		title = stringResources.aboutTitle

		setContent { ProjectBlueTheme { AboutView(hiddenSettingsActivityIntentBuilder) } }
	}
}

@Composable
fun AboutView(hiddenSettingsActivityIntentBuilder: HiddenSettingsActivityIntentBuilder) {
	Surface {
		BoxWithConstraints(modifier = Modifier
			.fillMaxSize()
			.padding(12.dp)
		) {
			if (maxWidth < maxHeight) AboutViewVertical(hiddenSettingsActivityIntentBuilder)
			else AboutViewHorizontal(hiddenSettingsActivityIntentBuilder)
		}
	}
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AboutViewVertical(
	hiddenSettingsActivityIntentBuilder: HiddenSettingsActivityIntentBuilder,
) {
	Column(
		Modifier.fillMaxSize()
	) {
		with (LocalContext.current) {
			val hapticFeedback = LocalHapticFeedback.current
			ApplicationLogo(
				modifier = Modifier
					.combinedClickable(
						interactionSource = remember { MutableInteractionSource() },
						indication = null,
						enabled = true,
						onLongClick = {
							hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
							startActivity(
								hiddenSettingsActivityIntentBuilder.buildHiddenSettingsIntent()
							)
						},
						onClick = {}
					)
					.fillMaxWidth(),
			)

			ApplicationInfoText(
				modifier = Modifier
					.fillMaxSize()
					.align(Alignment.CenterHorizontally)
					.padding(top = 48.dp)
			)
		}
	}
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AboutViewHorizontal(hiddenSettingsActivityIntentBuilder: HiddenSettingsActivityIntentBuilder) {
	Row(
		Modifier
			.fillMaxSize()) {
		with (LocalContext.current) {
			val hapticFeedback = LocalHapticFeedback.current
			ApplicationLogo(
				modifier = Modifier
					.combinedClickable(
						interactionSource = remember { MutableInteractionSource() },
						indication = null,
						enabled = true,
						onLongClick = {
							hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
							startActivity(
								hiddenSettingsActivityIntentBuilder.buildHiddenSettingsIntent()
							)
						},
						onClick = {}
					)
					.fillMaxHeight(),
			)

			ApplicationInfoText(
				modifier = Modifier.fillMaxSize().align(Alignment.CenterVertically)
			)
		}
	}
}

