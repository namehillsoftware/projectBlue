package com.lasthopesoftware.bluewater.client.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
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
import com.lasthopesoftware.bluewater.shared.android.ui.components.BackButton
import com.lasthopesoftware.bluewater.shared.android.ui.components.ColumnMenuIcon
import com.lasthopesoftware.bluewater.shared.android.ui.components.GradientSide
import com.lasthopesoftware.bluewater.shared.android.ui.components.LabeledSelection
import com.lasthopesoftware.bluewater.shared.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.shared.android.ui.components.StandardTextField
import com.lasthopesoftware.bluewater.shared.android.ui.components.memorableScrollConnectedScaler
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsMutableState
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.strings.GetStringResources
import kotlinx.coroutines.launch
import kotlin.math.pow

private val expandedTitleHeight = 84.dp
private val expandedIconSize = Dimensions.menuHeight
private val expandedMenuVerticalPadding = Dimensions.viewPaddingUnit * 2
private val collapsedTopRowPadding = 6.dp
private val appBarHeight = Dimensions.appBarHeight
private val boxHeight = expandedTitleHeight + expandedIconSize + expandedMenuVerticalPadding * 2 + appBarHeight
private val innerGroupPadding = Dimensions.viewPaddingUnit * 2
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
private fun RowScope.UnlabelledRemoveServerButton(
	librarySettingsViewModel: LibrarySettingsViewModel,
	stringResources: GetStringResources,
) {
	ColumnMenuIcon(
		onClick = librarySettingsViewModel::requestLibraryRemoval,
		iconPainter = painterResource(id = R.drawable.ic_remove_item_36dp),
		contentDescription = stringResources.removeServer,
		label = null,
	)
}

@Composable
private fun RowScope.LabelledSaveAndConnectButton(
	librarySettingsViewModel: LibrarySettingsViewModel,
	navigateApplication: NavigateApplication,
	stringResources: GetStringResources,
	modifier: Modifier = Modifier,
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
	)
}

@Composable
private fun RowScope.UnlabelledSaveAndConnectButton(
	librarySettingsViewModel: LibrarySettingsViewModel,
	navigateApplication: NavigateApplication,
	stringResources: GetStringResources,
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
		label = null,
	)
}

@Composable
private fun RemoveServerConfirmationDialog(
	librarySettingsViewModel: LibrarySettingsViewModel,
	navigateApplication: NavigateApplication
) {
	val scope = rememberCoroutineScope()
	val libraryRemovalRequested by librarySettingsViewModel.isRemovalRequested.subscribeAsState()
	val accessCodeState by librarySettingsViewModel.accessCode.subscribeAsMutableState()
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
}

