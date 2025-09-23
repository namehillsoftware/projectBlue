package com.lasthopesoftware.bluewater.client.stored.library.items.files.view

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.calculateSummaryColumnWidth
import com.lasthopesoftware.bluewater.android.ui.components.BackButton
import com.lasthopesoftware.bluewater.android.ui.components.ConsumedOffsetErasingNestedScrollConnection
import com.lasthopesoftware.bluewater.android.ui.components.FullScreenScrollConnectedScaler
import com.lasthopesoftware.bluewater.android.ui.components.GradientSide
import com.lasthopesoftware.bluewater.android.ui.components.LinkedNestedScrollConnection
import com.lasthopesoftware.bluewater.android.ui.components.ListLoading
import com.lasthopesoftware.bluewater.android.ui.components.ListMenuRow
import com.lasthopesoftware.bluewater.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.android.ui.components.MenuIcon
import com.lasthopesoftware.bluewater.android.ui.components.UnlabelledChevronIcon
import com.lasthopesoftware.bluewater.android.ui.components.rememberDeferredPreScrollConnectedScaler
import com.lasthopesoftware.bluewater.android.ui.components.rememberTitleStartPadding
import com.lasthopesoftware.bluewater.android.ui.linearInterpolation
import com.lasthopesoftware.bluewater.android.ui.remember
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.rowPadding
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topMenuHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topMenuIconSize
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topMenuIconWidth
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topRowOuterPadding
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.viewPaddingUnit
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.TrackTitleItemView
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewFileItem
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListContentType
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.ScreenDimensionsScope
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncIcon
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

private val expandedTitleHeight = Dimensions.expandedTitleHeight
private val appBarHeight = Dimensions.appBarHeight
private val boxHeight = expandedTitleHeight + appBarHeight

@Composable
private fun SyncMenu(
	activeFileDownloadsViewModel: ActiveFileDownloadsViewModel,
) {
	ListMenuRow(
		modifier = Modifier.fillMaxWidth()
	) {
		val isSyncing by activeFileDownloadsViewModel.isSyncing.subscribeAsState()
		val label = stringResource(
			if (isSyncing) R.string.stop_sync_button
			else R.string.start_sync_button
		)

		val isSyncChangeEnabled by activeFileDownloadsViewModel.isSyncStateChangeEnabled.subscribeAsState()
		MenuIcon(
			onClick = { activeFileDownloadsViewModel.toggleSync() },
			icon = {
				var modifier: Modifier = Modifier.size(topMenuIconSize)

				if (isSyncing) {
					val infiniteTransition = rememberInfiniteTransition()
					val angle by infiniteTransition.animateFloat(
						initialValue = 360F,
						targetValue = 0F,
						animationSpec = infiniteRepeatable(
							animation = tween(2000, easing = LinearEasing)
						)
					)

					modifier = modifier.graphicsLayer {
						rotationZ = angle
					}
				}

				SyncIcon(
					isActive = isSyncing,
					modifier = modifier,
					contentDescription = label,
				)
			},
			label = { Text(text = label) },
			enabled = isSyncChangeEnabled,
			modifier = Modifier.requiredWidth(topMenuIconWidth),
		)
	}
}

@Composable
fun RenderTrackHeaderItem(
	activeFileDownloadsViewModel: ActiveFileDownloadsViewModel,
	trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewFileItem>,
	storedFile: StoredFile
) {
	val downloadingFileId by activeFileDownloadsViewModel.downloadingFileId.subscribeAsState()
	val fileItemViewModel = remember(trackHeadlineViewModelProvider::getViewModel)

	DisposableEffect(storedFile.serviceId) {
		activeFileDownloadsViewModel.activeLibraryId?.also {
			fileItemViewModel.promiseUpdate(it, ServiceFile(storedFile.serviceId))
		}

		onDispose {
			fileItemViewModel.reset()
		}
	}

	val fileName by fileItemViewModel.title.subscribeAsState()

	TrackTitleItemView(
		itemName = fileName,
		isActive = downloadingFileId == storedFile.id,
	)
}

