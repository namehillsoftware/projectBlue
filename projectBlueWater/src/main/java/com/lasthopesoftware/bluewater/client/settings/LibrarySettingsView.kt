package com.lasthopesoftware.bluewater.client.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.shared.android.ui.components.GradientSide
import com.lasthopesoftware.bluewater.shared.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ColumnMenuIcon
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.viewmodels.collectAsMutableState
import com.lasthopesoftware.resources.strings.GetStringResources
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import kotlin.math.pow

private const val expandedTitleHeight = 84
private const val expandedIconSize = 44
private const val expandedMenuVerticalPadding = 8
private val appBarHeight = Dimensions.AppBarHeight.value

@Composable
private fun DataEntryRow(content: @Composable (RowScope.() -> Unit)) {
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
fun LibrarySettingsView(
	librarySettingsViewModel: LibrarySettingsViewModel,
	navigateApplication: NavigateApplication,
	stringResources: GetStringResources,
) {
	Surface {
		val toolbarState = rememberCollapsingToolbarScaffoldState()
		val headerHidingProgress by remember { derivedStateOf(structuralEqualityPolicy()) { 1 - toolbarState.toolbarState.progress } }

		CollapsingToolbarScaffold(
			enabled = true,
			state = toolbarState,
			scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
			modifier = Modifier.fillMaxSize(),
			toolbar = {
				val topPadding by remember { derivedStateOf(structuralEqualityPolicy()) { (appBarHeight - 46 * headerHidingProgress).dp } }
				val boxHeight =
					expandedTitleHeight + expandedIconSize + expandedMenuVerticalPadding * 2 + appBarHeight
				BoxWithConstraints(
					modifier = Modifier
						.height(boxHeight.dp)
						.padding(top = topPadding)
				) {
					val minimumMenuWidth = (3 * 32).dp
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
						val endPadding by remember { derivedStateOf(structuralEqualityPolicy()) { 4.dp + minimumMenuWidth * acceleratedHeaderHidingProgress } }
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

					val menuWidth by remember { derivedStateOf(structuralEqualityPolicy()) { (maxWidth - (maxWidth - minimumMenuWidth) * acceleratedHeaderHidingProgress) } }
					val expandedTopRowPadding = expandedTitleHeight + expandedMenuVerticalPadding
					val collapsedTopRowPadding = 6
					val topRowPadding by remember { derivedStateOf(structuralEqualityPolicy()) { (expandedTopRowPadding - (expandedTopRowPadding - collapsedTopRowPadding) * headerHidingProgress).dp } }
					Row(
						modifier = Modifier
							.padding(
								top = topRowPadding,
								bottom = expandedMenuVerticalPadding.dp,
								start = 8.dp,
								end = 8.dp
							)
							.width(menuWidth)
							.align(Alignment.TopEnd)
					) {
						val iconSize = Dimensions.MenuIconSize
						val textModifier = Modifier.alpha(acceleratedToolbarStateProgress)

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

						ColumnMenuIcon(
							modifier = Modifier
								.fillMaxHeight(),
							onClick = {
//							librarySettingsViewModel.removeLibrary()
							},
							icon = {
								Image(
									painter = painterResource(id = R.drawable.ic_remove_item_36dp),
									contentDescription = stringResources.removeServer,
									modifier = Modifier.size(iconSize)
								)
							},
							label = if (acceleratedHeaderHidingProgress < 1) stringResources.removeServer else null,
							labelModifier = textModifier,
						)
					}
				}

				// Always draw box to help the collapsing toolbar measure minimum size
				Box(modifier = Modifier.height(appBarHeight.dp)) {
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
}
