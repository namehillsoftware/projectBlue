package com.lasthopesoftware.bluewater.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.components.ApplicationInfoText
import com.lasthopesoftware.bluewater.android.ui.components.ApplicationLogo
import com.lasthopesoftware.bluewater.android.ui.components.LabeledSelection
import com.lasthopesoftware.bluewater.android.ui.navigable
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState

private val horizontalOptionsPadding = 32.dp

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
private fun LazyListScope.settingsList(
	standardRowModifier: Modifier,
	rowFontSize: TextUnit,
	applicationSettingsViewModel: ApplicationSettingsViewModel,
	applicationNavigation: NavigateApplication,
	playbackService: ControlPlaybackService,
	libraries: List<Pair<LibraryId, String>>,
	selectedLibraryId: LibraryId?,
	isLoading: Boolean
) {
	stickyHeader {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.background(MaterialTheme.colors.surface)
		) {
			ProvideTextStyle(MaterialTheme.typography.h5) {
				Text(text = stringResource(id = R.string.app_name))
			}
		}
	}

	item {
		Row(
			modifier = standardRowModifier,
			verticalAlignment = Alignment.CenterVertically,
		) {
			ProvideTextStyle(value = MaterialTheme.typography.h6) {
				Text(text = stringResource(id = R.string.app_audio_settings))
			}
		}
	}

	item {
		Column(
			modifier = standardRowModifier.padding(horizontal = horizontalOptionsPadding),
			verticalArrangement = Arrangement.SpaceEvenly,
			horizontalAlignment = Alignment.Start,
		) {
			val isVolumeLevelingEnabled by applicationSettingsViewModel.isVolumeLevelingEnabled.subscribeAsState()
			LabeledSelection(
                label = stringResource(id = R.string.use_volume_leveling_setting),
                selected = isVolumeLevelingEnabled,
                onSelected = { applicationSettingsViewModel.promiseVolumeLevelingEnabledChange(!isVolumeLevelingEnabled) },
                {
                    Checkbox(checked = isVolumeLevelingEnabled, onCheckedChange = null, enabled = !isLoading)
                }
            )

            Box(
				modifier = standardRowModifier.padding(start = horizontalOptionsPadding),
				contentAlignment = Alignment.CenterStart,
			) {
				val isPeakLevelNormalizeEnabled by applicationSettingsViewModel.isPeakLevelNormalizeEnabled.subscribeAsState()
				val isPeakLevelNormalizeEditable by applicationSettingsViewModel.isPeakLevelNormalizeEditable.subscribeAsState()
				LabeledSelection(
                    label = stringResource(id = R.string.enable_peak_level_normalize),
                    selected = isPeakLevelNormalizeEnabled,
                    onSelected = { applicationSettingsViewModel.promisePeakLevelNormalizeEnabledChange(!isPeakLevelNormalizeEnabled) },
                    {
                        Checkbox(checked = isPeakLevelNormalizeEnabled, onCheckedChange = null, enabled = isPeakLevelNormalizeEditable && !isLoading)
                    }
                )
            }
		}
	}

	item {
		Row(
			modifier = standardRowModifier
				.navigable(
					onClick = { applicationNavigation.viewNewServerSettings() },
				),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween,
		) {
			Text(
				text = stringResource(id = R.string.btn_add_server),
				fontSize = rowFontSize,
			)
			Image(
				painter = painterResource(id = R.drawable.ic_add_item_36dp),
				contentDescription = stringResource(id = R.string.btn_add_server)
			)
		}
	}

	items(libraries) { (libraryId, name) ->
		Row(
			modifier = standardRowModifier
				.navigable(onClick =  { applicationNavigation.viewServerSettings(libraryId) }),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Text(
				text = name,
				modifier = Modifier
					.weight(1f)
					.padding(Dimensions.viewPaddingUnit),
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				fontWeight = if (libraryId == selectedLibraryId) FontWeight.Bold else FontWeight.Normal,
				fontSize = rowFontSize,
			)

			Row(
				modifier = Modifier.padding(Dimensions.viewPaddingUnit),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Icon(
					painter = painterResource(id = R.drawable.ic_action_settings),
					contentDescription = stringResource(id = R.string.settings)
				)
			}
		}
	}

	item {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.padding(top = Dimensions.viewPaddingUnit)
		) {
			ApplicationInfoText(
				versionName = BuildConfig.VERSION_NAME,
				versionCode = BuildConfig.VERSION_CODE,
				modifier = Modifier
					.fillMaxWidth()
					.align(Alignment.Center)
			)
		}
	}

	item {
		Button(
			modifier = Modifier.padding(top = Dimensions.viewPaddingUnit),
			onClick = {
				playbackService.kill()
			}
		) {
			Text(
				text = stringResource(id = R.string.kill_playback),
				fontSize = rowFontSize,
			)
		}
	}
}

@Composable
private fun ApplicationSettingsViewHorizontal(
	applicationSettingsViewModel: ApplicationSettingsViewModel,
	applicationNavigation: NavigateApplication,
	playbackService: ControlPlaybackService,
) {
	Row(
		modifier = Modifier.fillMaxSize().padding(Dimensions.viewPaddingUnit),
		horizontalArrangement = Arrangement.SpaceEvenly,
	) {
		ApplicationLogo(
			modifier = Modifier
				.fillMaxHeight(.75f)
				.align(Alignment.CenterVertically)
		)

		val rowHeight = Dimensions.standardRowHeight
		val rowFontSize = LocalDensity.current.run { dimensionResource(id = R.dimen.row_font_size).toSp() }

		val standardRowModifier = Modifier
			.fillMaxWidth()
			.height(rowHeight)

		val libraries by applicationSettingsViewModel.libraries.subscribeAsState()
		val selectedLibraryId by applicationSettingsViewModel.chosenLibraryId.subscribeAsState()
		val isLoading by applicationSettingsViewModel.isLoading.subscribeAsState()

		LazyColumn(
			modifier = Modifier
				.fillMaxHeight()
				.padding(start = Dimensions.viewPaddingUnit * 2)
		) {
			settingsList(
				standardRowModifier,
				rowFontSize,
				applicationSettingsViewModel,
				applicationNavigation,
				playbackService,
				libraries,
				selectedLibraryId,
				isLoading
			)
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
		ApplicationSettingsViewHorizontal(applicationSettingsViewModel, applicationNavigation, playbackService)
	}
}
