package com.lasthopesoftware.bluewater.client.settings

import VerticalHeaderScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.calculateSummaryColumnWidth
import com.lasthopesoftware.bluewater.android.ui.components.BackButton
import com.lasthopesoftware.bluewater.android.ui.components.ColumnMenuIcon
import com.lasthopesoftware.bluewater.android.ui.components.ConsumableConnectedScaler
import com.lasthopesoftware.bluewater.android.ui.components.ConsumableConnectedScaler.Companion.offsetLayout
import com.lasthopesoftware.bluewater.android.ui.components.DeferredPreScrollConnectedScaler
import com.lasthopesoftware.bluewater.android.ui.components.GradientSide
import com.lasthopesoftware.bluewater.android.ui.components.LabeledSelection
import com.lasthopesoftware.bluewater.android.ui.components.ListMenuRow
import com.lasthopesoftware.bluewater.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.android.ui.components.MenuIcon
import com.lasthopesoftware.bluewater.android.ui.components.StandardTextField
import com.lasthopesoftware.bluewater.android.ui.components.ignoreConsumedOffset
import com.lasthopesoftware.bluewater.android.ui.components.linkedTo
import com.lasthopesoftware.bluewater.android.ui.components.rememberTitleStartPadding
import com.lasthopesoftware.bluewater.android.ui.navigable
import com.lasthopesoftware.bluewater.android.ui.remember
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.rowPadding
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topMenuHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topMenuIconWidth
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topRowOuterPadding
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.viewPaddingUnit
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.connection.trust.ProvideUserSslCertificates
import com.lasthopesoftware.bluewater.shared.android.UndoStack
import com.lasthopesoftware.observables.subscribeAsMutableState
import com.lasthopesoftware.observables.subscribeAsState
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.strings.GetStringResources
import com.lasthopesoftware.view.ScreenDimensionsScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val appBarHeight = Dimensions.appBarHeight
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
private fun LabelledChangeServerTypeButton(
	stringResources: GetStringResources,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	ColumnMenuIcon(
		onClick = onClick,
		iconPainter = painterResource(id = R.drawable.select_library_36dp),
		contentDescription = stringResources.changeServerType,
		modifier = modifier,
		label = stringResources.changeServerType,
		labelMaxLines = 2,
	)
}


@Composable
private fun LabelledRemoveServerButton(
	librarySettingsViewModel: LibrarySettingsViewModel,
	stringResources: GetStringResources,
	modifier: Modifier = Modifier,
) {
	ColumnMenuIcon(
		onClick = librarySettingsViewModel::requestLibraryRemoval,
		iconPainter = painterResource(id = R.drawable.remove_item_36dp),
		contentDescription = stringResources.removeServer,
		modifier = modifier,
		label = stringResources.removeServer,
	)
}


@Composable
private fun LabelledSaveAndTestButton(
	librarySettingsViewModel: LibrarySettingsViewModel,
	stringResources: GetStringResources,
	modifier: Modifier = Modifier,
) {
	val isSettingsChanged by librarySettingsViewModel.isSettingsChanged.subscribeAsState()
	val isTestingConnection by librarySettingsViewModel.isTestingConnection.subscribeAsState()
	val connectionStatusText by librarySettingsViewModel.connectionStatus.subscribeAsState()
	val saveAndTestText by remember {
		derivedStateOf {
			when {
				isSettingsChanged -> stringResources.saveAndTestConnection
				connectionStatusText.isNotEmpty() -> connectionStatusText
				else -> stringResources.testConnection
			}
		}
	}

	var promisedSaveAndTest by remember { mutableStateOf(Unit.toPromise()) }

	val buttonIcon by remember {
		derivedStateOf {
			if (isTestingConnection) R.drawable.cancel_36dp
			else R.drawable.refresh_36
		}
	}

	ColumnMenuIcon(
		onClick = {
			if (!isTestingConnection) {
				promisedSaveAndTest = librarySettingsViewModel.saveAndTestLibrary()
			} else {
				promisedSaveAndTest.cancel()
			}
		},
		iconPainter = painterResource(id = buttonIcon),
		contentDescription = saveAndTestText,
		label = saveAndTestText,
		labelModifier = modifier,
		labelMaxLines = 2,
		enabled = !isTestingConnection
	)
}


