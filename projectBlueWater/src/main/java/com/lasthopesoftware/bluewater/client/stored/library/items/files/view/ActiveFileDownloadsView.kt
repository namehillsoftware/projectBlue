package com.lasthopesoftware.bluewater.client.stored.library.items.files.view

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.TrackTitleItemView
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewFileItem
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncIcon
import com.lasthopesoftware.bluewater.shared.android.ui.components.BackButton
import com.lasthopesoftware.bluewater.shared.android.ui.components.GradientSide
import com.lasthopesoftware.bluewater.shared.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.shared.android.ui.components.MenuIcon
import com.lasthopesoftware.bluewater.shared.android.ui.components.memorableScrollConnectedScaler
import com.lasthopesoftware.bluewater.shared.android.ui.components.rememberCalculatedKnobHeight
import com.lasthopesoftware.bluewater.shared.android.ui.components.rememberTitleStartPadding
import com.lasthopesoftware.bluewater.shared.android.ui.components.scrollbar
import com.lasthopesoftware.bluewater.shared.android.ui.linearInterpolation
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions.topMenuIconSize
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState

private val expandedTitleHeight = Dimensions.expandedTitleHeight
private val appBarHeight = Dimensions.appBarHeight
private val boxHeight = expandedTitleHeight + appBarHeight

@Composable
fun ActiveFileDownloadsView(
	activeFileDownloadsViewModel: ActiveFileDownloadsViewModel,
	trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewFileItem>,
	applicationNavigation: NavigateApplication,
) {
	@Composable
	fun RenderTrackHeaderItem(storedFile: StoredFile) {
		val downloadingFileId by activeFileDownloadsViewModel.downloadingFileId.collectAsState()
		val fileItemViewModel = remember(trackHeadlineViewModelProvider::getViewModel)

		DisposableEffect(storedFile.serviceId) {
			activeFileDownloadsViewModel.activeLibraryId?.also {
				fileItemViewModel.promiseUpdate(it, ServiceFile(storedFile.serviceId))
			}

			onDispose {
				fileItemViewModel.reset()
			}
		}

		val fileName by fileItemViewModel.title.collectAsState()

		TrackTitleItemView(
			itemName = fileName,
			isActive = downloadingFileId == storedFile.id,
		)
	}

	ControlSurface {
		val isLoading by activeFileDownloadsViewModel.isLoading.subscribeAsState()

		val heightScaler = LocalDensity.current.run {
			memorableScrollConnectedScaler(max = boxHeight.toPx(), min = appBarHeight.toPx())
		}

		Box(
			modifier = Modifier
				.fillMaxSize()
				.nestedScroll(heightScaler)
		) {
			val headerCollapseProgress by heightScaler.getProgressState()

			if (isLoading) {
				Box(modifier = Modifier.fillMaxSize()) {
					CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
				}
			} else {
				BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
					val rowHeight = Dimensions.standardRowHeight
					val fileMap by activeFileDownloadsViewModel.downloadingFiles.collectAsState()
					val files by remember { derivedStateOf(referentialEqualityPolicy()) { fileMap.values.toList() } }
					val lazyListState = rememberLazyListState()
					val knobHeight by rememberCalculatedKnobHeight(lazyListState, rowHeight)

					LazyColumn(
						state = lazyListState,
						modifier = Modifier
							.scrollbar(
								lazyListState,
								horizontal = false,
								knobColor = MaterialTheme.colors.onSurface,
								trackColor = Color.Transparent,
								visibleAlpha = .4f,
								knobCornerRadius = 1.dp,
								fixedKnobRatio = knobHeight,
							),
					) {
						item {
							Spacer(modifier = Modifier.fillMaxWidth().height(boxHeight))
						}

						item {
							Box(
								modifier = Modifier
									.padding(4.dp)
									.height(48.dp)
							) {
								ProvideTextStyle(MaterialTheme.typography.h5) {
									Text(
										text = stringResource(R.string.file_count_label, files.size),
										fontWeight = FontWeight.Bold,
										modifier = Modifier
											.padding(4.dp)
											.align(Alignment.CenterStart)
									)
								}
							}
						}

						itemsIndexed(files, { _, f -> f.id }) { i, f ->
							RenderTrackHeaderItem(f)

							if (i < files.lastIndex)
								Divider()
						}
					}
				}
			}

			Column(
				modifier = Modifier
					.fillMaxWidth()
					.background(MaterialTheme.colors.surface),
			) {
				val heightValue by heightScaler.getValueState()
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.height(LocalDensity.current.run { heightValue.toDp() })
				) {
					val topPadding by remember { derivedStateOf { linearInterpolation(Dimensions.appBarHeight, 14.dp, headerCollapseProgress) } }

					ProvideTextStyle(MaterialTheme.typography.h5) {
						val startPadding by rememberTitleStartPadding(heightScaler.getProgressState())
						val header = stringResource(id = R.string.activeDownloads)
						MarqueeText(
							text = header,
							overflow = TextOverflow.Ellipsis,
							gradientSides = setOf(GradientSide.End),
							gradientEdgeColor = MaterialTheme.colors.surface,
							modifier = Modifier
								.fillMaxWidth()
								.padding(start = startPadding, top = topPadding, end = Dimensions.viewPaddingUnit),
						)
					}

					// Always draw box to help the collapsing toolbar measure minimum size
					Box(modifier = Modifier.height(appBarHeight)) {
						BackButton(
							applicationNavigation::backOut,
							Modifier
								.align(Alignment.CenterStart)
								.clickable(
									interactionSource = remember { MutableInteractionSource() },
									indication = null,
									onClick = applicationNavigation::backOut
								)
								.padding(Dimensions.topRowOuterPadding)
						)
					}
				}

				val menuHeight by remember {
					derivedStateOf {
						linearInterpolation(Dimensions.menuHeight, topMenuIconSize, headerCollapseProgress)
					}
				}

				Row(
					modifier = Modifier
						.padding(Dimensions.rowPadding)
						.height(menuHeight)
						.fillMaxWidth()
				) {
					val isSyncing by activeFileDownloadsViewModel.isSyncing.collectAsState()
					val label = stringResource(
						if (isSyncing) R.string.stop_sync_button
						else R.string.start_sync_button
					)

					val isSyncChangeEnabled by activeFileDownloadsViewModel.isSyncStateChangeEnabled.collectAsState()
					MenuIcon(
						onClick = { activeFileDownloadsViewModel.toggleSync() },
						icon =  {
							var modifier = Modifier.size(topMenuIconSize)

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
						modifier = Modifier.fillMaxHeight().weight(1f),
						label = {
							if (headerCollapseProgress < 1) {
								val invertedProgress by remember { derivedStateOf { 1 - headerCollapseProgress } }
								Text(
									text = label,
									modifier = Modifier.alpha(invertedProgress),
								)
							}
						},
						enabled = isSyncChangeEnabled,
					)
				}
			}
		}
	}
}
