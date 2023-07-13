package com.lasthopesoftware.bluewater.client.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.shared.android.ui.components.LabeledSelection
import com.lasthopesoftware.bluewater.shared.android.ui.components.StandardTextField
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsMutableState
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.resources.strings.GetStringResources
import kotlinx.coroutines.launch

@Composable
private fun SpacedOutRow(content: @Composable (RowScope.() -> Unit)) {
	Row(
		modifier = Modifier
			.fillMaxWidth(.8f)
			.height(Dimensions.standardRowHeight),
		verticalAlignment = Alignment.CenterVertically,
	) {
		content()
	}
}

@Composable
fun TvLibrarySettingsView(
	librarySettingsViewModel: LibrarySettingsViewModel,
	navigateApplication: NavigateApplication,
	stringResources: GetStringResources,
) {
	ControlSurface {
		var accessCodeState by librarySettingsViewModel.accessCode.subscribeAsMutableState()

		val scope = rememberCoroutineScope()
		val libraryRemovalRequested by librarySettingsViewModel.isRemovalRequested.subscribeAsState()
		if (libraryRemovalRequested) {
			AlertDialog(
				onDismissRequest = librarySettingsViewModel::cancelLibraryRemovalRequest,
				buttons = {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(Dimensions.viewPaddingUnit),
						horizontalArrangement = Arrangement.SpaceEvenly,
					) {
						Button(
							onClick = librarySettingsViewModel::cancelLibraryRemovalRequest,
						) {
							Text(text = stringResource(id = R.string.no))
						}

						Button(
							onClick = {
								scope.launch {
									librarySettingsViewModel.removeLibrary().suspend()
									navigateApplication.navigateUp()
								}
							},
						) {
							Text(text = stringResource(id = R.string.yes))
						}
					}
				},
				title = {
					Text(
						text = stringResource(id = R.string.remove_server),
					)
				},
				text = {
					Box(
						modifier = Modifier
							.padding(Dimensions.viewPaddingUnit)
							.fillMaxWidth()
							.heightIn(100.dp, 300.dp),
						contentAlignment = Alignment.Center
					) {
						Text(stringResource(id = R.string.confirmServerRemoval, accessCodeState))
					}
				}
			)
		}

		Box(
			modifier = Modifier
				.fillMaxSize()
		) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(rememberScrollState()),
				horizontalAlignment = Alignment.CenterHorizontally,
			) {
				librarySettingsViewModel.apply {
					SpacedOutRow {
						StandardTextField(
							placeholder = stringResource(R.string.lbl_access_code),
							value = accessCodeState,
							onValueChange = { accessCodeState = it }
						)
					}

					SpacedOutRow {
						var userNameState by userName.subscribeAsMutableState()
						StandardTextField(
							placeholder = stringResource(R.string.lbl_user_name),
							value = userNameState,
							onValueChange = { userNameState = it }
						)
					}

					SpacedOutRow {
						var passwordState by password.subscribeAsMutableState()
						StandardTextField(
							placeholder = stringResource(R.string.lbl_password),
							value = passwordState,
							onValueChange = { passwordState = it },
							visualTransformation = PasswordVisualTransformation(),
							keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
						)
					}

					SpacedOutRow {
						var libraryNameState by libraryName.subscribeAsMutableState()
						StandardTextField(
							placeholder = stringResource(R.string.lbl_library_name),
							value = libraryNameState,
							onValueChange = { libraryNameState = it },
						)
					}

					SpacedOutRow {
						var isLocalOnlyState by isLocalOnly.subscribeAsMutableState()
						LabeledSelection(
							label = stringResource(id = R.string.lbl_local_only),
							selected = isLocalOnlyState,
							onSelected = { isLocalOnlyState = !isLocalOnlyState },
							role = Role.Checkbox,
						) {
							Checkbox(
								checked = isLocalOnlyState,
								onCheckedChange = null,
							)
						}
					}

					SpacedOutRow {
						var isWolEnabledState by isWakeOnLanEnabled.subscribeAsMutableState()
						LabeledSelection(
							label = stringResource(id = R.string.wake_on_lan_setting),
							selected = isWolEnabledState,
							onSelected = { isWolEnabledState = !isWolEnabledState },
							role = Role.Checkbox,
						) {
							Checkbox(
								checked = isWolEnabledState,
								onCheckedChange = null,
							)
						}
					}

					val isSavingState by isSaving.subscribeAsState()
					var isSaved by remember { mutableStateOf(false) }
					Button(
						onClick = {
							scope.launch {
								isSaved = saveLibrary().suspend()
								if (isSaved)
									navigateApplication.navigateUp()
							}
						},
						enabled = !isSavingState && !isSaved,
					) {
						Text(text = if (isSaved) stringResource(id = R.string.saved) else stringResource(id = R.string.save))
					}
				}
			}
		}
	}
}
