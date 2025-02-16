package com.lasthopesoftware.bluewater.settings.hidden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.shared.android.ui.components.LabeledSelection
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState

private val optionsPadding = PaddingValues(start = 32.dp, end = 32.dp)

@Composable
fun HiddenSettingsView(hiddenSettingsViewModel: HiddenSettingsViewModel) {
	Box(modifier = Modifier
		.fillMaxSize()
		.background(Color.Black)
	) {

		val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
		val layoutDirection = LocalLayoutDirection.current
		Column(
			modifier = Modifier
				.padding(
					start = systemBarsPadding.calculateStartPadding(layoutDirection),
					end = systemBarsPadding.calculateEndPadding(layoutDirection),
					bottom = systemBarsPadding.calculateBottomPadding(),
				)
				.fillMaxSize()
		) {
			Spacer(
				modifier = Modifier
					.windowInsetsTopHeight(WindowInsets.systemBars)
					.fillMaxWidth()
					.background(MaterialTheme.colors.surface)
			)

			Column(modifier = Modifier.weight(1f)) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.height(Dimensions.standardRowHeight)
						.padding(optionsPadding),
					verticalAlignment = Alignment.CenterVertically,
				) {
					val isLoggingToFile by hiddenSettingsViewModel.isLoggingToFile.subscribeAsState()
					val isLoading by hiddenSettingsViewModel.isLoading.subscribeAsState()
					LabeledSelection(
						label = stringResource(R.string.log_to_file),
						selected = isLoggingToFile,
						onSelected = { hiddenSettingsViewModel.promiseIsLoggingToFile(!isLoggingToFile) }
					) {
						Checkbox(checked = isLoggingToFile, onCheckedChange = null, enabled = !isLoading)
					}
				}
			}
		}
	}
}