@Composable
fun DownloadingFilesList(
	activeFileDownloadsViewModel: ActiveFileDownloadsViewModel,
	trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewFileItem>,
	modifier: Modifier = Modifier,
	headerHeight: Dp = 0.dp,
) {
	val files by activeFileDownloadsViewModel.downloadingFiles.subscribeAsState()
	val lazyListState = rememberLazyListState()

	LazyColumn(
		state = lazyListState,
		contentPadding = PaddingValues(top = headerHeight),
		modifier = modifier,
	) {
		item(contentType = ItemListContentType.Header) {
			Box(
				modifier = Modifier
					.padding(viewPaddingUnit)
					.height(48.dp)
			) {
				ProvideTextStyle(MaterialTheme.typography.h5) {
					Text(
						text = stringResource(R.string.file_count_label, files.size),
						fontWeight = FontWeight.Bold,
						modifier = Modifier
							.padding(viewPaddingUnit)
							.align(Alignment.CenterStart)
					)
				}
			}
		}

		itemsIndexed(
			files,
			{ _, f -> f.id },
			contentType = { _, _ -> ItemListContentType.File }) { i, f ->
			RenderTrackHeaderItem(
				activeFileDownloadsViewModel,
				trackHeadlineViewModelProvider,
				f,
			)

			if (i < files.lastIndex)
				Divider()
		}
	}
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun ScreenDimensionsScope.ActiveFileDownloadsView(
    activeFileDownloadsViewModel: ActiveFileDownloadsViewModel,
    trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewFileItem>,
    applicationNavigation: NavigateApplication,
) {
	ControlSurface {
		val isLoading by activeFileDownloadsViewModel.isLoading.subscribeAsState()

		BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
			if (maxWidth < Dimensions.twoColumnThreshold) {
				val heightScaler = LocalDensity.current.run {
					FullScreenScrollConnectedScaler.remember(min = appBarHeight.toPx(), max = boxHeight.toPx())
				}
				val topMenuHeightPx = LocalDensity.current.remember { topMenuHeight.toPx() }
				val menuHeightScaler = rememberDeferredPreScrollConnectedScaler(topMenuHeightPx, 0f)
				val compositeScroller = remember(heightScaler) {
					ConsumedOffsetErasingNestedScrollConnection(
						LinkedNestedScrollConnection(heightScaler, menuHeightScaler)
					)
				}

				Box(
					modifier = Modifier
						.fillMaxSize()
						.nestedScroll(compositeScroller)
				) {
					val listFocus = remember { FocusRequester() }
					if (isLoading) {
						ListLoading(modifier = Modifier.fillMaxSize())
					} else {
						DownloadingFilesList(
							activeFileDownloadsViewModel = activeFileDownloadsViewModel,
							trackHeadlineViewModelProvider = trackHeadlineViewModelProvider,
							modifier = Modifier.fillMaxSize().focusRequester(listFocus),
							headerHeight = Dimensions.expandedTitleHeight + Dimensions.appBarHeight + topMenuHeight + rowPadding * 2
						)
					}

					Column(
						modifier = Modifier
							.fillMaxWidth()
							.background(MaterialTheme.colors.surface)
							.focusProperties {
								onExit = {
									if (requestedFocusDirection == FocusDirection.Down)
										listFocus.requestFocus()
								}
							},
					) headerColumn@{
						val heightValue by heightScaler.valueState
						val heightValueDp by LocalDensity.current.remember { derivedStateOf { heightValue.toDp() } }
						Box(
							modifier = Modifier
								.fillMaxWidth()
								.requiredHeight(heightValueDp)
						) {
							val headerCollapseProgress by heightScaler.progressState
							val topPadding by remember {
								derivedStateOf {
									linearInterpolation(
										Dimensions.appBarHeight,
										14.dp,
										headerCollapseProgress
									)
								}
							}

							ProvideTextStyle(MaterialTheme.typography.h5) {
								val startPadding by rememberTitleStartPadding(heightScaler.progressState)
								val header = stringResource(id = R.string.activeDownloads)
								MarqueeText(
									text = header,
									overflow = TextOverflow.Ellipsis,
									gradientSides = setOf(GradientSide.End),
									gradientEdgeColor = MaterialTheme.colors.surface,
									modifier = Modifier
										.fillMaxWidth()
										.padding(start = startPadding, top = topPadding, end = viewPaddingUnit),
								)
							}

							// Always draw box to help the collapsing toolbar measure minimum size
							Box(modifier = Modifier.height(appBarHeight).fillMaxWidth()) {
								BackButton(
									applicationNavigation::navigateUp,
									Modifier
										.align(Alignment.CenterStart)
										.padding(topRowOuterPadding)
								)

								if (headerCollapseProgress > 0f) {
									val menuHeightProgress by menuHeightScaler.progressState
									val chevronRotation by remember {
										derivedStateOf { linearInterpolation(0f, 180f, menuHeightProgress) }
									}
									val isMenuFullyShown by remember { derivedStateOf { menuHeightProgress < .02f } }
									val chevronLabel =
										stringResource(id = if (isMenuFullyShown) R.string.collapse else R.string.expand)

									val scope = rememberCoroutineScope()

									UnlabelledChevronIcon(
										onClick = {
											if (headerCollapseProgress < 1f) return@UnlabelledChevronIcon
											scope.launch {
												if (!isMenuFullyShown) {
													menuHeightScaler.animateGoToMax()
												} else {
													menuHeightScaler.animateGoToMin()
												}
											}
										},
										chevronDescription = chevronLabel,
										modifier = Modifier
											.align(Alignment.TopEnd)
											.padding(
												vertical = topRowOuterPadding,
												horizontal = viewPaddingUnit * 2
											),
										chevronModifier = Modifier
											.rotate(chevronRotation)
											.alpha(headerCollapseProgress),
									)
								}
							}
						}

						val menuHeightValue by menuHeightScaler.valueState
						val menuHeightValueDp by LocalDensity.current.remember { derivedStateOf { menuHeightValue.toDp() } }
						Box(
							modifier = Modifier
								.fillMaxWidth()
								.background(MaterialTheme.colors.surface)
								.height(menuHeightValueDp)
								.clipToBounds()
						) {
							SyncMenu(activeFileDownloadsViewModel)
						}
					}
				}
			} else {
				Row(
					modifier = Modifier.fillMaxSize(),
				) {
					val menuWidth = this@ActiveFileDownloadsView.calculateSummaryColumnWidth()
					Column(
						modifier = Modifier.width(menuWidth),
					) {
						Column(
							modifier = Modifier.weight(1f)
						) {
							BackButton(
								applicationNavigation::navigateUp,
								modifier = Modifier.padding(topRowOuterPadding)
							)

							val header = stringResource(id = R.string.activeDownloads)

							ProvideTextStyle(MaterialTheme.typography.h5) {
								Text(
									text = header,
									maxLines = 2,
									overflow = TextOverflow.Ellipsis,
									modifier = Modifier
										.fillMaxWidth()
										.padding(horizontal = viewPaddingUnit),
								)
							}
						}

						SyncMenu(activeFileDownloadsViewModel)
					}

					if (isLoading) {
						ListLoading(modifier = Modifier.fillMaxSize())
					} else {
						DownloadingFilesList(
							activeFileDownloadsViewModel = activeFileDownloadsViewModel,
							trackHeadlineViewModelProvider = trackHeadlineViewModelProvider,
							modifier = Modifier.fillMaxSize(),
						)
					}
				}
			}
		}
	}
}
