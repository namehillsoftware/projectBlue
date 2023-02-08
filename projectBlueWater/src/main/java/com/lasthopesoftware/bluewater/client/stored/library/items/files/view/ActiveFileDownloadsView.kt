package com.lasthopesoftware.bluewater.client.stored.library.items.files.view

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.TrackHeaderItemView
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewFileItem
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncButton
import com.lasthopesoftware.bluewater.shared.android.ui.components.GradientSide
import com.lasthopesoftware.bluewater.shared.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.shared.android.ui.components.scrollbar
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import kotlin.math.pow

@Composable
fun ActiveFileDownloadsView(
	activeFileDownloadsViewModel: ActiveFileDownloadsViewModel,
	trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewFileItem>,
	onBack: (() -> Unit)? = null,
) {
	@Composable
	fun RenderTrackHeaderItem(storedFile: StoredFile) {
		val downloadingFileId by activeFileDownloadsViewModel.downloadingFileId.collectAsState()
		val fileItemViewModel = remember(trackHeadlineViewModelProvider::getViewModel)

		DisposableEffect(storedFile.serviceId) {
			fileItemViewModel.promiseUpdate(ServiceFile(storedFile.serviceId))

			onDispose {
				fileItemViewModel.reset()
			}
		}

		val fileName by fileItemViewModel.title.collectAsState()

		TrackHeaderItemView(
			itemName = fileName,
			isActive = downloadingFileId == storedFile.id,
		)
	}

	Surface {
		val toolbarState = rememberCollapsingToolbarScaffoldState()
		val headerHidingProgress by remember { derivedStateOf(structuralEqualityPolicy()) { 1 - toolbarState.toolbarState.progress } }
		val isLoading by activeFileDownloadsViewModel.isLoading.collectAsState()

		CollapsingToolbarScaffold(
			enabled = true,
			state = toolbarState,
			scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
			modifier = Modifier.fillMaxSize(),
			toolbar = {
				val appBarHeight = 56
				val topPadding by remember { derivedStateOf(structuralEqualityPolicy()) { (appBarHeight - 46 * headerHidingProgress).dp } }
				val expandedTitleHeight = 84
				val expandedIconSize = 36
				val expandedMenuVerticalPadding = 12
				val boxHeight =
					expandedTitleHeight + expandedIconSize + expandedMenuVerticalPadding * 2 + appBarHeight
				BoxWithConstraints(
					modifier = Modifier
						.height(boxHeight.dp)
						.padding(top = topPadding)
				) {
					val minimumMenuWidth = (3 * 32).dp
					val acceleratedProgress by remember {
						derivedStateOf(structuralEqualityPolicy()) {
							1 - toolbarState.toolbarState.progress.pow(
								3
							).coerceIn(0f, 1f)
						}
					}
					ProvideTextStyle(MaterialTheme.typography.h5) {
						val iconClearance = if (onBack != null) 48 else 0
						val startPadding by remember {  derivedStateOf(structuralEqualityPolicy()) { (4 + iconClearance * headerHidingProgress).dp } }
						val endPadding by remember { derivedStateOf(structuralEqualityPolicy()) { 4.dp + minimumMenuWidth * acceleratedProgress } }
						val header = stringResource(id = R.string.activeDownloads)
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

					val menuWidth by remember { derivedStateOf(structuralEqualityPolicy()) { (maxWidth - (maxWidth - minimumMenuWidth) * acceleratedProgress) } }
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
						val iconSize by remember { derivedStateOf { (expandedIconSize - (12 * headerHidingProgress)).dp } }

						val isSyncing by activeFileDownloadsViewModel.isSyncing.collectAsState()

						var modifier = Modifier
							.fillMaxWidth()
							.size(iconSize)
							.clickable { activeFileDownloadsViewModel.toggleSync() }
							.weight(1f)

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

						SyncButton(
							isActive = isSyncing,
							modifier = modifier,
						)
					}
				}

				// Always draw box to help the collapsing toolbar measure minimum size
				Box(modifier = Modifier.height(appBarHeight.dp)) {
					if (onBack != null) {
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
									onClick = onBack
								)
						)
					}
				}
			},
		) {
			if (isLoading) {
				Box(modifier = Modifier.fillMaxSize()) {
					CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
				}
			} else {
				BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
					val rowHeight = dimensionResource(id = R.dimen.standard_row_height)
					val fileMap by activeFileDownloadsViewModel.downloadingFiles.collectAsState()
					val files by remember { derivedStateOf(referentialEqualityPolicy()) { fileMap.values.toList() } }
					val lazyListState = rememberSaveable(files, saver = LazyListState.Saver) { LazyListState() }
					val knobHeight by remember {
						derivedStateOf {
							lazyListState.layoutInfo.totalItemsCount
								.takeIf { it > 0 }
								?.let { maxHeight / (rowHeight * it) }
								?.takeIf { it > 0 && it < 1 }
						}
					}

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

						itemsIndexed(files) { i, f ->
							RenderTrackHeaderItem(f)

							if (i < files.lastIndex)
								Divider()
						}
					}
				}
			}
		}
	}
}
