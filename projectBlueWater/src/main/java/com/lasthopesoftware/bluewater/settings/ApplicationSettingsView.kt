@file:OptIn(ExperimentalFoundationApi::class)

package com.lasthopesoftware.bluewater.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.components.ApplicationInfoText
import com.lasthopesoftware.bluewater.android.ui.components.ApplicationLogo
import com.lasthopesoftware.bluewater.android.ui.components.ColumnMenuIcon
import com.lasthopesoftware.bluewater.android.ui.components.ConsumedOffsetErasingNestedScrollConnection
import com.lasthopesoftware.bluewater.android.ui.components.FullScreenScrollConnectedScaler
import com.lasthopesoftware.bluewater.android.ui.components.LabeledSelection
import com.lasthopesoftware.bluewater.android.ui.components.LinkedNestedScrollConnection
import com.lasthopesoftware.bluewater.android.ui.components.ListMenuRow
import com.lasthopesoftware.bluewater.android.ui.components.rememberDeferredPreScrollConnectedScaler
import com.lasthopesoftware.bluewater.android.ui.remember
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topMenuIconWidth
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.viewPaddingUnit
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsMutableState
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState

private val horizontalOptionsPadding = 32.dp
val expandedMenuHeight = Dimensions.topMenuHeight + viewPaddingUnit * 6

val standardRowModifier = Modifier
	.fillMaxWidth()
	.height(Dimensions.standardRowHeight)

@Composable
fun SettingsSection(
	headerText: String,
	modifier: Modifier = Modifier,
	headerModifier: Modifier = Modifier,
	body: @Composable () -> Unit,
) {
	Column(modifier = modifier) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.then(headerModifier),
			verticalAlignment = Alignment.CenterVertically,
		) {
			ProvideTextStyle(value = MaterialTheme.typography.h6) {
				Text(text = headerText, modifier = Modifier.weight(1f))
			}
		}

		body()
	}
}

@Composable
fun AudioSettingsSection(
	applicationSettingsViewModel: ApplicationSettingsViewModel,
) {
	SettingsSection(
		headerText = stringResource(id = R.string.app_audio_settings),
		headerModifier = standardRowModifier,
		modifier = Modifier.fillMaxWidth(),
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = horizontalOptionsPadding),
		) {
			val isLoading by applicationSettingsViewModel.isLoading.subscribeAsState()
			val isVolumeLevelingEnabled by applicationSettingsViewModel.isVolumeLevelingEnabled.subscribeAsState()
			LabeledSelection(
				label = stringResource(id = R.string.use_volume_leveling_setting),
				selected = isVolumeLevelingEnabled,
				onSelected = { applicationSettingsViewModel.promiseVolumeLevelingEnabledChange(!isVolumeLevelingEnabled) },
				{
					Checkbox(checked = isVolumeLevelingEnabled, onCheckedChange = null, enabled = !isLoading)
				},
				modifier = standardRowModifier,
				enabled = !isLoading,
			)

			val isPeakLevelNormalizeEnabled by applicationSettingsViewModel.isPeakLevelNormalizeEnabled.subscribeAsState()
			val isPeakLevelNormalizeEditable by applicationSettingsViewModel.isPeakLevelNormalizeEditable.subscribeAsState()
			LabeledSelection(
				label = stringResource(id = R.string.enable_peak_level_normalize),
				selected = isPeakLevelNormalizeEnabled,
				onSelected = { applicationSettingsViewModel.promisePeakLevelNormalizeEnabledChange(!isPeakLevelNormalizeEnabled) },
				{
					Checkbox(
						checked = isPeakLevelNormalizeEnabled,
						onCheckedChange = null,
						enabled = isPeakLevelNormalizeEditable && !isLoading
					)
				},
				modifier = standardRowModifier.padding(start = horizontalOptionsPadding),
				enabled = isPeakLevelNormalizeEditable && !isLoading
			)
		}
	}
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun ThemeSettingsSection(
	applicationSettingsViewModel: ApplicationSettingsViewModel,
) {
	SettingsSection(
		headerText = stringResource(id = R.string.theme),
		headerModifier = standardRowModifier,
		modifier = Modifier.fillMaxWidth(),
	) {
		Row(
			modifier = standardRowModifier.padding(horizontal = horizontalOptionsPadding).selectableGroup(),
			horizontalArrangement = Arrangement.SpaceEvenly,
		) {
			val isLoading by applicationSettingsViewModel.isLoading.subscribeAsState()
			val theme by applicationSettingsViewModel.theme.subscribeAsState()
			val selectedChipColors = ChipDefaults.chipColors(
				backgroundColor = MaterialTheme.colors.secondary,
				contentColor = MaterialTheme.colors.onSecondary,
			)
			Chip(
				colors = if (theme == ApplicationSettings.Theme.SYSTEM) selectedChipColors else ChipDefaults.chipColors() ,
				onClick = { applicationSettingsViewModel.promiseThemeChange(ApplicationSettings.Theme.SYSTEM) },
				enabled = !isLoading
			) {
				Text(stringResource(R.string.system))
			}

			Chip(
				colors = if (theme == ApplicationSettings.Theme.LIGHT) selectedChipColors else ChipDefaults.chipColors() ,
				onClick = { applicationSettingsViewModel.promiseThemeChange(ApplicationSettings.Theme.LIGHT) },
				enabled = !isLoading
			) {
				Text(stringResource(R.string.light))
			}

			Chip(
				colors = if (theme == ApplicationSettings.Theme.DARK) selectedChipColors else ChipDefaults.chipColors() ,
				onClick = { applicationSettingsViewModel.promiseThemeChange(ApplicationSettings.Theme.DARK) },
				enabled = !isLoading
			) {
				Text(stringResource(R.string.dark))
			}
		}
	}
}