@Composable
private fun LabelledSaveAndConnectButton(
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
			if (librarySettingsViewModel.isSettingsChanged.value) {
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
		modifier = modifier,
		label = saveAndConnectText,
		labelMaxLines = 2,
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
private fun CustomHeadersList(
	connectionSettingsViewModel: LibrarySettingsViewModel.ConnectionSettingsViewModel<*>,
) {
	with(connectionSettingsViewModel) {
		val isHeaderChanged by isHeaderChanged.subscribeAsState()
		val isEditingHeader by isEditingHeader.subscribeAsState()

		if (isEditingHeader) {
			Dialog(::cancelHeaderEdit) {
				ControlSurface {
					Column(
						modifier = Modifier.padding(viewPaddingUnit * 2),
					) {
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(viewPaddingUnit)
						) {
							ProvideTextStyle(MaterialTheme.typography.h5) {
								Text(
									text = "Edit Header",
									modifier = Modifier
										.weight(1f)
										.align(Alignment.CenterVertically),
								)
							}

							Icon(
								painter = painterResource(id = R.drawable.cancel_36dp),
								contentDescription = stringResource(id = R.string.btn_cancel),
								modifier = Modifier
									.navigable(onClick = ::cancelHeaderEdit)
									.align(Alignment.CenterVertically),
							)
						}

						val fieldFocusRequester = remember { FocusRequester() }
						var editingHeaderKey by editingHeaderKey.subscribeAsMutableState()
						var editingHeaderValue by editingHeaderValue.subscribeAsMutableState()
						Column(
							modifier = Modifier
								.padding(viewPaddingUnit)
								.fillMaxWidth()
								.heightIn(100.dp, 450.dp),
							horizontalAlignment = Alignment.CenterHorizontally
						) {
							val verticalPadding = 2.dp
							StandardTextField(
								value = editingHeaderKey,
								placeholder = stringResource(R.string.header),
								onValueChange = { editingHeaderKey = it },
								modifier = Modifier.focusRequester(fieldFocusRequester).padding(vertical = verticalPadding),
							)

							StandardTextField(
								value = editingHeaderValue,
								placeholder = stringResource(R.string.value),
								onValueChange = { editingHeaderValue = it },
								modifier = Modifier.focusRequester(fieldFocusRequester).padding(vertical = verticalPadding),
							)
						}

						Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(rowPadding)
						) {
							MenuIcon(
								onClick = {
									removeHeader()
									cancelHeaderEdit()
								},
								iconPainter = painterResource(id = R.drawable.remove_item_36dp),
								iconModifier = Modifier.size(Dimensions.listItemMenuIconSize),
								contentDescription = stringResource(id = R.string.btn_remove_header),
								modifier = Modifier
									.fillMaxWidth()
									.weight(1f)
									.align(Alignment.CenterVertically)
									.size(Dimensions.listItemMenuIconSize),
							)

							MenuIcon(
								onClick = {
									saveHeader()
									cancelHeaderEdit()
								},
								iconPainter = painterResource(id = R.drawable.ic_save_white_36dp),
								iconModifier = Modifier.size(Dimensions.listItemMenuIconSize),
								contentDescription = stringResource(id = R.string.save),
								enabled = isHeaderChanged,
								modifier = Modifier
									.fillMaxWidth()
									.weight(1f)
									.align(Alignment.CenterVertically),
							)
						}
					}
				}
			}
		}

		Column(
			modifier = Modifier.padding(innerGroupPadding)
		) {
			val customHeaders by customHeaders.subscribeAsState()
			val itemPadding = 2.dp
			for ((key, value) in customHeaders) {
				Row(
					modifier = Modifier
						.navigable(
							onClick = { editHeader(key) },
						)
						.fillMaxWidth(),
				) {
					Text(
						text = key,
						modifier = Modifier
							.weight(1f)
							.padding(itemPadding),
					)

					Text(
						text = value,
						modifier = Modifier
							.weight(2f)
							.padding(itemPadding),
					)
				}
			}

			Row(
				modifier = Modifier
					.navigable(
						onClick = { editHeader("") },
					)
					.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
			) {
				MenuIcon(
					onClick = { editHeader("") },
					iconPainter = painterResource(id = R.drawable.ic_add_item_36dp),
					contentDescription = stringResource(id = R.string.btn_add_header),
					modifier = Modifier
						.padding(itemPadding)
						.fillMaxWidth()
						.weight(1f)
				)

				Text(
					text = stringResource(id = R.string.btn_add_header),
					modifier = Modifier
						.weight(2f)
						.padding(itemPadding),
				)
			}
		}
	}
}


