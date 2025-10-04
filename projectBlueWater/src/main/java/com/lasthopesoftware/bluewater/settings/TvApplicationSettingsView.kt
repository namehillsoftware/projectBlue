package com.lasthopesoftware.bluewater.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.components.ApplicationLogo
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.viewPaddingUnit
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun SettingsList(
	applicationSettingsViewModel: ApplicationSettingsViewModel,
	playbackService: ControlPlaybackService
) {
	AudioSettingsSection(applicationSettingsViewModel)

	ThemeSettingsSection(applicationSettingsViewModel)

	AboutApplication(playbackService)
}

@Composable
private fun BoxWithConstraintsScope.ApplicationSettingsViewHorizontal(
	applicationSettingsViewModel: ApplicationSettingsViewModel,
	applicationNavigation: NavigateApplication,
	playbackService: ControlPlaybackService,
) {
	Row(
		modifier = Modifier.fillMaxSize(),
		horizontalArrangement = Arrangement.Start,
	) {
		val libraries by applicationSettingsViewModel.libraries.subscribeAsState()
		val selectedLibraryId by applicationSettingsViewModel.chosenLibraryId.subscribeAsState()

		Column(
			modifier = Modifier.width(this@ApplicationSettingsViewHorizontal.maxHeight)
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.height(Dimensions.appBarHeight)
					.background(MaterialTheme.colors.surface)
			) {
				ProvideTextStyle(MaterialTheme.typography.h4) {
					Text(text = stringResource(id = R.string.app_name))
				}
			}

			ApplicationLogo(modifier = Modifier
				.fillMaxHeight(.5f)
				.align(Alignment.CenterHorizontally)
				.weight(1f)
				.combinedClickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onClick = {},
					onLongClick = { applicationNavigation.viewHiddenSettings() },
				)
			)

			ApplicationSettingsMenu(
				applicationSettingsViewModel,
				applicationNavigation = applicationNavigation,
				selectedLibraryId = selectedLibraryId,
				modifier = Modifier
					.fillMaxWidth()
					.height(expandedMenuHeight)
					.clipToBounds()
					.align(Alignment.Start),
			)
		}

		Column(
			modifier = Modifier
				.fillMaxHeight()
				.padding(start = viewPaddingUnit * 2)
				.verticalScroll(rememberScrollState()),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			val selectedTab by applicationSettingsViewModel.selectedTab.subscribeAsState()
			when (selectedTab) {
				ApplicationSettingsViewModel.SelectedTab.ViewServers -> ServersList(
					applicationNavigation,
					libraries,
					selectedLibraryId,
				)
				ApplicationSettingsViewModel.SelectedTab.ViewSettings -> SettingsList(
					applicationSettingsViewModel,
					playbackService
				)
			}
		}
	}
}

@Composable
fun TvApplicationSettingsView(
	applicationSettingsViewModel: ApplicationSettingsViewModel,
	applicationNavigation: NavigateApplication,
	playbackService: ControlPlaybackService,
) {
	ControlSurface {
		BoxWithConstraints {
			ApplicationSettingsViewHorizontal(applicationSettingsViewModel, applicationNavigation, playbackService)
		}
	}
}