@Composable
fun AboutApplication(
	playbackService: ControlPlaybackService,
) {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = Dimensions.topMenuHeight)
	) {
		ApplicationInfoText(
			versionName = BuildConfig.VERSION_NAME,
			versionCode = BuildConfig.VERSION_CODE,
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.Center)
		)
	}

	Box {
		var dependenciesString by remember { mutableStateOf("") }
		val localResources = LocalResources.current
		LaunchedEffect(localResources) {
			localResources.openRawResource(R.raw.dependencies).bufferedReader().use {
				dependenciesString = it.readText()
			}
		}

		Text(dependenciesString)
	}

	Button(
		modifier = Modifier
			.padding(top = Dimensions.topMenuHeight),
		onClick = { playbackService.kill() }
	) {
		val rowFontSize = LocalDensity.current.run { dimensionResource(id = R.dimen.row_font_size).toSp() }
		Text(
			text = stringResource(id = R.string.kill_playback),
			fontSize = rowFontSize,
		)
	}
}

@Composable
private fun SettingsList(
	applicationSettingsViewModel: ApplicationSettingsViewModel,
	playbackService: ControlPlaybackService,
) {
	val isLoading by applicationSettingsViewModel.isLoading.subscribeAsState()
	SettingsSection(
		headerText = stringResource(id = R.string.app_sync_settings),
		modifier = Modifier.fillMaxWidth(),
		headerModifier = standardRowModifier,
	) {
		Column {
			val isSyncOnWifiOnly by applicationSettingsViewModel.isSyncOnWifiOnly.subscribeAsState()
			LabeledSelection(
				label = stringResource(id = R.string.app_only_sync_on_wifi),
				selected = isSyncOnWifiOnly,
				onSelected = { applicationSettingsViewModel.promiseSyncOnWifiChange(!isSyncOnWifiOnly) },
				{
					Checkbox(checked = isSyncOnWifiOnly, onCheckedChange = null, enabled = !isLoading)
				},
				enabled = !isLoading,
				modifier = standardRowModifier.padding(horizontal = horizontalOptionsPadding)
			)

			val isSyncOnPowerOnly by applicationSettingsViewModel.isSyncOnPowerOnly.subscribeAsState()
			LabeledSelection(
				label = stringResource(id = R.string.app_only_sync_ext_power),
				selected = isSyncOnPowerOnly,
				onSelected = { applicationSettingsViewModel.promiseSyncOnPowerChange(!isSyncOnPowerOnly) },
				{
					Checkbox(checked = isSyncOnPowerOnly, onCheckedChange = null, enabled = !isLoading)
				},
				enabled = !isLoading,
				modifier = standardRowModifier.padding(horizontal = horizontalOptionsPadding),
			)
		}
	}

	AudioSettingsSection(applicationSettingsViewModel)

	ThemeSettingsSection(applicationSettingsViewModel)

	AboutApplication(playbackService)
}

@Composable
fun ServersList(
	applicationNavigation: NavigateApplication,
	libraries: List<Pair<LibraryId, String>>,
	selectedLibraryId: LibraryId?
) {
	val rowFontSize = LocalDensity.current.run { dimensionResource(id = R.dimen.row_font_size).toSp() }

	for ((libraryId, name) in libraries) {
		Row(
			modifier = standardRowModifier.clickable { applicationNavigation.viewServerSettings(libraryId) },
			verticalAlignment = Alignment.CenterVertically,
		) {
			Text(
				text = name,
				modifier = Modifier
					.weight(1f)
					.padding(viewPaddingUnit),
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				fontWeight = if (libraryId == selectedLibraryId) FontWeight.Bold else FontWeight.Normal,
				fontSize = rowFontSize,
			)

			Row(
				modifier = Modifier
					.padding(viewPaddingUnit),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Icon(
					painter = painterResource(id = R.drawable.ic_action_settings),
					contentDescription = stringResource(id = R.string.settings)
				)
			}
		}
	}
}