@Composable
private fun LibrarySettingsList(
	librarySettingsViewModel: LibrarySettingsViewModel,
	connectionSettingsViewModel: LibrarySettingsViewModel.MediaCenterConnectionSettingsViewModel,
	stringResources: GetStringResources,
	userSslCertificates: ProvideUserSslCertificates,
) {
	with(librarySettingsViewModel) {
		val isSettingsChanged by isSettingsChanged.subscribeAsState()

		with(connectionSettingsViewModel) {
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
								} catch (_: Throwable) {
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
                    {
                        Checkbox(
                            checked = isLocalOnlyState,
                            onCheckedChange = null,
                        )
                    },
                    role = Role.Checkbox,
				)
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
                    {
                        Checkbox(
                            checked = isWolEnabledState,
                            onCheckedChange = null,
                        )
                    },
                    role = Role.Checkbox,
				)

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
					text = stringResource(id = R.string.lbl_custom_headers),
					modifier = Modifier.padding(innerGroupPadding),
				)

				CustomHeadersList(
					connectionSettingsViewModel,
				)
			}

			Column(
				modifier = Modifier
					.fillMaxWidth(inputRowMaxWidth)
					.selectableGroup()
			) {
				Text(
					text = stringResource(id = R.string.lbl_sync_music_location),
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
                        {
                            RadioButton(
                                selected = syncedFileLocationState == SyncedFileLocation.INTERNAL,
                                onClick = null,
                            )
                        },
                        role = Role.RadioButton,
					)
                }

				Row(
					modifier = Modifier.padding(innerGroupPadding)
				) {
					LabeledSelection(
                        label = stringResource(id = R.string.rbPublicLocation),
                        selected = syncedFileLocationState == SyncedFileLocation.EXTERNAL,
                        onSelected = { syncedFileLocationState = SyncedFileLocation.EXTERNAL },
                        {
                            RadioButton(
                                selected = syncedFileLocationState == SyncedFileLocation.EXTERNAL,
                                onClick = null,
                            )
                        },
                        role = Role.RadioButton,
					)
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
                    {
                        Checkbox(
                            checked = isSyncLocalConnectionsOnlyState,
                            null,
                        )
                    },
                    role = Role.Checkbox,
				)
            }

			SpacedOutRow {
				var isUsingExistingFilesState by isUsingExistingFiles.subscribeAsMutableState()
				LabeledSelection(
                    label = stringResource(id = R.string.lbl_use_existing_music),
                    selected = isUsingExistingFilesState,
                    onSelected = { isUsingExistingFilesState = !isUsingExistingFilesState },
                    {
                        Checkbox(
                            checked = isUsingExistingFilesState,
                            null,
                        )
                    },
                    role = Role.Checkbox,
				)
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
	with(librarySettingsViewModel) {
		val isSettingsChanged by isSettingsChanged.subscribeAsState()

		with(connectionSettingsViewModel) {
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
								} catch (_: Throwable) {
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
                    {
                        Checkbox(
                            checked = isWolEnabledState,
                            onCheckedChange = null,
                        )
                    },
                    role = Role.Checkbox,
				)

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
					text = stringResource(id = R.string.lbl_custom_headers),
					modifier = Modifier.padding(innerGroupPadding),
				)

				CustomHeadersList(
					connectionSettingsViewModel,
				)
			}

			Column(
				modifier = Modifier
					.fillMaxWidth(inputRowMaxWidth)
					.selectableGroup()
			) {
				Text(
					text = stringResource(id = R.string.lbl_sync_music_location),
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
                        {
                            RadioButton(
                                selected = syncedFileLocationState == SyncedFileLocation.INTERNAL,
                                onClick = null,
                            )
                        },
                        role = Role.RadioButton,
					)
                }

				Row(
					modifier = Modifier.padding(innerGroupPadding)
				) {
					LabeledSelection(
                        label = stringResource(id = R.string.rbPublicLocation),
                        selected = syncedFileLocationState == SyncedFileLocation.EXTERNAL,
                        onSelected = { syncedFileLocationState = SyncedFileLocation.EXTERNAL },
                        {
                            RadioButton(
                                selected = syncedFileLocationState == SyncedFileLocation.EXTERNAL,
                                onClick = null,
                            )
                        },
                        role = Role.RadioButton,
					)
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
                    {
                        Checkbox(
                            checked = isUsingExistingFilesState,
                            null,
                        )
                    },
                    role = Role.Checkbox,
				)
            }

			val isSavingState by isSaving.subscribeAsState()
			Button(
				onClick = { saveLibrary() },
				enabled = isSettingsChanged && !isSavingState,
			) {
				Text(
					text =
						if (!isSettingsChanged) stringResource(id = R.string.saved)
						else stringResource(id = R.string.save)
				)
			}
		}
	}
}

