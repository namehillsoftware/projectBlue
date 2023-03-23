package com.lasthopesoftware.bluewater.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
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
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.shared.android.ui.components.LabeledSelection
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions

@Composable
fun ApplicationSettingsView(
	applicationSettingsViewModel: ApplicationSettingsViewModel,
	applicationNavigation: NavigateApplication,
) {
	Surface {
		val rowHeight = dimensionResource(id = R.dimen.standard_row_height)
		val rowFontSize = LocalDensity.current.run { dimensionResource(id = R.dimen.row_font_size).toSp() }

		val libraries by applicationSettingsViewModel.libraries.collectAsState()
		val selectedLibraryId by applicationSettingsViewModel.chosenLibraryId.collectAsState()

		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(Dimensions.ViewPadding)
		) {
			item {
				Row(
					modifier = Modifier.fillMaxWidth(),
				) {
					ProvideTextStyle(value = MaterialTheme.typography.h3) {
						Text(text = stringResource(id = R.string.app_sync_settings))
					}
				}
			}

			item {
				Row(
					modifier = Modifier.fillMaxWidth(),
				) {
					val isSyncOnWifiOnly by applicationSettingsViewModel.isSyncOnWifiOnly.collectAsState()
					LabeledSelection(
						label = stringResource(id = R.string.app_only_sync_on_wifi),
						selected = isSyncOnWifiOnly,
						onSelected = { applicationSettingsViewModel.promiseSyncOnWifiChange(!isSyncOnWifiOnly) }
					) {
						Checkbox(checked = isSyncOnWifiOnly, onCheckedChange = null)
					}
				}
			}

			item {
				Row(
					modifier = Modifier.fillMaxWidth(),
				) {
					val isSyncOnPowerOnly by applicationSettingsViewModel.isSyncOnPowerOnly.collectAsState()
					LabeledSelection(
						label = stringResource(id = R.string.app_only_sync_ext_power),
						selected = isSyncOnPowerOnly,
						onSelected = { applicationSettingsViewModel.promiseSyncOnPowerChange(!isSyncOnPowerOnly) }
					) {
						Checkbox(checked = isSyncOnPowerOnly, onCheckedChange = null)
					}
				}
			}

			item {
				Row(
					modifier = Modifier.fillMaxWidth(),
				) {
					ProvideTextStyle(value = MaterialTheme.typography.h3) {
						Text(text = stringResource(id = R.string.app_audio_settings))
					}
				}
			}

			item {
				Row(
					modifier = Modifier.fillMaxWidth(),
				) {
					val isVolumeLevelingEnabled by applicationSettingsViewModel.isVolumeLevelingEnabled.collectAsState()
					LabeledSelection(
						label = stringResource(id = R.string.useVolumeLevelingSetting),
						selected = isVolumeLevelingEnabled,
						onSelected = { applicationSettingsViewModel.promiseSyncOnPowerChange(!isVolumeLevelingEnabled) }
					) {
						Checkbox(checked = isVolumeLevelingEnabled, onCheckedChange = null)
					}
				}
			}

			item {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.clickable {
							applicationNavigation.viewNewServerSettings()
						},
					horizontalArrangement = Arrangement.SpaceBetween,
				) {
					Text(text = stringResource(id = R.string.btn_add_server))
					Image(
						painter = painterResource(id = R.drawable.ic_add_item_36dp),
						contentDescription = stringResource(id = R.string.btn_add_server)
					)
				}
			}

			items(libraries) { library ->
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.height(rowHeight),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Text(
						text = library.accessCode ?: "",
						modifier = Modifier.weight(1f),
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
						fontWeight = if (library.libraryId == selectedLibraryId) FontWeight.Bold else FontWeight.Normal,
						fontSize = rowFontSize,
					)

					Image(
						painter = painterResource(id = R.drawable.ic_action_settings),
						contentDescription = stringResource(id = R.string.action_settings),
						modifier = Modifier.clickable { applicationNavigation.viewServerSettings(library.libraryId) }
					)

					Button(
						onClick = {
							applicationNavigation.connectToLibrary(library.libraryId)
						}
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
		}
	}
}
