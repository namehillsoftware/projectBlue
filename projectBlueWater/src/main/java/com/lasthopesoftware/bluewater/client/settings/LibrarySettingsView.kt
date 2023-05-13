package com.lasthopesoftware.bluewater.client.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.viewmodels.collectAsMutableState
import com.lasthopesoftware.bluewater.shared.promises.extensions.suspend
import com.lasthopesoftware.resources.strings.GetStringResources
import kotlinx.coroutines.launch
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
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
		var accessCodeState by librarySettingsViewModel.accessCode.collectAsMutableState()

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
						text = stringResource(id = R.string.removeServer),
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

		val toolbarState = rememberCollapsingToolbarScaffoldState()
		val headerHidingProgress by remember { derivedStateOf(structuralEqualityPolicy()) { 1 - toolbarState.toolbarState.progress } }

		CollapsingToolbarScaffold(
			enabled = true,
			state = toolbarState,
			scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
			modifier = Modifier.fillMaxSize(),
			toolbar = {
				val topPadding by remember { derivedStateOf(structuralEqualityPolicy()) { (appBarHeight - 46.dp * headerHidingProgress) } }
				BoxWithConstraints(
					modifier = Modifier
						.height(boxHeight)
						.padding(top = topPadding)
				) {
					val iconSize = Dimensions.topMenuIconSize
					val acceleratedToolbarStateProgress by remember {
						derivedStateOf {
							toolbarState.toolbarState.progress.pow(
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
								modifier = Modifier
									.fillMaxHeight(),
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

						ColumnMenuIcon(
							modifier = Modifier
								.fillMaxHeight(),
							onClick = {
								navigateApplication.launchAboutActivity()
							},
							icon = {
								Image(
									painter = painterResource(id = R.drawable.ic_help),
									contentDescription = stringResources.aboutTitle,
									modifier = Modifier.size(iconSize)
								)
							},
							label = if (acceleratedHeaderHidingProgress < 1) stringResources.aboutTitle else null,
							labelModifier = textModifier,
						)
					}
				}

				// Always draw box to help the collapsing toolbar measure minimum size
				Box(modifier = Modifier.height(appBarHeight)) {
					Icon(
						Icons.Default.ArrowBack,
						contentDescription = "",
						tint = MaterialTheme.colors.onSurface,
						modifier = Modifier
							.padding(16.dp)
							.align(Alignment.CenterStart)
							.clickable(
								interactionSource = remember { MutableInteractionSource() },
								indication = null,
								onClick = navigateApplication::navigateUp
							)
					)
				}
			},
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
						var userNameState by userName.collectAsMutableState()
						StandardTextField(
							placeholder = stringResource(R.string.lbl_user_name),
							value = userNameState,
							onValueChange = { userNameState = it }
						)
					}

					SpacedOutRow {
						var passwordState by password.collectAsMutableState()
						StandardTextField(
							placeholder = stringResource(R.string.lbl_password),
							value = passwordState,
							onValueChange = { passwordState = it },
							visualTransformation = PasswordVisualTransformation(),
							keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
						)
					}

					SpacedOutRow {
						var libraryNameState by libraryName.collectAsMutableState()
						StandardTextField(
							placeholder = stringResource(R.string.lbl_library_name),
							value = libraryNameState,
							onValueChange = { libraryNameState = it },
						)
					}

					SpacedOutRow {
						var isLocalOnlyState by isLocalOnly.collectAsMutableState()
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
						var isWolEnabledState by isWakeOnLanEnabled.collectAsMutableState()
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

						var syncedFileLocationState by syncedFileLocation.collectAsMutableState()
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

						Row(
							modifier = Modifier.padding(innerGroupPadding)
						) {
							LabeledSelection(
								label = stringResource(id = R.string.rbCustomLocation),
								selected = syncedFileLocationState == Library.SyncedFileLocation.CUSTOM,
								onSelected = { syncedFileLocationState = Library.SyncedFileLocation.CUSTOM },
								role = Role.RadioButton,
							) {
								RadioButton(
									selected = syncedFileLocationState == Library.SyncedFileLocation.CUSTOM,
									onClick = null,
								)
							}
						}

						Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(innerGroupPadding)
						) {
							var customSyncPathState by customSyncPath.collectAsMutableState()
							StandardTextField(
								placeholder = stringResource(id = R.string.lbl_sync_path),
								value = customSyncPathState,
								onValueChange = { customSyncPathState = it },
								enabled = syncedFileLocationState == Library.SyncedFileLocation.CUSTOM,
							)
						}
					}

					SpacedOutRow {
						var isSyncLocalConnectionsOnlyState by isSyncLocalConnectionsOnly.collectAsMutableState()
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
						var isUsingExistingFilesState by isUsingExistingFiles.collectAsMutableState()
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
						Text(text = if (isSaved) stringResource(id = R.string.btn_saved) else stringResource(id = R.string.btn_save))
					}
				}
			}
		}
	}
}