@Composable
private fun LibrarySettings(
	librarySettingsViewModel: LibrarySettingsViewModel,
	connectionSettingsViewModel: LibrarySettingsViewModel.ConnectionSettingsViewModel<*>,
	stringResources: GetStringResources,
	userSslCertificates: ProvideUserSslCertificates,
) {

	when (connectionSettingsViewModel) {
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
	}
}


@Composable
fun ServerTypeSelection(
	librarySettingsViewModel: LibrarySettingsViewModel,
	undoBackStack: UndoStack,
	modifier: Modifier = Modifier,
	onServerTypeSelectionFinished: () -> Unit = {},
) {
	val backAction = { onServerTypeSelectionFinished(); true.toPromise() }
	undoBackStack.addAction(backAction)

	Box(
		modifier = Modifier
			.fillMaxSize()
			.then(modifier),
		contentAlignment = Alignment.Center,
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth(inputRowMaxWidth)
				.selectableGroup(),
		) {
			Text(
				text = stringResource(R.string.server_type),
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
                        {
                            RadioButton(
                                selected = connectionSettingsViewModelState == connectionType,
                                onClick = null,
                            )
                        },
                        role = Role.RadioButton,
                        enabled = !isLoadingState,
					)
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
							undoBackStack.removeAction(backAction)
							librarySettingsViewModel.saveLibrary().suspend()
							onServerTypeSelectionFinished()
						}
					},
					enabled = isSettingsChanged && !isSaving,
				) {
					Text(text = if (!isSettingsChanged) stringResource(id = R.string.saved) else stringResource(id = R.string.save))
				}

				Button(
					onClick = {
						undoBackStack.removeAction(backAction)
						onServerTypeSelectionFinished()
					},
				) {
					Text(text = stringResource(id = R.string.btn_cancel))
				}
			}
		}
	}
}

