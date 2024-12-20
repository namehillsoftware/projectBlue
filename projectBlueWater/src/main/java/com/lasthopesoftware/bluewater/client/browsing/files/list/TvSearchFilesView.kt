package com.lasthopesoftware.bluewater.client.browsing.files.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.list.ConnectionLostView
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.android.ui.components.BackButton
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions.expandedMenuVerticalPadding
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import com.lasthopesoftware.promises.extensions.suspend
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

private val searchFieldPadding = Dimensions.viewPaddingUnit * 4
private val textFieldHeight = TextFieldDefaults.MinHeight + TextFieldDefaults.FocusedBorderThickness * 2
private val topBarHeight = textFieldHeight + searchFieldPadding
private val minimumMenuWidth = (2 * 32).dp

@Composable
fun TvSearchFilesView(
	searchFilesViewModel: SearchFilesViewModel,
	nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
	trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	applicationNavigation: NavigateApplication,
	playbackServiceController: ControlPlaybackService,
) {
	val files by searchFilesViewModel.files.subscribeAsState()
	var isConnectionLost by remember { mutableStateOf(false) }
	val scope = rememberCoroutineScope()

	ControlSurface {
		val isLoading by searchFilesViewModel.isLoading.subscribeAsState()

		Column(
			modifier = Modifier.fillMaxSize()
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.requiredHeight(topBarHeight),
				horizontalArrangement = Arrangement.Center,
				verticalAlignment = Alignment.CenterVertically,
			) {
				BackButton(
					onBack = applicationNavigation::backOut,
					modifier = Modifier.padding(Dimensions.topRowOuterPadding)
				)

				val endPadding = Dimensions.topRowOuterPadding + minimumMenuWidth
				val query by searchFilesViewModel.query.subscribeAsState()
				val isLibraryIdActive by searchFilesViewModel.isLibraryIdActive.subscribeAsState()

				val focusRequester = remember { FocusRequester() }

				TextField(
					value = query,
					placeholder = { stringResource(id = R.string.lbl_search_hint) },
					onValueChange = { searchFilesViewModel.query.value = it },
					singleLine = true,
					keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
					keyboardActions = KeyboardActions(onSearch = {
						scope.launch {
							try {
								searchFilesViewModel.findFiles().suspend()
							} catch (e: IOException) {
								isConnectionLost =
									ConnectionLostExceptionFilter.isConnectionLostException(e)
							}
						}
					}),
					trailingIcon = {
						Icon(
							painter = painterResource(R.drawable.search_24dp),
							contentDescription = stringResource(id = R.string.search)
						)
					},
					enabled = isLibraryIdActive && !isLoading,
					modifier = Modifier
						.padding(end = endPadding)
						.weight(1f)
						.focusRequester(focusRequester),
				)

				LaunchedEffect(Unit) {
					delay(300)
					focusRequester.requestFocus()
				}
			}

			when {
				isLoading -> {
					Box(modifier = Modifier.fillMaxSize()) {
						CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
					}
				}

				isConnectionLost -> {
					ConnectionLostView(
						onCancel = { isConnectionLost = false },
						onRetry = {
							scope.launch {
								try {
									searchFilesViewModel.findFiles().suspend()
								} catch (e: IOException) {
									isConnectionLost =
										ConnectionLostExceptionFilter.isConnectionLostException(e)
								}
							}
						}
					)
				}

				files.any() -> {
					Row(
						modifier = Modifier
							.padding(
								top = expandedMenuVerticalPadding
							)
					) {
						LabelledPlayButton(
							libraryState = searchFilesViewModel,
							playbackServiceController = playbackServiceController,
							serviceFilesListState = searchFilesViewModel
						)

						LabelledShuffleButton(
							libraryState = searchFilesViewModel,
							playbackServiceController = playbackServiceController,
							serviceFilesListState = searchFilesViewModel
						)

						LabelledRefreshButton(searchFilesViewModel)
					}

					LazyColumn {
						item {
							Box(
								modifier = Modifier
									.padding(Dimensions.viewPaddingUnit)
									.height(Dimensions.menuHeight)
							) {
								ProvideTextStyle(MaterialTheme.typography.h5) {
									Text(
										text = stringResource(
											R.string.file_count_label,
											files.size
										),
										fontWeight = FontWeight.Bold,
										modifier = Modifier
											.padding(Dimensions.viewPaddingUnit)
											.align(Alignment.CenterStart)
									)
								}
							}
						}

						itemsIndexed(files) { i, f ->
							RenderTrackTitleItem(
								position = i,
								serviceFile = f,
								trackHeadlineViewModelProvider = trackHeadlineViewModelProvider,
								searchFilesViewModel = searchFilesViewModel,
								applicationNavigation = applicationNavigation,
								nowPlayingViewModel = nowPlayingViewModel,
								itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
								playbackServiceController = playbackServiceController,
							)

							if (i < files.lastIndex)
								Divider()
						}
					}
				}
			}
		}
	}
}
