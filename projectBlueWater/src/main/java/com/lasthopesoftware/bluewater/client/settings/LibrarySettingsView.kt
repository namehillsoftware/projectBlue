package com.lasthopesoftware.bluewater.client.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.connection.trust.ProvideUserSslCertificates
import com.lasthopesoftware.bluewater.shared.android.BuildUndoBackStack
import com.lasthopesoftware.bluewater.shared.android.ui.components.BackButton
import com.lasthopesoftware.bluewater.shared.android.ui.components.ColumnMenuIcon
import com.lasthopesoftware.bluewater.shared.android.ui.components.GradientSide
import com.lasthopesoftware.bluewater.shared.android.ui.components.LabeledSelection
import com.lasthopesoftware.bluewater.shared.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.shared.android.ui.components.StandardTextField
import com.lasthopesoftware.bluewater.shared.android.ui.components.memorableScrollConnectedScaler
import com.lasthopesoftware.bluewater.shared.android.ui.linearInterpolation
import com.lasthopesoftware.bluewater.shared.android.ui.components.rememberTitleStartPadding
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions.menuHeight
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions.topRowOuterPadding
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions.viewPaddingUnit
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsMutableState
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.strings.GetStringResources
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

private val expandedTitleHeight = Dimensions.expandedTitleHeight
private val expandedIconSize = menuHeight
private val appBarHeight = Dimensions.appBarHeight
private val boxHeight = expandedTitleHeight + appBarHeight
private val innerGroupPadding = viewPaddingUnit * 2
private const val inputRowMaxWidth = .8f

@Composable
private fun SpacedOutRow(modifier: Modifier = Modifier, content: @Composable (RowScope.() -> Unit)) {
	Row(
		modifier = Modifier
			.fillMaxWidth(inputRowMaxWidth)
			.height(Dimensions.standardRowHeight)
			.then(modifier),
		verticalAlignment = Alignment.CenterVertically,
	) {
		content()
	}
}


@Composable
private fun RowScope.LabelledChangeServerTypeButton(
	stringResources: GetStringResources,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	ColumnMenuIcon(
		onClick = onClick,
		iconPainter = painterResource(id = R.drawable.select_library_36dp),
		contentDescription = stringResources.changeServerType,
		label = stringResources.changeServerType,
		labelModifier = modifier,
	)
}


@Composable
private fun RowScope.LabelledRemoveServerButton(
	librarySettingsViewModel: LibrarySettingsViewModel,
	stringResources: GetStringResources,
	modifier: Modifier = Modifier,
) {
	ColumnMenuIcon(
		onClick = librarySettingsViewModel::requestLibraryRemoval,
		iconPainter = painterResource(id = R.drawable.ic_remove_item_36dp),
		contentDescription = stringResources.removeServer,
		label = stringResources.removeServer,
		labelModifier = modifier,
	)
}

@Composable
private fun RowScope.LabelledSaveAndConnectButton(
	librarySettingsViewModel: LibrarySettingsViewModel,
	navigateApplication: NavigateApplication,
	stringResources: GetStringResources,
	modifier: Modifier = Modifier,
	focusRequester: FocusRequester? = null
) {
	val isSettingsChanged by librarySettingsViewModel.isSettingsChanged.subscribeAsState()
	val saveAndConnectText by remember {
		derivedStateOf {
			if (isSettingsChanged) stringResources.saveAndConnect
			else stringResources.connect
		}
	}

	val scope = rememberCoroutineScope()
	ColumnMenuIcon(
		onClick = {
			if (isSettingsChanged) {
				scope.launch {
					val isSaved = librarySettingsViewModel.saveLibrary().suspend()
					if (isSaved)
						librarySettingsViewModel.activeLibraryId?.also(navigateApplication::viewLibrary)
				}
			} else {
				librarySettingsViewModel.activeLibraryId?.also(navigateApplication::viewLibrary)
			}
		},
		iconPainter = painterResource(id = R.drawable.arrow_right_24dp),
		contentDescription = saveAndConnectText,
		label = saveAndConnectText,
		labelModifier = modifier,
		focusRequester = focusRequester,
	)
}