@Composable
private fun LibrarySettingsMenu(
	librarySettingsViewModel: LibrarySettingsViewModel,
	navigateApplication: NavigateApplication,
	stringResources: GetStringResources,
	onChangeServerTypeClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	ListMenuRow(
		modifier = modifier,
		verticalAlignment = Alignment.Top,
	) {
		val buttonModifier = Modifier.requiredWidth(topMenuIconWidth)

		LabelledChangeServerTypeButton(
			stringResources = stringResources,
			onClick = onChangeServerTypeClick,
			modifier = buttonModifier,
		)

		LabelledRemoveServerButton(
			librarySettingsViewModel = librarySettingsViewModel,
			stringResources = stringResources,
			modifier = buttonModifier,
		)

		LabelledSaveAndTestButton(
			librarySettingsViewModel = librarySettingsViewModel,
			stringResources = stringResources,
			modifier = buttonModifier,
		)

		LabelledSaveAndConnectButton(
			librarySettingsViewModel = librarySettingsViewModel,
			navigateApplication = navigateApplication,
			stringResources = stringResources,
			modifier = buttonModifier,
		)
	}
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun ScreenDimensionsScope.LibrarySettingsView(
	librarySettingsViewModel: LibrarySettingsViewModel,
	navigateApplication: NavigateApplication,
	stringResources: GetStringResources,
	userSslCertificates: ProvideUserSslCertificates,
	undoBackStack: UndoStack,
) {
	RemoveServerConfirmationDialog(
		librarySettingsViewModel = librarySettingsViewModel,
		navigateApplication = navigateApplication
	)

	var isUserSelectingServerType by rememberSaveable { mutableStateOf(false) }
	val isLoadingState by librarySettingsViewModel.isLoading.subscribeAsState()

	if (this@LibrarySettingsView.maxWidth < Dimensions.twoColumnThreshold) {

		val collapsedHeightPx = LocalDensity.current.remember { appBarHeight.toPx() }
		val thisTopMenuHeight = topMenuHeight + viewPaddingUnit * 6
		val topMenuHeightPx = LocalDensity.current.remember { thisTopMenuHeight.toPx()  }
		val headerScaler = ConsumableConnectedScaler.remember(collapsedHeightPx)
		val menuHeightScaler = DeferredPreScrollConnectedScaler.remember(topMenuHeightPx, 0f)
		val compositeScroller = remember(headerScaler, menuHeightScaler) {
			headerScaler.linkedTo(menuHeightScaler).ignoreConsumedOffset()
		}

		var lastScrollPosition by rememberSaveable { mutableIntStateOf(0) }
		val scrollState = rememberScrollState()
		LaunchedEffect(scrollState) {
			val firstScrollPosition = snapshotFlow { scrollState.value }.first()
			compositeScroller.onPreScroll(Offset(x = 0f, y = (lastScrollPosition - firstScrollPosition).toFloat()), NestedScrollSource.SideEffect)

			snapshotFlow { scrollState.value }
				.collect {
					lastScrollPosition = it
				}
		}

		VerticalHeaderScaffold(
			header = {
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.background(MaterialTheme.colors.surface)
				) {
					Column(
						modifier = Modifier
							.fillMaxWidth()
							.offsetLayout(headerScaler)
							.clipToBounds()
					) {
						Spacer(modifier = Modifier.height(Dimensions.appBarHeight))

						ProvideTextStyle(MaterialTheme.typography.h5) {
							val startPadding by rememberTitleStartPadding(headerScaler.progressState)
							val endPadding = viewPaddingUnit
							MarqueeText(
								text = stringResource(id = R.string.settings),
								overflow = TextOverflow.Ellipsis,
								gradientSides = setOf(GradientSide.End),
								gradientEdgeColor = MaterialTheme.colors.surface,
								modifier = Modifier
									.fillMaxWidth()
									.padding(
										start = startPadding,
										end = endPadding,
										top = topRowOuterPadding,
										bottom = topRowOuterPadding,
									),
							)
						}
					}

					BackButton(
						navigateApplication::navigateUp,
						modifier = Modifier
							.align(Alignment.TopStart)
							.padding(topRowOuterPadding)
					)
				}
			},
			overlay = {
				val menuHeightPx by menuHeightScaler.valueState
				val menuHeightDp by LocalDensity.current.remember { derivedStateOf { menuHeightPx.toDp() } }
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.background(MaterialTheme.colors.surface)
						.height(menuHeightDp)
						.clipToBounds()
				) {
					LibrarySettingsMenu(
						librarySettingsViewModel = librarySettingsViewModel,
						navigateApplication = navigateApplication,
						stringResources = stringResources,
						onChangeServerTypeClick = { isUserSelectingServerType = true },
						modifier = Modifier
							.graphicsLayer {
								translationY = (menuHeightPx - topMenuHeightPx) * 0.5f
							}
					)
				}
			},
			content = { headerHeight ->
				if (!isLoadingState) {
					val connectionSettingsViewModelState by librarySettingsViewModel.savedConnectionSettingsViewModel.subscribeAsState()
					val isSelectingServerType by remember { derivedStateOf { isUserSelectingServerType || connectionSettingsViewModelState == null } }
					var modifier: Modifier = Modifier.fillMaxWidth()
					modifier =
						if (!isSelectingServerType) modifier.verticalScroll(scrollState)
						else modifier.fillMaxSize()

					Column(
						modifier = modifier,
						horizontalAlignment = Alignment.CenterHorizontally,
					) {
						Spacer(modifier = Modifier.height(headerHeight))

						if (isSelectingServerType) {
							ServerTypeSelection(
								librarySettingsViewModel = librarySettingsViewModel,
								undoBackStack = undoBackStack,
								onServerTypeSelectionFinished = { isUserSelectingServerType = false },
								modifier = Modifier.weight(1f)
							)
						} else {
							connectionSettingsViewModelState?.also { connectionSettingsViewModel ->
								LibrarySettings(
									librarySettingsViewModel = librarySettingsViewModel,
									connectionSettingsViewModel = connectionSettingsViewModel,
									stringResources = stringResources,
									userSslCertificates = userSslCertificates
								)
							}
						}
					}
				}
			},
			modifier = Modifier
				.fillMaxSize()
				.nestedScroll(compositeScroller)
		)
	} else {
		Row(
			modifier = Modifier.fillMaxSize(),
		) {
			val menuWidth = this@LibrarySettingsView.calculateSummaryColumnWidth()
			Column(
				modifier = Modifier.width(menuWidth),
			) {
				Column(
					modifier = Modifier.weight(1f)
				) {
					BackButton(
						navigateApplication::navigateUp,
						modifier = Modifier.padding(topRowOuterPadding)
					)

					ProvideTextStyle(MaterialTheme.typography.h5) {
						Text(
							text = stringResource(id = R.string.settings),
							maxLines = 2,
							overflow = TextOverflow.Ellipsis,
							modifier = Modifier
								.fillMaxWidth()
								.padding(horizontal = viewPaddingUnit),
						)
					}
				}

				LibrarySettingsMenu(
					librarySettingsViewModel = librarySettingsViewModel,
					navigateApplication = navigateApplication,
					stringResources = stringResources,
					{ isUserSelectingServerType = true },
					modifier = Modifier.fillMaxWidth()
				)
			}

			if (!isLoadingState) {
				val connectionSettingsViewModelState by librarySettingsViewModel.savedConnectionSettingsViewModel.subscribeAsState()
				val isSelectingServerType by remember { derivedStateOf { isUserSelectingServerType || connectionSettingsViewModelState == null } }

				var modifier: Modifier = Modifier.fillMaxWidth()
				modifier =
					if (!isSelectingServerType) modifier.verticalScroll(rememberScrollState())
					else modifier.fillMaxSize()

				Column(
					modifier = modifier,
					horizontalAlignment = Alignment.CenterHorizontally,
				) {
					if (isSelectingServerType) {
						ServerTypeSelection(
							librarySettingsViewModel = librarySettingsViewModel,
							undoBackStack = undoBackStack,
							onServerTypeSelectionFinished = { isUserSelectingServerType = false },
							modifier = Modifier.weight(1f)
						)
					} else {
						val connectionSettingsViewModelState by librarySettingsViewModel.savedConnectionSettingsViewModel.subscribeAsState()
						val connectionSettingsViewModel = connectionSettingsViewModelState
						if (connectionSettingsViewModel == null) {
							ServerTypeSelection(
								librarySettingsViewModel = librarySettingsViewModel,
								undoBackStack = undoBackStack,
								onServerTypeSelectionFinished = { isUserSelectingServerType = false },
								modifier = Modifier.weight(1f)
							)
						} else {
							connectionSettingsViewModelState?.also { connectionSettingsViewModel ->
								LibrarySettings(
									librarySettingsViewModel = librarySettingsViewModel,
									connectionSettingsViewModel = connectionSettingsViewModel,
									stringResources = stringResources,
									userSslCertificates = userSslCertificates
								)
							}
						}
					}
				}
			}
		}
	}
}