@Composable
fun ApplicationSettingsMenu(
	applicationSettingsViewModel: ApplicationSettingsViewModel,
	applicationNavigation: NavigateApplication,
	selectedLibraryId: LibraryId?,
	modifier: Modifier = Modifier,
) {
	ListMenuRow(
		modifier = modifier,
		verticalAlignment = Alignment.Top,
	) {
		val modifier = Modifier.requiredWidth(topMenuIconWidth)

		var selectedTab by applicationSettingsViewModel.selectedTab.subscribeAsMutableState()

		val viewServersLabel = stringResource(R.string.view_servers)
		ColumnMenuIcon(
			onClick = {
				selectedTab = ApplicationSettingsViewModel.SelectedTab.ViewServers
			},
			iconPainter = painterResource(id = R.drawable.select_library_36dp),
			contentDescription = viewServersLabel,
			label = viewServersLabel,
			labelMaxLines = 2,
			modifier = modifier,
			enabled = selectedTab != ApplicationSettingsViewModel.SelectedTab.ViewServers
		)

		val settingsButtonLabel = stringResource(R.string.application_settings)
		ColumnMenuIcon(
			onClick = {
				selectedTab = ApplicationSettingsViewModel.SelectedTab.ViewSettings
			},
			iconPainter = painterResource(id = R.drawable.ic_action_settings),
			contentDescription = settingsButtonLabel,
			label = settingsButtonLabel,
			labelMaxLines = 2,
			modifier = modifier,
			enabled = selectedTab != ApplicationSettingsViewModel.SelectedTab.ViewSettings
		)

		val addServerButtonLabel = stringResource(R.string.btn_add_server)
		ColumnMenuIcon(
			onClick = {
				applicationNavigation.viewNewServerSettings()
			},
			iconPainter = painterResource(id = R.drawable.ic_add_item_36dp),
			contentDescription = addServerButtonLabel,
			label = addServerButtonLabel,
			labelMaxLines = 2,
			modifier = modifier,
		)

		val connectText = stringResource(R.string.active_server)
		ColumnMenuIcon(
			onClick = {
				selectedLibraryId?.also(applicationNavigation::viewLibrary)
			},
			iconPainter = painterResource(id = R.drawable.arrow_right_24dp),
			contentDescription = connectText,
			modifier = modifier,
			label = connectText,
			labelMaxLines = 2,
			enabled = selectedLibraryId != null
		)
	}
}

@Composable
private fun ApplicationSettingsViewVertical(
	applicationSettingsViewModel: ApplicationSettingsViewModel,
	applicationNavigation: NavigateApplication,
	playbackService: ControlPlaybackService,
) {
	val expandedMenuHeightPx = LocalDensity.current.remember { expandedMenuHeight.toPx() }
	var menuOffsetY by remember { mutableFloatStateOf(Float.MAX_VALUE) }
	val paddingScaler = FullScreenScrollConnectedScaler.remember(
		min = 0f,
		max = menuOffsetY
	)
	val menuScaler = rememberDeferredPreScrollConnectedScaler(expandedMenuHeightPx, 0f)
	val scrollConnection = remember(paddingScaler, menuScaler) {
		ConsumedOffsetErasingNestedScrollConnection(
			LinkedNestedScrollConnection(paddingScaler,menuScaler)
		)
	}

	val libraries by applicationSettingsViewModel.libraries.subscribeAsState()
	val selectedLibraryId by applicationSettingsViewModel.chosenLibraryId.subscribeAsState()

	Box(
		modifier = Modifier
			.fillMaxSize()
			.nestedScroll(scrollConnection)
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState()),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.padding(viewPaddingUnit * 8)
			) {
				ApplicationLogo(
					modifier = Modifier
						.fillMaxWidth(.5f)
						.align(Alignment.TopCenter)
						.combinedClickable(
							interactionSource = remember { MutableInteractionSource() },
							indication = null,
							onClick = {},
							onLongClick = { applicationNavigation.viewHiddenSettings() },
						)
				)
			}

			Spacer(modifier = Modifier
				.height(expandedMenuHeight + Dimensions.appBarHeight)
				.onPlaced { bounds ->
					menuOffsetY = bounds.positionInParent().y
				}
			)

			val selectedTab by applicationSettingsViewModel.selectedTab.subscribeAsState()
			when (selectedTab) {
				ApplicationSettingsViewModel.SelectedTab.ViewServers -> ServersList(
					applicationNavigation,
					libraries,
					selectedLibraryId,
				)
				ApplicationSettingsViewModel.SelectedTab.ViewSettings -> SettingsList(
					applicationSettingsViewModel,
					playbackService
				)
			}
		}

		val menuHeightDp by LocalDensity.current.remember(menuScaler) { derivedStateOf { menuScaler.valueState.floatValue.toDp() } }
		val paddingDp by LocalDensity.current.remember(paddingScaler) { derivedStateOf { paddingScaler.valueState.value.toDp() } }
		Column(
			modifier = Modifier
				.padding(top = paddingDp)
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.height(Dimensions.appBarHeight)
					.background(MaterialTheme.colors.surface)
			) {
				ProvideTextStyle(MaterialTheme.typography.h4) {
					Text(text = stringResource(id = R.string.app_name))
				}
			}

			ApplicationSettingsMenu(
				applicationSettingsViewModel,
				applicationNavigation = applicationNavigation,
				selectedLibraryId = selectedLibraryId,
				modifier = Modifier
					.fillMaxWidth()
					.height(menuHeightDp)
					.background(MaterialTheme.colors.surface)
					.clipToBounds(),
			)
		}
	}
}

