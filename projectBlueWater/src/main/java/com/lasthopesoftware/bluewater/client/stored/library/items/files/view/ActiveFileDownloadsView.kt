package com.lasthopesoftware.bluewater.client.stored.library.items.files.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.TrackHeaderItemView
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewFileItem
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.android.ui.components.scrollbar
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Light
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
	val files by activeFileDownloadsViewModel.downloadingFiles.collectAsState()
	val downloadingFileId by activeFileDownloadsViewModel.downloadingFileId.collectAsState()

	@Composable
	fun RenderTrackHeaderItem(storedFile: StoredFile) {
		val fileItemViewModel = remember(trackHeadlineViewModelProvider::getViewModel)

		DisposableEffect(Unit) {
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
		val headerHidingProgress by remember { derivedStateOf { 1 - toolbarState.toolbarState.progress } }
		val isLoading by activeFileDownloadsViewModel.isLoading.collectAsState()

		CollapsingToolbarScaffold(
			enabled = true,
			state = toolbarState,
			scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
			modifier = Modifier.fillMaxSize(),
			toolbar = {
				val appBarHeight = 56
				val searchFieldPadding = 16
				val minimumMenuWidth = (2 * 32).dp

				val expandedIconSize = 36
				val expandedMenuVerticalPadding = 4
				val boxHeight = appBarHeight + expandedIconSize + expandedMenuVerticalPadding * 2 + searchFieldPadding * 2

				val acceleratedProgress by remember {
					derivedStateOf {
						1 - toolbarState.toolbarState.progress.pow(
							5
						).coerceIn(0f, 1f)
					}
				}

				BoxWithConstraints(
					modifier = Modifier
						.height(boxHeight.dp)
						.fillMaxWidth()
				) {
					if (files.any()) {

						val collapsedIconSize = 24
						val menuWidth by remember { derivedStateOf { (maxWidth - (maxWidth - minimumMenuWidth) * acceleratedProgress) } }
						val expandedTopRowPadding = appBarHeight + expandedMenuVerticalPadding + searchFieldPadding * 2
						val collapsedTopRowPadding = searchFieldPadding + appBarHeight / 2 - collapsedIconSize / 2
						val topRowPadding by remember { derivedStateOf { (expandedTopRowPadding - (expandedTopRowPadding - collapsedTopRowPadding) * headerHidingProgress).dp } }
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
							val iconSize by remember { derivedStateOf { (expandedIconSize - ((expandedIconSize - collapsedIconSize) * headerHidingProgress)).dp } }

							val isSyncing by activeFileDownloadsViewModel.isSyncing.collectAsState()

							Image(
								painter = painterResource(id = R.drawable.ic_sync_white),
								contentDescription = stringResource(id = R.string.btn_sync_item),
								colorFilter = ColorFilter.tint(if (isSyncing) MaterialTheme.colors.primary else Light.GrayClickable),
								alpha = if (isSyncing) .9f else .6f,
								modifier = Modifier
									.fillMaxWidth()
									.size(iconSize)
									.clickable { activeFileDownloadsViewModel.toggleSync() }
									.weight(1f),
							)
						}
					}
				}

				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(searchFieldPadding.dp)
						.height(appBarHeight.dp),
					horizontalArrangement = Arrangement.Center,
				) {
					if (onBack != null) {
						Icon(
							Icons.Default.ArrowBack,
							contentDescription = "",
							tint = MaterialTheme.colors.onSurface,
							modifier = Modifier
								.padding(16.dp)
								.align(Alignment.CenterVertically)
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

			when {
				isLoading -> {
					Box(modifier = Modifier.fillMaxSize()) {
						CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
					}
				}
				files.any() -> {
					BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
						val rowHeight = dimensionResource(id = R.dimen.standard_row_height)
						val lazyListState = rememberLazyListState()
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
				else -> {
					Spacer(modifier = Modifier.fillMaxSize())
				}
			}
		}
	}
}
