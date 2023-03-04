package com.lasthopesoftware.bluewater.client.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library

@Composable
private fun DataEntryRow(content: @Composable (RowScope.() -> Unit)) {
	Row(
		verticalAlignment = Alignment.CenterVertically
	) {
		content()
	}
}

@Composable
fun LibrarySettingsView(librarySettingsViewModel: LibrarySettingsViewModel) {
	Surface {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			librarySettingsViewModel.apply {
				TextField(
					value = accessCode.collectAsState().value,
					onValueChange = { accessCode.value = it }
				)

				TextField(
					value = userName.collectAsState().value,
					onValueChange = { userName.value = it },
				)

				TextField(
					value = password.collectAsState().value,
					onValueChange = { password.value = it },
					visualTransformation = PasswordVisualTransformation(),
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
				)

				DataEntryRow {
					Checkbox(
						checked = isLocalOnly.collectAsState().value,
						onCheckedChange = { isLocalOnly.value = it },
					)

					Text(text = stringResource(id = R.string.lbl_local_only))
				}

				DataEntryRow {
					Checkbox(
						checked = isWakeOnLanEnabled.collectAsState().value,
						onCheckedChange = { isWakeOnLanEnabled.value = it },
					)

					Text(text = stringResource(id = R.string.wake_on_lan_setting))
				}

				Text(stringResource(id = R.string.lblSyncMusicLocation))

				val syncedFileLocationValue by syncedFileLocation.collectAsState()
				DataEntryRow {
					RadioButton(
						selected = syncedFileLocationValue == Library.SyncedFileLocation.INTERNAL,
						onClick = { syncedFileLocation.value = Library.SyncedFileLocation.INTERNAL },
					)

					Text(stringResource(id = R.string.rbPrivateToApp))
				}

				DataEntryRow {
					RadioButton(
						selected = syncedFileLocationValue == Library.SyncedFileLocation.EXTERNAL,
						onClick = { syncedFileLocation.value = Library.SyncedFileLocation.EXTERNAL },
					)

					Text(stringResource(id = R.string.rbPublicLocation))
				}

				DataEntryRow {
					RadioButton(
						selected = syncedFileLocationValue == Library.SyncedFileLocation.CUSTOM,
						onClick = { syncedFileLocation.value = Library.SyncedFileLocation.CUSTOM },
					)

					Text(stringResource(id = R.string.rbCustomLocation))
				}

				TextField(
					value = customSyncPath.collectAsState().value,
					onValueChange = { customSyncPath.value = it },
					enabled = syncedFileLocationValue == Library.SyncedFileLocation.CUSTOM,
				)

				DataEntryRow {
					Checkbox(
						checked = isUsingLocalConnectionForSync.collectAsState().value,
						onCheckedChange = { isUsingLocalConnectionForSync.value = it },
					)

					Text(stringResource(id = R.string.lbl_sync_local_connection))
				}

				DataEntryRow {
					Checkbox(
						checked = isUsingExistingFiles.collectAsState().value,
						onCheckedChange = { isUsingExistingFiles.value = it },
					)

					Text(stringResource(id = R.string.lbl_use_existing_music))
				}

				var isSaving by mutableStateOf(false)
				TextButton(
					onClick = {
						isSaving = true
						saveLibrary()
					},
					enabled = !isSaving,
				) {
					Text(text = stringResource(id = R.string.btn_save))
				}
			}
		}
	}
}
