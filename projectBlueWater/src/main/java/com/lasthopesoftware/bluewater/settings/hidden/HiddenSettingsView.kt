package com.lasthopesoftware.bluewater.settings.hidden

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.shared.android.ui.components.LabeledSelection
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState

private val optionsPadding = PaddingValues(start = 32.dp, end = 32.dp)

@Composable
fun HiddenSettingsView(hiddenSettingsViewModel: HiddenSettingsViewModel) {
	val isLoading by hiddenSettingsViewModel.isLoading.subscribeAsState()
	val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

	Column(
		modifier = Modifier.padding(systemBarsPadding)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.height(Dimensions.standardRowHeight)
				.padding(optionsPadding),
			verticalAlignment = Alignment.CenterVertically,
		) {
			val isLoggingToFile by hiddenSettingsViewModel.isLoggingToFile.subscribeAsState()
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