@Composable
private fun RemoveServerConfirmationDialog(
	librarySettingsViewModel: LibrarySettingsViewModel,
	navigateApplication: NavigateApplication
) {
	val scope = rememberCoroutineScope()
	val libraryRemovalRequested by librarySettingsViewModel.isRemovalRequested.subscribeAsState()
	val accessCodeState by librarySettingsViewModel.libraryName.subscribeAsState()
	if (libraryRemovalRequested) {
		AlertDialog(
			onDismissRequest = librarySettingsViewModel::cancelLibraryRemovalRequest,
			buttons = {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(viewPaddingUnit),
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
						.padding(viewPaddingUnit)
						.fillMaxWidth()
						.heightIn(100.dp, 300.dp),
					contentAlignment = Alignment.Center
				) {
					Text(stringResource(id = R.string.confirmServerRemoval, accessCodeState))
				}
			}
		)
	}
}

@Composable
private fun LibrarySettingsList(
	librarySettingsViewModel: LibrarySettingsViewModel,
	connectionSettingsViewModel: LibrarySettingsViewModel.MediaCenterConnectionSettingsViewModel,
	stringResources: GetStringResources,
	userSslCertificates: ProvideUserSslCertificates,
) {
	with (librarySettingsViewModel) {
		val isSettingsChanged by isSettingsChanged.subscribeAsState()

		with (connectionSettingsViewModel) {
			var accessCodeState by accessCode.subscribeAsMutableState()
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

			Column(modifier = Modifier.fillMaxWidth(inputRowMaxWidth)) {
				Text(
					text = stringResource(R.string.optional_ssl_certificate),
					modifier = Modifier.padding(top = innerGroupPadding, bottom = innerGroupPadding),
				)

				var hasError by remember { mutableStateOf(false) }
				Row {
					val hasSslCertificate by hasSslCertificate.subscribeAsState()

					Button(
						onClick = {
							sslCertificateFingerprint.value = emptyByteArray
						},
						modifier = Modifier
							.weight(1f)
							.padding(end = innerGroupPadding),
						enabled = hasSslCertificate,
					) {
						Text(text = stringResources.clear)
					}

					val scope = rememberCoroutineScope()
					Button(
						onClick = {
							hasError = false
							scope.launch {
								try {
									val fingerprint =
										userSslCertificates.promiseUserSslCertificateFingerprint().suspend()
									sslCertificateFingerprint.value = fingerprint
								} catch (e: Throwable) {
									hasError = true
								}
							}
						},
						modifier = Modifier
							.weight(1f)
							.padding(start = innerGroupPadding),
					) {
						Text(
							text = stringResources.run { if (hasSslCertificate) change else set }
						)
					}
				}

				if (hasError) {
					ProvideTextStyle(value = MaterialTheme.typography.caption) {
						Text(
							text = stringResource(R.string.invalid_ssl_certificate),
							color = MaterialTheme.colors.error
						)
					}
				}
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

			Column(
				modifier = Modifier
					.fillMaxWidth(inputRowMaxWidth)
					.selectableGroup()
			) {
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

				if (isWolEnabledState) {
					var macAddressState by macAddress.subscribeAsMutableState()
					StandardTextField(
						placeholder = stringResource(R.string.optional_mac_address),
						value = macAddressState,
						onValueChange = { macAddressState = it },
						modifier = Modifier.padding(innerGroupPadding),
					)
				}
			}

			Column(
				modifier = Modifier
					.fillMaxWidth(inputRowMaxWidth)
					.selectableGroup()
			) {
				Text(
					text = stringResource(id = R.string.lblSyncMusicLocation),
					modifier = Modifier.padding(innerGroupPadding),
				)

				var syncedFileLocationState by syncedFileLocation.subscribeAsMutableState()
				Row(
					modifier = Modifier.padding(innerGroupPadding)
				) {
					LabeledSelection(
						label = stringResource(id = R.string.rbPrivateToApp),
						selected = syncedFileLocationState == SyncedFileLocation.INTERNAL,
						onSelected = { syncedFileLocationState = SyncedFileLocation.INTERNAL },
						role = Role.RadioButton,
					) {
						RadioButton(
							selected = syncedFileLocationState == SyncedFileLocation.INTERNAL,
							onClick = null,
						)
					}
				}

				Row(
					modifier = Modifier.padding(innerGroupPadding)
				) {
					LabeledSelection(
						label = stringResource(id = R.string.rbPublicLocation),
						selected = syncedFileLocationState == SyncedFileLocation.EXTERNAL,
						onSelected = { syncedFileLocationState = SyncedFileLocation.EXTERNAL },
						role = Role.RadioButton,
					) {
						RadioButton(
							selected = syncedFileLocationState == SyncedFileLocation.EXTERNAL,
							onClick = null,
						)
					}
				}

				val isStoragePermissionsNeeded by isStoragePermissionsNeeded.subscribeAsState()
				if (isStoragePermissionsNeeded) {
					ProvideTextStyle(value = MaterialTheme.typography.caption) {
						Text(
							text = stringResource(R.string.permissions_must_be_granted_for_settings),
							color = MaterialTheme.colors.error
						)
					}
				}
			}

			SpacedOutRow {
				var isSyncLocalConnectionsOnlyState by isSyncLocalConnectionsOnly.subscribeAsMutableState()
				LabeledSelection(
					label = stringResource(id = R.string.lbl_sync_local_connection),
					selected = isSyncLocalConnectionsOnlyState,
					onSelected = { isSyncLocalConnectionsOnlyState = !isSyncLocalConnectionsOnlyState },
					role = Role.Checkbox,
				) {
					Checkbox(
						checked = isSyncLocalConnectionsOnlyState,
						null,
					)
				}
			}

			SpacedOutRow {
				var isUsingExistingFilesState by isUsingExistingFiles.subscribeAsMutableState()
				LabeledSelection(
					label = stringResource(id = R.string.lbl_use_existing_music),
					selected = isUsingExistingFilesState,
					onSelected = { isUsingExistingFilesState = !isUsingExistingFilesState },
					role = Role.Checkbox,
				) {
					Checkbox(
						checked = isUsingExistingFilesState,
						null,
					)
				}
			}

			val isSavingState by isSaving.subscribeAsState()
			Button(
				onClick = { saveLibrary() },
				enabled = isSettingsChanged && !isSavingState,
			) {
				Text(text = if (!isSettingsChanged) stringResource(id = R.string.saved) else stringResource(id = R.string.save))
			}
		}
	}
}


@Composable
private fun LibrarySettingsList(
	librarySettingsViewModel: LibrarySettingsViewModel,
	connectionSettingsViewModel: LibrarySettingsViewModel.SubsonicConnectionSettingsViewModel,
	stringResources: GetStringResources,
	userSslCertificates: ProvideUserSslCertificates,
) {
	with (librarySettingsViewModel) {
		val isSettingsChanged by isSettingsChanged.subscribeAsState()

		with (connectionSettingsViewModel) {
			var urlState by url.subscribeAsMutableState()
			SpacedOutRow {
				StandardTextField(
					placeholder = stringResource(R.string.url),
					value = urlState,
					onValueChange = { urlState = it }
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

			Column(modifier = Modifier.fillMaxWidth(inputRowMaxWidth)) {
				Text(
					text = stringResource(R.string.optional_ssl_certificate),
					modifier = Modifier.padding(top = innerGroupPadding, bottom = innerGroupPadding),
				)

				var hasError by remember { mutableStateOf(false) }
				Row {
					val hasSslCertificate by hasSslCertificate.subscribeAsState()

					Button(
						onClick = {
							sslCertificateFingerprint.value = emptyByteArray
						},
						modifier = Modifier
							.weight(1f)
							.padding(end = innerGroupPadding),
						enabled = hasSslCertificate,
					) {
						Text(text = stringResources.clear)
					}

					val scope = rememberCoroutineScope()
					Button(
						onClick = {
							hasError = false
							scope.launch {
								try {
									val fingerprint =
										userSslCertificates.promiseUserSslCertificateFingerprint().suspend()
									sslCertificateFingerprint.value = fingerprint
								} catch (e: Throwable) {
									hasError = true
								}
							}
						},
						modifier = Modifier
							.weight(1f)
							.padding(start = innerGroupPadding),
					) {
						Text(
							text = stringResources.run { if (hasSslCertificate) change else set }
						)
					}
				}

				if (hasError) {
					ProvideTextStyle(value = MaterialTheme.typography.caption) {
						Text(
							text = stringResource(R.string.invalid_ssl_certificate),
							color = MaterialTheme.colors.error
						)
					}
				}
			}

			Column(
				modifier = Modifier
					.fillMaxWidth(inputRowMaxWidth)
					.selectableGroup()
			) {
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

				if (isWolEnabledState) {
					var macAddressState by macAddress.subscribeAsMutableState()
					StandardTextField(
						placeholder = stringResource(R.string.optional_mac_address),
						value = macAddressState,
						onValueChange = { macAddressState = it },
						modifier = Modifier.padding(innerGroupPadding),
					)
				}
			}

			Column(
				modifier = Modifier
					.fillMaxWidth(inputRowMaxWidth)
					.selectableGroup()
			) {
				Text(
					text = stringResource(id = R.string.lblSyncMusicLocation),
					modifier = Modifier.padding(innerGroupPadding),
				)

				var syncedFileLocationState by syncedFileLocation.subscribeAsMutableState()
				Row(
					modifier = Modifier.padding(innerGroupPadding)
				) {
					LabeledSelection(
						label = stringResource(id = R.string.rbPrivateToApp),
						selected = syncedFileLocationState == SyncedFileLocation.INTERNAL,
						onSelected = { syncedFileLocationState = SyncedFileLocation.INTERNAL },
						role = Role.RadioButton,
					) {
						RadioButton(
							selected = syncedFileLocationState == SyncedFileLocation.INTERNAL,
							onClick = null,
						)
					}
				}

				Row(
					modifier = Modifier.padding(innerGroupPadding)
				) {
					LabeledSelection(
						label = stringResource(id = R.string.rbPublicLocation),
						selected = syncedFileLocationState == SyncedFileLocation.EXTERNAL,
						onSelected = { syncedFileLocationState = SyncedFileLocation.EXTERNAL },
						role = Role.RadioButton,
					) {
						RadioButton(
							selected = syncedFileLocationState == SyncedFileLocation.EXTERNAL,
							onClick = null,
						)
					}
				}

				val isStoragePermissionsNeeded by isStoragePermissionsNeeded.subscribeAsState()
				if (isStoragePermissionsNeeded) {
					ProvideTextStyle(value = MaterialTheme.typography.caption) {
						Text(
							text = stringResource(R.string.permissions_must_be_granted_for_settings),
							color = MaterialTheme.colors.error
						)
					}
				}
			}

			SpacedOutRow {
				var isUsingExistingFilesState by isUsingExistingFiles.subscribeAsMutableState()
				LabeledSelection(
					label = stringResource(id = R.string.lbl_use_existing_music),
					selected = isUsingExistingFilesState,
					onSelected = { isUsingExistingFilesState = !isUsingExistingFilesState },
					role = Role.Checkbox,
				) {
					Checkbox(
						checked = isUsingExistingFilesState,
						null,
					)
				}
			}

			val isSavingState by isSaving.subscribeAsState()
			Button(
				onClick = { saveLibrary() },
				enabled = isSettingsChanged && !isSavingState,
			) {
				Text(text = if (!isSettingsChanged) stringResource(id = R.string.saved) else stringResource(id = R.string.save))
			}
		}
	}
}


@Composable
fun ServerTypeSelection(
	librarySettingsViewModel: LibrarySettingsViewModel,
	modifier: Modifier = Modifier,
	onServerTypeSelectionFinished: () -> Unit = {},
) {
	BackHandler { onServerTypeSelectionFinished() }

	Box(
		modifier = Modifier.fillMaxSize().then(modifier),
		contentAlignment = Alignment.Center,
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth(inputRowMaxWidth)
				.selectableGroup(),
		) {
			Text(
				text = "Server Type",
				modifier = Modifier.padding(innerGroupPadding),
			)

			val isLoadingState by librarySettingsViewModel.isLoading.subscribeAsState()

			var connectionSettingsViewModelState by librarySettingsViewModel.connectionSettingsViewModel.subscribeAsMutableState()
			for (connectionType in librarySettingsViewModel.availableConnectionSettings) {
				Box(
					modifier = Modifier.padding(innerGroupPadding),
				) {
					LabeledSelection(
						label = connectionType.connectionTypeName,
						selected = connectionSettingsViewModelState == connectionType,
						onSelected = { connectionSettingsViewModelState = connectionType },
						enabled = !isLoadingState,
						role = Role.RadioButton,
					) {
						RadioButton(
							selected = connectionSettingsViewModelState == connectionType,
							onClick = null,
						)
					}
				}
			}

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
			) {
				val scope = rememberCoroutineScope()
				val isSettingsChanged by librarySettingsViewModel.isSettingsChanged.subscribeAsState()
				val isSaving by librarySettingsViewModel.isSaving.subscribeAsState()
				Button(
					onClick = {
						scope.launch {
							librarySettingsViewModel.saveLibrary().suspend()
							onServerTypeSelectionFinished()
						}
					},
					enabled = isSettingsChanged && !isSaving,
				) {
					Text(text = if (!isSettingsChanged) stringResource(id = R.string.saved) else stringResource(id = R.string.save))
				}

				Button(
					onClick = onServerTypeSelectionFinished,
				) {
					Text(text = stringResource(id = R.string.btn_cancel))
				}
			}
		}
	}
}


@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun LibrarySettingsView(
	librarySettingsViewModel: LibrarySettingsViewModel,
	navigateApplication: NavigateApplication,
	stringResources: GetStringResources,
	userSslCertificates: ProvideUserSslCertificates,
	undoBackStack: BuildUndoBackStack,
) {
	ControlSurface {
		RemoveServerConfirmationDialog(
			librarySettingsViewModel = librarySettingsViewModel,
			navigateApplication = navigateApplication
		)

		val boxHeightPx = LocalDensity.current.run { boxHeight.toPx() }
		val collapsedHeightPx = LocalDensity.current.run { appBarHeight.toPx() }
		val heightScaler = memorableScrollConnectedScaler(boxHeightPx, collapsedHeightPx)

		val isLoadingState by librarySettingsViewModel.isLoading.subscribeAsState()
		var isSelectingServerType by remember { mutableStateOf(false) }

		BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
			val isHeaderTall by remember { derivedStateOf { (boxHeight + menuHeight) * 2 < maxHeight } }

			Column(
				modifier = Modifier
					.fillMaxSize()
					.nestedScroll(heightScaler)
			) {
				val scrollState = rememberScrollState()

				val scope = rememberCoroutineScope()

				val headerCollapseProgress by heightScaler.getProgressState()

				val isIconsVisible by LocalDensity.current.run {
					remember {
						derivedStateOf { scrollState.value < expandedIconSize.toPx() }
					}
				}
				val connectFocusRequester = remember { FocusRequester() }

				val inputMode = LocalInputModeManager.current
				DisposableEffect(isIconsVisible, inputMode, heightScaler) {
					if (isIconsVisible) {
						onDispose { }
					} else {
						val scrollToTopAction = {
							scope.async {
								heightScaler.goToMax()
								scrollState.scrollTo(0)
								if (inputMode.inputMode == InputMode.Keyboard)
									connectFocusRequester.requestFocus()
								true
							}.toPromise()
						}

						undoBackStack.addAction(scrollToTopAction)

						onDispose {
							undoBackStack.removeAction(scrollToTopAction)
						}
					}
				}

				if (isHeaderTall) {
					val heightValue by heightScaler.getValueState()

					Box(
						modifier = Modifier
							.height(LocalDensity.current.run { heightValue.toDp() })
							.fillMaxWidth()
							.background(MaterialTheme.colors.surface)
					) {
						ProvideTextStyle(MaterialTheme.typography.h5) {
							val topPadding by remember {
								derivedStateOf {
									linearInterpolation(
										Dimensions.appBarHeight,
										14.dp,
										headerCollapseProgress
									)
								}
							}

							val startPadding by rememberTitleStartPadding(heightScaler.getProgressState())
							val endPadding = viewPaddingUnit
							MarqueeText(
								text = stringResource(id = R.string.settings),
								overflow = TextOverflow.Ellipsis,
								gradientSides = setOf(GradientSide.End),
								gradientEdgeColor = MaterialTheme.colors.surface,
								modifier = Modifier
									.fillMaxWidth()
									.padding(start = startPadding, end = endPadding, top = topPadding),
							)
						}

						BackButton(
							navigateApplication::navigateUp,
							modifier = Modifier
								.align(Alignment.TopStart)
								.padding(topRowOuterPadding)
						)
					}
				} else {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.background(MaterialTheme.colors.surface)
							.height(appBarHeight),
						verticalAlignment = Alignment.CenterVertically,
					) {
						BackButton(
							navigateApplication::navigateUp,
							modifier = Modifier.padding(horizontal = topRowOuterPadding)
						)

						ProvideTextStyle(MaterialTheme.typography.h5) {
							MarqueeText(
								text = stringResource(id = R.string.settings),
								overflow = TextOverflow.Ellipsis,
								gradientSides = setOf(GradientSide.End),
								gradientEdgeColor = MaterialTheme.colors.surface,
								modifier = Modifier
									.fillMaxWidth()
									.padding(horizontal = viewPaddingUnit)
									.weight(1f),
							)
						}
					}
				}

				Column(
					modifier = Modifier.verticalScroll(scrollState),
					horizontalAlignment = Alignment.CenterHorizontally,
				) {
					Row(
						modifier = Modifier
							.padding(Dimensions.rowPadding)
							.fillMaxWidth()
							.height(expandedIconSize)
					) {
						LabelledChangeServerTypeButton(
							stringResources = stringResources,
							onClick = { isSelectingServerType = true },
						)

						LabelledRemoveServerButton(
							librarySettingsViewModel = librarySettingsViewModel,
							stringResources = stringResources,
						)

						LabelledSaveAndConnectButton(
							librarySettingsViewModel = librarySettingsViewModel,
							navigateApplication = navigateApplication,
							stringResources = stringResources,
							focusRequester = connectFocusRequester,
						)
					}

					if (!isLoadingState) {
						if (isSelectingServerType) {
							ServerTypeSelection(
								librarySettingsViewModel = librarySettingsViewModel,
								onServerTypeSelectionFinished = { isSelectingServerType = false },
								modifier = Modifier.weight(1f),
							)
						} else {
							val connectionSettingsViewModelState by librarySettingsViewModel.savedConnectionSettingsViewModel.subscribeAsState()

							when (val connectionSettingsViewModel = connectionSettingsViewModelState) {
								is LibrarySettingsViewModel.MediaCenterConnectionSettingsViewModel -> LibrarySettingsList(
									librarySettingsViewModel = librarySettingsViewModel,
									connectionSettingsViewModel = connectionSettingsViewModel,
									stringResources = stringResources,
									userSslCertificates = userSslCertificates,
								)

								is LibrarySettingsViewModel.SubsonicConnectionSettingsViewModel -> LibrarySettingsList(
									librarySettingsViewModel = librarySettingsViewModel,
									connectionSettingsViewModel = connectionSettingsViewModel,
									stringResources = stringResources,
									userSslCertificates = userSslCertificates,
								)

								null -> ServerTypeSelection(
									librarySettingsViewModel = librarySettingsViewModel,
									onServerTypeSelectionFinished = { isSelectingServerType = false }
								)
							}
						}
					}
				}
			}
		}
	}
}