@Composable
private fun BoxWithConstraintsScope.ApplicationSettingsViewHorizontal(
	applicationSettingsViewModel: ApplicationSettingsViewModel,
	applicationNavigation: NavigateApplication,
	playbackService: ControlPlaybackService,
) {
	Row(
		modifier = Modifier.fillMaxSize(),
		horizontalArrangement = Arrangement.Start,
	) {
		val libraries by applicationSettingsViewModel.libraries.subscribeAsState()
		val selectedLibraryId by applicationSettingsViewModel.chosenLibraryId.subscribeAsState()

		Column(
			modifier = Modifier.width(this@ApplicationSettingsViewHorizontal.maxHeight)
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.height(Dimensions.appBarHeight)
					.background(MaterialTheme.colors.surface)
			) {
				ProvideTextStyle(MaterialTheme.typography.h4) {
					Text(text = stringResource(id = R.string.app_name))
				}
			}

			ApplicationLogo(modifier = Modifier
				.fillMaxHeight(.5f)
				.align(Alignment.CenterHorizontally)
				.weight(1f)
				.combinedClickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onClick = {},
					onLongClick = { applicationNavigation.viewHiddenSettings() },
				)
			)

			ApplicationSettingsMenu(
				applicationSettingsViewModel,
				applicationNavigation = applicationNavigation,
				selectedLibraryId = selectedLibraryId,
				modifier = Modifier
					.fillMaxWidth()
					.height(expandedMenuHeight)
					.clipToBounds()
					.align(Alignment.Start),
			)
		}

		Column(
			modifier = Modifier
				.fillMaxHeight()
				.padding(start = viewPaddingUnit * 2)
				.verticalScroll(rememberScrollState()),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			val selectedTab by applicationSettingsViewModel.selectedTab.subscribeAsState()
			when (selectedTab) {
				ApplicationSettingsViewModel.SelectedTab.ViewServers -> ServersList(
					applicationNavigation,
					libraries,
					selectedLibraryId,
				)
				ApplicationSettingsViewModel.SelectedTab.ViewSettings -> SettingsList(
					applicationSettingsViewModel,
					playbackService
				)
			}
		}
	}
}

@Composable
fun ApplicationSettingsView(
	applicationSettingsViewModel: ApplicationSettingsViewModel,
	applicationNavigation: NavigateApplication,
	playbackService: ControlPlaybackService,
) {
	Box(modifier = Modifier
		.fillMaxSize()
		.background(Color.Black)
	) {

		val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
		val layoutDirection = LocalLayoutDirection.current
		Column(
			modifier = Modifier
				.padding(
					start = systemBarsPadding.calculateStartPadding(layoutDirection),
					end = systemBarsPadding.calculateEndPadding(layoutDirection),
					bottom = systemBarsPadding.calculateBottomPadding(),
				)
				.fillMaxSize()
		) {
			Spacer(
				modifier = Modifier
					.windowInsetsTopHeight(WindowInsets.systemBars)
					.fillMaxWidth()
					.background(MaterialTheme.colors.surface)
			)

			ControlSurface(modifier = Modifier.weight(1f)) {
				BoxWithConstraints(
					modifier = Modifier
						.fillMaxSize()
						.padding(viewPaddingUnit)
				) {
					if (maxWidth < maxHeight) ApplicationSettingsViewVertical(
						applicationSettingsViewModel,
						applicationNavigation,
						playbackService
					) else ApplicationSettingsViewHorizontal(
						applicationSettingsViewModel,
						applicationNavigation,
						playbackService
					)
				}
			}
		}
	}
}
