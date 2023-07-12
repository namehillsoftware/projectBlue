package com.lasthopesoftware.bluewater.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.android.ui.components.ApplicationInfoText
import com.lasthopesoftware.bluewater.shared.android.ui.components.ApplicationLogo
import com.lasthopesoftware.bluewater.shared.android.ui.components.LabeledSelection
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions

private val optionsPadding = PaddingValues(start = 32.dp, end = 32.dp)

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.settingsList(
	standardRowModifier: Modifier,
	rowFontSize: TextUnit,
	applicationSettingsViewModel: ApplicationSettingsViewModel,
	applicationNavigation: NavigateApplication,
	playbackService: ControlPlaybackService,
	libraries: List<Library>,
	selectedLibraryId: LibraryId,
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
		Row(
			modifier = standardRowModifier.padding(optionsPadding),
			verticalAlignment = Alignment.CenterVertically,
		) {
			val isVolumeLevelingEnabled by applicationSettingsViewModel.isVolumeLevelingEnabled.collectAsState()
			LabeledSelection(
				label = stringResource(id = R.string.useVolumeLevelingSetting),
				selected = isVolumeLevelingEnabled,
				onSelected = { applicationSettingsViewModel.promiseVolumeLevelingEnabledChange(!isVolumeLevelingEnabled) }
			) {
				Checkbox(checked = isVolumeLevelingEnabled, onCheckedChange = null)
			}
		}
	}

	item {
		Row(
			modifier = standardRowModifier
				.clickable {
					applicationNavigation.viewNewServerSettings()
				},
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

	items(libraries) { library ->
		Row(
			modifier = standardRowModifier,
			verticalAlignment = Alignment.CenterVertically,
		) {
			Text(
				text = library.accessCode ?: "",
				modifier = Modifier
					.weight(1f)
					.padding(Dimensions.viewPaddingUnit),
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				fontWeight = if (library.libraryId == selectedLibraryId) FontWeight.Bold else FontWeight.Normal,
				fontSize = rowFontSize,
			)

			Image(
				painter = painterResource(id = R.drawable.ic_action_settings),
				contentDescription = stringResource(id = R.string.settings),
				modifier = Modifier
					.clickable { applicationNavigation.viewServerSettings(library.libraryId) }
					.padding(Dimensions.viewPaddingUnit),
			)

			Row(
				modifier = Modifier
					.clickable { applicationNavigation.viewLibrary(library.libraryId) }
					.padding(Dimensions.viewPaddingUnit),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Text(
					text = stringResource(id = R.string.lbl_connect),
					fontSize = rowFontSize,
				)

				Image(
					painter = painterResource(id = R.drawable.ic_arrow_right),
					contentDescription = stringResource(id = R.string.lbl_connect)
				)
			}
		}
	}

	item {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.padding(top = 48.dp)
		) {
			ApplicationInfoText(
				modifier = Modifier
					.fillMaxWidth()
					.align(Alignment.Center)
			)
		}
	}

	item {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.padding(top = 48.dp),
		) {
			Button(
				modifier = Modifier.align(Alignment.Center),
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
}

@Composable
private fun ApplicationSettingsViewHorizontal(
	applicationSettingsViewModel: ApplicationSettingsViewModel,
	applicationNavigation: NavigateApplication,
	playbackService: ControlPlaybackService,
) {
	Row(
		modifier = Modifier.fillMaxSize(),
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

		val libraries by applicationSettingsViewModel.libraries.collectAsState()
		val selectedLibraryId by applicationSettingsViewModel.chosenLibraryId.collectAsState()

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
				selectedLibraryId
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
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(Dimensions.viewPaddingUnit)
		) {
			ApplicationSettingsViewHorizontal(applicationSettingsViewModel, applicationNavigation, playbackService)
		}
	}
}
