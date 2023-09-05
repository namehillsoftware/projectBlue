package com.lasthopesoftware.bluewater.client.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
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
import com.lasthopesoftware.bluewater.shared.promises.extensions.suspend
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
fun LibrarySettingsView(
	librarySettingsViewModel: LibrarySettingsViewModel,
	navigateApplication: NavigateApplication,
	stringResources: GetStringResources,
) {
	ControlSurface {
		var accessCodeState by librarySettingsViewModel.accessCode.subscribeAsMutableState()

		val scope = rememberCoroutineScope()
		val libraryRemovalRequested by librarySettingsViewModel.isRemovalRequested.collectAsState()
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

		val isSettingsChanged by librarySettingsViewModel.isSettingsChanged.subscribeAsState()
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
				Spacer(modifier = Modifier
					.requiredHeight(boxHeight)
					.fillMaxWidth())

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

					Column(modifier = Modifier
						.fillMaxWidth(.8f)
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
								selected = syncedFileLocationState == Library.SyncedFileLocation.INTERNAL,
								onSelected = { syncedFileLocationState = Library.SyncedFileLocation.INTERNAL },
								role = Role.RadioButton,
							) {
								RadioButton(
									selected = syncedFileLocationState == Library.SyncedFileLocation.INTERNAL,
									onClick = null,
								)
							}
						}

						Row(
							modifier = Modifier.padding(innerGroupPadding)
						) {
							LabeledSelection(
								label = stringResource(id = R.string.rbPublicLocation),
								selected = syncedFileLocationState == Library.SyncedFileLocation.EXTERNAL,
								onSelected = { syncedFileLocationState = Library.SyncedFileLocation.EXTERNAL },
								role = Role.RadioButton,
							) {
								RadioButton(
									selected = syncedFileLocationState == Library.SyncedFileLocation.EXTERNAL,
									onClick = null,
								)
							}
						}

						val isStoragePermissionsNeeded by isStoragePermissionsNeeded.collectAsState()
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

					val isSavingState by isSaving.collectAsState()
					Button(
						onClick = { saveLibrary() },
						enabled = isSettingsChanged && !isSavingState,
					) {
						Text(text = if (!isSettingsChanged) stringResource(id = R.string.saved) else stringResource(id = R.string.save))
					}
				}
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
						val startPadding by remember {  derivedStateOf(structuralEqualityPolicy()) { (4 + iconClearance * headerHidingProgress).dp } }
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
								start = 8.dp,
								end = 8.dp
							)
							.width(menuWidth)
							.align(Alignment.TopEnd)
					) {
						val textModifier = Modifier.alpha(acceleratedToolbarStateProgress)

						if (menuWidth > iconSize * 2) {
							ColumnMenuIcon(
								modifier = Modifier.fillMaxHeight(),
								onClick = librarySettingsViewModel::requestLibraryRemoval,
								icon = {
									Image(
										painter = painterResource(id = R.drawable.ic_remove_item_36dp),
										contentDescription = stringResources.removeServer,
										modifier = Modifier.size(iconSize)
									)
								},
								label = stringResources.removeServer,
								labelModifier = textModifier,
							)
						}

						val saveAndConnectText by remember {
							derivedStateOf {
								if (isSettingsChanged) stringResources.saveAndConnect
								else stringResources.connect
							}
						}

						ColumnMenuIcon(
							modifier = Modifier.fillMaxHeight(),
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
							icon = {
								Image(
									painter = painterResource(id = R.drawable.ic_arrow_right),
									contentDescription = saveAndConnectText,
									modifier = Modifier.size(iconSize)
								)
							},
							label = if (acceleratedHeaderHidingProgress < 1) saveAndConnectText else null,
							labelModifier = textModifier,
						)
					}
				}

				Icon(
					Icons.Default.ArrowBack,
					contentDescription = "",
					tint = MaterialTheme.colors.onSurface,
					modifier = Modifier
						.padding(16.dp)
						.align(Alignment.TopStart)
						.clickable(
							interactionSource = remember { MutableInteractionSource() },
							indication = null,
							onClick = navigateApplication::navigateUp
						)
				)
			}
		}
	}
}
