package com.lasthopesoftware.bluewater.client.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.shared.android.viewmodels.collectAsMutableState

@Composable
private fun DataEntryRow(content: @Composable() (RowScope.() -> Unit)) {
	Row(
		modifier = Modifier
			.fillMaxWidth(.8f)
			.height(dimensionResource(id = R.dimen.standard_row_height)),
		verticalAlignment = Alignment.CenterVertically,
	) {
		content()
	}
}

@Composable
fun LibrarySettingsView(librarySettingsViewModel: LibrarySettingsViewModel) {
	Surface {
		Column(
			modifier = Modifier.fillMaxSize(),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			librarySettingsViewModel.apply {
				DataEntryRow {
					var accessCodeState by accessCode.collectAsMutableState()
					TextField(
						modifier = Modifier.fillMaxWidth(),
						value = accessCodeState,
						onValueChange = { accessCodeState = it }
					)
				}

				DataEntryRow {
					var userNameState by userName.collectAsMutableState()
					TextField(
						modifier = Modifier.fillMaxWidth(),
						value = userNameState,
						onValueChange = { userNameState = it },
					)
				}

				DataEntryRow {
					var passwordState by password.collectAsMutableState()
					TextField(
						modifier = Modifier.fillMaxWidth(),
						value = passwordState,
						onValueChange = { passwordState = it },
						visualTransformation = PasswordVisualTransformation(),
						keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
					)
				}

				DataEntryRow {
					var isLocalOnlyState by isLocalOnly.collectAsMutableState()
					Checkbox(
						checked = isLocalOnlyState,
						onCheckedChange = { isLocalOnlyState = it },
					)

					Text(text = stringResource(id = R.string.lbl_local_only))
				}

				DataEntryRow {
					var isWolEnabledState by isWakeOnLanEnabled.collectAsMutableState()
					Checkbox(
						checked = isWolEnabledState,
						onCheckedChange = { isWolEnabledState = it },
					)

					Text(text = stringResource(id = R.string.wake_on_lan_setting))
				}

				Text(stringResource(id = R.string.lblSyncMusicLocation))

				var syncedFileLocationState by syncedFileLocation.collectAsMutableState()
				DataEntryRow {
					RadioButton(
						selected = syncedFileLocationState == Library.SyncedFileLocation.INTERNAL,
						onClick = { syncedFileLocationState = Library.SyncedFileLocation.INTERNAL },
					)

					Text(stringResource(id = R.string.rbPrivateToApp))
				}

				DataEntryRow {
					RadioButton(
						selected = syncedFileLocationState == Library.SyncedFileLocation.EXTERNAL,
						onClick = { syncedFileLocationState = Library.SyncedFileLocation.EXTERNAL },
					)

					Text(stringResource(id = R.string.rbPublicLocation))
				}

				DataEntryRow {
					RadioButton(
						selected = syncedFileLocationState == Library.SyncedFileLocation.CUSTOM,
						onClick = { syncedFileLocationState = Library.SyncedFileLocation.CUSTOM },
					)

					Text(stringResource(id = R.string.rbCustomLocation))
				}

				DataEntryRow {
					var customSyncPathState by customSyncPath.collectAsMutableState()
					TextField(
						modifier = Modifier.fillMaxWidth(),
						value = customSyncPathState,
						onValueChange = { customSyncPathState = it },
						enabled = syncedFileLocationState == Library.SyncedFileLocation.CUSTOM,
					)
				}

				DataEntryRow {
					var isSyncLocalConnectionsOnlyState by isSyncLocalConnectionsOnly.collectAsMutableState()
					Checkbox(
						checked = isSyncLocalConnectionsOnlyState,
						onCheckedChange = { isSyncLocalConnectionsOnlyState = it },
					)

					Text(stringResource(id = R.string.lbl_sync_local_connection))
				}

				DataEntryRow {
					var isUsingExistingFilesState by isUsingExistingFiles.collectAsMutableState()
					Checkbox(
						checked = isUsingExistingFilesState,
						onCheckedChange = { isUsingExistingFilesState = it },
					)

					Text(stringResource(id = R.string.lbl_use_existing_music))
				}

				DataEntryRow {
					val isSavingState by isSaving.collectAsState()
					var isSaved by remember { mutableStateOf(false) }
					Button(
						onClick = {
							saveLibrary()
								.then { isSaved = true }
						},
						enabled = !isSavingState && !isSaved,
					) {
						Text(text = if (isSaved) stringResource(id = R.string.btn_saved) else stringResource(id = R.string.btn_save))
					}
				}
			}
		}
	}
}