@Composable
private fun LibrarySettingsList(
	librarySettingsViewModel: LibrarySettingsViewModel,
	stringResources: GetStringResources,
	userSslCertificates: ProvideUserSslCertificates,
) {
	val isSettingsChanged by librarySettingsViewModel.isSettingsChanged.subscribeAsState()

		librarySettingsViewModel.apply {
			var accessCodeState by librarySettingsViewModel.accessCode.subscribeAsMutableState()
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

@Composable
fun LibrarySettingsView(
	librarySettingsViewModel: LibrarySettingsViewModel,
	navigateApplication: NavigateApplication,
	stringResources: GetStringResources,
	userSslCertificates: ProvideUserSslCertificates,
) {
	ControlSurface {
		RemoveServerConfirmationDialog(
			librarySettingsViewModel = librarySettingsViewModel,
			navigateApplication = navigateApplication
		)

		val boxHeightPx = LocalDensity.current.run { boxHeight.toPx() }
		val collapsedHeightPx = LocalDensity.current.run { appBarHeight.toPx() }
		val heightScaler = memorableScrollConnectedScaler(boxHeightPx, collapsedHeightPx)

		Box(
			modifier = Modifier
				.fillMaxSize()
				.nestedScroll(heightScaler)
		) {
			val heightValue by heightScaler.getValueState()


			Column(
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(rememberScrollState()),
				horizontalAlignment = Alignment.CenterHorizontally,
			) {
				Spacer(
					modifier = Modifier
						.requiredHeight(boxHeight)
						.fillMaxWidth()
				)

				LibrarySettingsList(
					librarySettingsViewModel = librarySettingsViewModel,
					stringResources = stringResources,
					userSslCertificates = userSslCertificates,
				)
			}

			Box(
				modifier = Modifier
					.fillMaxWidth()
					.align(Alignment.TopStart)
					.background(MaterialTheme.colors.surface)
					.height(LocalDensity.current.run { heightValue.toDp() })
			) {
				val headerHidingProgress by heightScaler.getProgressState()
				val headerExpandingProgress by remember { derivedStateOf { 1 - headerHidingProgress } }
				val topPadding by remember { derivedStateOf(structuralEqualityPolicy()) { (appBarHeight - 46.dp * headerHidingProgress) } }
				BoxWithConstraints(
					modifier = Modifier
						.height(boxHeight)
						.padding(top = topPadding)
				) {
					val iconSize = Dimensions.topMenuIconSize
					val acceleratedToolbarStateProgress by remember {
						derivedStateOf {
							headerExpandingProgress.pow(
								3
							).coerceIn(0f, 1f)
						}
					}

					val acceleratedHeaderHidingProgress by remember {
						derivedStateOf { 1 - acceleratedToolbarStateProgress }
					}

					ProvideTextStyle(MaterialTheme.typography.h5) {
						val iconClearance = 48
						val startPadding by remember { derivedStateOf(structuralEqualityPolicy()) { (4 + iconClearance * headerHidingProgress).dp } }
						val endPadding by remember { derivedStateOf(structuralEqualityPolicy()) { 4.dp + iconSize * acceleratedHeaderHidingProgress } }
						val header = stringResource(id = R.string.settings)
						MarqueeText(
							text = header,
							overflow = TextOverflow.Ellipsis,
							gradientSides = setOf(GradientSide.End),
							gradientEdgeColor = MaterialTheme.colors.surface,
							modifier = Modifier
								.fillMaxWidth()
								.padding(start = startPadding, end = endPadding),
						)
					}

					val menuWidth by remember { derivedStateOf(structuralEqualityPolicy()) { (maxWidth - (maxWidth - iconSize) * acceleratedHeaderHidingProgress) } }
					val expandedTopRowPadding = expandedTitleHeight + expandedMenuVerticalPadding
					val topRowPadding by remember { derivedStateOf(structuralEqualityPolicy()) { (expandedTopRowPadding - (expandedTopRowPadding - collapsedTopRowPadding) * headerHidingProgress) } }
					Row(
						modifier = Modifier
							.padding(
								top = topRowPadding,
								bottom = expandedMenuVerticalPadding,
								start = Dimensions.viewPaddingUnit * 2,
								end = Dimensions.viewPaddingUnit * 2
							)
							.width(menuWidth)
							.align(Alignment.TopEnd)
					) {
						val textModifier = Modifier.alpha(acceleratedToolbarStateProgress)

						if (menuWidth > iconSize * 2) {
							if (acceleratedHeaderHidingProgress < 1) {
								LabelledRemoveServerButton(
									librarySettingsViewModel = librarySettingsViewModel,
									stringResources = stringResources,
									modifier = textModifier
								)
							} else {
								UnlabelledRemoveServerButton(
									librarySettingsViewModel = librarySettingsViewModel,
									stringResources = stringResources
								)
							}
						}

						if (acceleratedHeaderHidingProgress < 1) {
							LabelledSaveAndConnectButton(
								librarySettingsViewModel = librarySettingsViewModel,
								navigateApplication = navigateApplication,
								stringResources = stringResources,
								modifier = textModifier
							)
						} else {
							UnlabelledSaveAndConnectButton(
								librarySettingsViewModel = librarySettingsViewModel,
								navigateApplication = navigateApplication,
								stringResources = stringResources,
							)
						}
					}
				}

				BackButton(
					navigateApplication::navigateUp,
					modifier = Modifier
						.align(Alignment.TopStart)
						.padding(Dimensions.topRowOuterPadding)
				)
			}
		}
	}
}

@Composable
fun TvLibrarySettingsView(
	librarySettingsViewModel: LibrarySettingsViewModel,
	navigateApplication: NavigateApplication,
	stringResources: GetStringResources,
	userSslCertificates: ProvideUserSslCertificates,
) {
	ControlSurface {
		RemoveServerConfirmationDialog(
			librarySettingsViewModel = librarySettingsViewModel,
			navigateApplication = navigateApplication
		)

		Column(modifier = Modifier.fillMaxSize()) {
			Column {
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.height(Dimensions.appBarHeight)
						.padding(Dimensions.viewPaddingUnit * 4),
					contentAlignment = Alignment.CenterStart,
				) {
					BackButton(navigateApplication::navigateUp, modifier = Modifier.align(Alignment.TopStart))
				}

				Box(modifier = Modifier.height(expandedTitleHeight)) {
					ProvideTextStyle(MaterialTheme.typography.h5) {
						val startPadding = Dimensions.viewPaddingUnit
						val endPadding = Dimensions.viewPaddingUnit
						val maxLines = 2
						Text(
							text = stringResource(id = R.string.settings),
							maxLines = maxLines,
							overflow = TextOverflow.Ellipsis,
							modifier = Modifier
								.fillMaxWidth()
								.padding(start = startPadding, end = endPadding),
						)
					}
				}

				Row(
					modifier = Modifier
						.padding(
							top = expandedMenuVerticalPadding,
							bottom = expandedMenuVerticalPadding,
							start = Dimensions.viewPaddingUnit * 2,
							end = Dimensions.viewPaddingUnit * 2
						)
						.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceEvenly,
				) {
					LabelledRemoveServerButton(
						librarySettingsViewModel = librarySettingsViewModel,
						stringResources = stringResources,
					)

					LabelledSaveAndConnectButton(
						librarySettingsViewModel = librarySettingsViewModel,
						navigateApplication = navigateApplication,
						stringResources = stringResources,
					)
				}
			}

			Column(
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(rememberScrollState()),
				horizontalAlignment = Alignment.CenterHorizontally,
			) {
				LibrarySettingsList(
					librarySettingsViewModel = librarySettingsViewModel,
					stringResources = stringResources,
					userSslCertificates = userSslCertificates,
				)
			}
		}
	}
}
