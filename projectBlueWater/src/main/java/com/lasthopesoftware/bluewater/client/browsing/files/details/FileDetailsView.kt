package com.lasthopesoftware.bluewater.client.browsing.files.details

import android.graphics.Bitmap
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.components.BackButton
import com.lasthopesoftware.bluewater.android.ui.components.ColumnMenuIcon
import com.lasthopesoftware.bluewater.android.ui.components.GradientSide
import com.lasthopesoftware.bluewater.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.android.ui.components.RatingBar
import com.lasthopesoftware.bluewater.android.ui.components.UnlabelledRefreshButton
import com.lasthopesoftware.bluewater.android.ui.components.memorableScrollConnectedScaler
import com.lasthopesoftware.bluewater.android.ui.components.rememberTitleStartPadding
import com.lasthopesoftware.bluewater.android.ui.indicateFocus
import com.lasthopesoftware.bluewater.android.ui.linearInterpolation
import com.lasthopesoftware.bluewater.android.ui.navigable
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.android.ui.theme.DetermineWindowControlColors
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.appBarHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.menuHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.rowPadding
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topMenuIconSize
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topRowOuterPadding
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.viewPaddingUnit
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyType
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ReadOnlyFileProperty
import com.lasthopesoftware.bluewater.shared.NullBox
import com.lasthopesoftware.bluewater.shared.android.colors.MediaStylePalette
import com.lasthopesoftware.bluewater.shared.android.colors.MediaStylePaletteProvider
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.promises.extensions.toState
import com.lasthopesoftware.resources.bitmaps.ProduceBitmaps
import kotlinx.coroutines.launch

private val viewPadding = viewPaddingUnit

@Composable
private fun StaticFileMenu(viewModel: FileDetailsViewModel, mediaStylePalette: MediaStylePalette) {
	val padding = viewPadding * 3

	Row(
		modifier = Modifier
			.padding(top = padding)
			.height(menuHeight)
	) {
		val iconColor = mediaStylePalette.secondaryTextColor
		val iconSize = topMenuIconSize

		val addFileToPlaybackLabel = stringResource(id = R.string.btn_add_file_to_playback)
		val colorFilter = ColorFilter.tint(iconColor)
		ColumnMenuIcon(
			onClick = { viewModel.addToNowPlaying() },
			icon = {
				Image(
					painter = painterResource(id = R.drawable.ic_add_item_white_36dp),
					colorFilter = colorFilter,
					contentDescription = addFileToPlaybackLabel,
					modifier = Modifier
						.size(iconSize)
						.align(Alignment.CenterVertically),
				)
			},
			label = addFileToPlaybackLabel,
			labelMaxLines = 1,
			labelColor = iconColor
		)

		val playLabel = stringResource(id = R.string.btn_play)
		ColumnMenuIcon(
			onClick = { viewModel.play() },
			icon = {
				Image(
					painter = painterResource(id = R.drawable.av_play_white),
					colorFilter = colorFilter,
					contentDescription = playLabel,
					modifier = Modifier.size(iconSize),
				)
			},
			label = playLabel,
			labelMaxLines = 1,
			labelColor = iconColor
		)
	}
}

@Composable
fun FileRating(viewModel: FileDetailsViewModel, mediaStylePalette: MediaStylePalette, modifier: Modifier) {
	val rating by viewModel.rating.subscribeAsState()

	RatingBar(
		rating = rating,
		color = mediaStylePalette.primaryTextColor,
		backgroundColor = mediaStylePalette.primaryTextColor.copy(.1f),
		modifier = modifier
	)
}

@Composable
fun rememberComputedColorPalette(
	paletteProvider: MediaStylePaletteProvider,
	coverArt: Bitmap?
): State<MediaStylePalette> {
	val defaultMediaStylePalette = MediaStylePalette(
		MaterialTheme.colors.onPrimary,
		MaterialTheme.colors.onSecondary,
		MaterialTheme.colors.primary,
		MaterialTheme.colors.secondary
	)

	val coverArtColorsState = remember { mutableStateOf(defaultMediaStylePalette) }

	LaunchedEffect(coverArt) {
		coverArtColorsState.value =
			if (coverArt != null) paletteProvider.promisePalette(coverArt).suspend()
			else defaultMediaStylePalette
	}

	return coverArtColorsState
}

@Composable
fun FilePropertyHeader(
	viewModel: FileDetailsViewModel,
	palette: MediaStylePalette,
	modifier: Modifier = Modifier,
	isMarqueeEnabled: Boolean,
	titleFontSize: TextUnit = 24.sp
) {
	val fileName by viewModel.fileName.subscribeAsState(NullBox(stringResource(id = R.string.lbl_loading)))

	Column(modifier = modifier) {
		val gradientSides = setOf(GradientSide.End)

		Row {
			MarqueeText(
				text = fileName.value,
				color = palette.primaryTextColor,
				gradientEdgeColor = palette.backgroundColor,
				fontSize = titleFontSize,
				overflow = TextOverflow.Ellipsis,
				gradientSides = gradientSides,
				isMarqueeEnabled = isMarqueeEnabled,
			)
		}

		Row {
			val artist by viewModel.artist.subscribeAsState()
			MarqueeText(
				text = artist,
				color = palette.primaryTextColor,
				gradientEdgeColor = palette.backgroundColor,
				fontSize = 16.sp,
				overflow = TextOverflow.Ellipsis,
				gradientSides = gradientSides,
			)
		}
	}
}

@Composable
fun FilePropertyRow(
	viewModel: FileDetailsViewModel,
	property: FileDetailsViewModel.FilePropertyViewModel,
	palette: MediaStylePalette
) {
	val itemPadding = 2.dp

	val interactionSource = remember { MutableInteractionSource() }
	Row(
		modifier = Modifier
			.clickable(
				onClick = property::highlight,
				interactionSource = interactionSource,
				indication = null,
			)
			.indicateFocus(interactionSource),
	) {
		Text(
			text = property.property,
			color = palette.primaryTextColor,
			modifier = Modifier
				.weight(1f)
				.padding(
					start = viewPadding,
					top = itemPadding,
					end = itemPadding,
					bottom = itemPadding
				),
		)

		val propertyValue by property.committedValue.subscribeAsState()

		when (property.property) {
			NormalizedFileProperties.Rating -> {
				Box(
					modifier = Modifier
						.weight(2f)
						.align(Alignment.CenterVertically)
				) {
					val height = with(LocalDensity.current) {
						MaterialTheme.typography.h6.fontSize.toDp()
					}

					FileRating(
						viewModel,
						palette,
						modifier = Modifier
							.height(height)
							.align(Alignment.CenterStart)
							.padding(
								start = itemPadding,
								top = itemPadding,
								end = viewPadding,
								bottom = itemPadding,
							),
					)
				}
			}

			else -> {
				Text(
					text = propertyValue,
					color = palette.primaryTextColor,
					modifier = Modifier
						.weight(2f)
						.padding(
							start = itemPadding,
							top = itemPadding,
							end = viewPadding,
							bottom = itemPadding
						),
				)
			}
		}
	}
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FileDetailsEditor(
	viewModel: FileDetailsViewModel,
	navigateApplication: NavigateApplication,
	palette: MediaStylePalette
) {
	val maybeHighlightedFileProperty by viewModel.highlightedProperty.subscribeAsState()
	maybeHighlightedFileProperty?.let { fileProperty ->
		val property = fileProperty.property

		Dialog(onDismissRequest = fileProperty::cancel) {
			ControlSurface(
				color = palette.backgroundColor,
				contentColor = palette.primaryTextColor,
			) {
				Column(
					modifier = Modifier.padding(viewPaddingUnit * 2),
				) {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(viewPadding)
					) {
						ProvideTextStyle(MaterialTheme.typography.h5) {
							Text(
								text = property,
								modifier = Modifier
									.weight(1f)
									.align(Alignment.CenterVertically),
							)
						}

						Image(
							painter = painterResource(id = R.drawable.ic_remove_item_white_36dp),
							colorFilter = ColorFilter.tint(palette.secondaryTextColor),
							contentDescription = stringResource(id = R.string.btn_cancel),
							modifier = Modifier
								.navigable(onClick = fileProperty::cancel)
								.align(Alignment.CenterVertically),
						)
					}

					val fieldFocusRequester = remember { FocusRequester() }
					val propertyValueFlow = fileProperty.uncommittedValue
					val propertyValue by propertyValueFlow.subscribeAsState()
					val isEditing by fileProperty.isEditing.subscribeAsState()
					Box(
						modifier = Modifier
							.padding(viewPadding)
							.fillMaxWidth()
							.heightIn(100.dp, 300.dp),
						contentAlignment = Alignment.Center
					) {
						when {
							fileProperty.property == NormalizedFileProperties.Rating -> {
								val ratingValue by remember { derivedStateOf { propertyValue.toInt() } }
								RatingBar(
									rating = ratingValue,
									color = palette.primaryTextColor,
									backgroundColor = palette.primaryTextColor.copy(.1f),
									modifier = Modifier
										.height(36.dp)
										.align(Alignment.Center)
										.focusRequester(fieldFocusRequester),
									onRatingSelected = if (isEditing) {
										{ fileProperty.updateValue(it.toString()) }
									} else null
								)
							}

							fileProperty.editableType == FilePropertyType.LongFormText -> {
								TextField(
									value = propertyValue,
									enabled = isEditing,
									singleLine = false,
									minLines = 100,
									maxLines = 100,
									onValueChange = fileProperty::updateValue,
									modifier = Modifier
										.verticalScroll(rememberScrollState())
										.focusRequester(fieldFocusRequester)
								)
							}

							else -> {
								TextField(
									value = propertyValue,
									enabled = isEditing,
									singleLine = true,
									onValueChange = fileProperty::updateValue,
									modifier = Modifier.focusRequester(fieldFocusRequester),
								)
							}
						}

						if (isEditing) {
							DisposableEffect(key1 = Unit) {
								fieldFocusRequester.requestFocus()

								onDispose { }
							}
						}
					}

					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(viewPadding)
					) {
						Image(
							painter = painterResource(id = R.drawable.search_36dp),
							colorFilter = ColorFilter.tint(palette.secondaryTextColor),
							contentDescription = stringResource(id = R.string.search),
							modifier = Modifier
								.fillMaxWidth()
								.weight(1f)
								.navigable(
									onClick = {
										viewModel.activeLibraryId?.also {
											navigateApplication.search(
												it,
												ReadOnlyFileProperty(property, propertyValue)
											)
										}
									},
									enabled = !isEditing
								)
								.align(Alignment.CenterVertically),
						)

						when {
							isEditing -> {
								Image(
									painter = painterResource(id = R.drawable.ic_save_white_36dp),
									colorFilter = ColorFilter.tint(palette.secondaryTextColor),
									contentDescription = stringResource(id = R.string.save),
									modifier = Modifier
										.fillMaxWidth()
										.weight(1f)
										.navigable(onClick = fileProperty::commitChanges)
										.align(Alignment.CenterVertically),
								)
							}

							fileProperty.isEditable -> {
								Image(
									painter = painterResource(id = R.drawable.pencil),
									colorFilter = ColorFilter.tint(palette.secondaryTextColor),
									contentDescription = stringResource(id = R.string.edit),
									modifier = Modifier
										.fillMaxWidth()
										.weight(1f)
										.navigable(onClick = {
											fileProperty.edit()
										})
										.align(Alignment.CenterVertically),
								)
							}
						}
					}
				}
			}
		}
	}
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun FileDetailsView(
	viewModel: FileDetailsViewModel,
	navigateApplication: NavigateApplication,
	bitmapProducer: ProduceBitmaps
) {
	val activity = LocalActivity.current ?: return

	val paletteProvider = MediaStylePaletteProvider(activity)
	val coverArt by viewModel.coverArt.subscribeAsState()
	val coverArtBitmap by coverArt
		.takeIf { it.isNotEmpty() }
		?.let(bitmapProducer::promiseBitmap)
		.keepPromise()
		.toState(null, coverArt)

	val coverArtColorState by rememberComputedColorPalette(paletteProvider = paletteProvider, coverArt = coverArtBitmap)

	FileDetailsEditor(viewModel = viewModel, navigateApplication = navigateApplication, palette = coverArtColorState)

	val isLoading by viewModel.isLoading.subscribeAsState()

	@Composable
	fun fileDetailsSingleColumn() {
		val fileProperties by viewModel.fileProperties.subscribeAsState()

		val coverArtContainerHeight = 300.dp
		val coverArtBottomPadding = viewPadding + 8.dp
		val expandedTitlePadding = coverArtContainerHeight + coverArtBottomPadding
		val titleFontSize = MaterialTheme.typography.h5.fontSize
		val subTitleFontSize = MaterialTheme.typography.h6.fontSize
		val guessedRowSpacing = viewPaddingUnit
		val titleHeight =
			LocalDensity.current.run { titleFontSize.toDp() + subTitleFontSize.toDp() } + guessedRowSpacing * 3
		val boxHeight = expandedTitlePadding + titleHeight
		val boxHeightPx = LocalDensity.current.run { boxHeight.toPx() }
		val collapsedHeight = appBarHeight + rowPadding
		val heightScaler = memorableScrollConnectedScaler(max = boxHeightPx, min = LocalDensity.current.run { collapsedHeight.toPx() })

		val lazyListState = rememberLazyListState()

		Box(
			modifier = Modifier
				.fillMaxSize()
				.nestedScroll(heightScaler)
		) {
			val headerCollapseProgress by heightScaler.getProgressState()

			if (!isLoading) {
				LazyColumn(modifier = Modifier.fillMaxSize(), state = lazyListState) {
					item {
						val coverArtTopPadding = viewPadding + appBarHeight

						Box(
							modifier = Modifier
								.requiredHeight(coverArtContainerHeight)
								.padding(
									top = coverArtTopPadding,
									start = viewPadding * 11,
									end = viewPadding * 11,
									bottom = coverArtBottomPadding
								)
								.fillMaxWidth()
						) {
							val coverArtState by remember { derivedStateOf { coverArtBitmap?.asImageBitmap() } }

							coverArtState
								?.also {
									val album by viewModel.album.subscribeAsState()
									val artist by viewModel.artist.subscribeAsState()
									Image(
										bitmap = it,
										contentDescription = stringResource(
											id = R.string.lbl_cover_art,
											album,
											artist
										),
										contentScale = ContentScale.FillHeight,
										modifier = Modifier
											.clip(RoundedCornerShape(5.dp))
											.border(
												1.dp,
												shape = RoundedCornerShape(5.dp),
												color = coverArtColorState.secondaryTextColor
											)
											.fillMaxHeight()
											.align(Alignment.Center),
									)
								}
						}
					}

					stickyHeader {
						Spacer(
							modifier = Modifier
								.requiredHeight(appBarHeight)
								.fillMaxWidth()
						)
					}

					item {
						Row(
							modifier = Modifier
								.padding(rowPadding)
								.fillMaxWidth()
								.align(Alignment.TopEnd),
							horizontalArrangement = Arrangement.SpaceEvenly,
						) {
							val chevronRotation by remember { derivedStateOf { 180 * headerCollapseProgress } }
							val isCollapsed by remember { derivedStateOf { headerCollapseProgress > .98f } }

							val chevronLabel =
								stringResource(id = if (isCollapsed) R.string.expand else R.string.collapse)
							val scope = rememberCoroutineScope()

							ColumnMenuIcon(
								onClick = {
									scope.launch {
										if (isCollapsed) {
											heightScaler.goToMax()
											lazyListState.scrollToItem(0)
										} else {
											heightScaler.goToMin()
											lazyListState.scrollToItem(1)
										}
									}
								},
								icon = {
									Image(
										painter = painterResource(id = R.drawable.chevron_up_white_36dp),
										colorFilter = ColorFilter.tint(coverArtColorState.secondaryTextColor),
										contentDescription = chevronLabel,
										modifier = Modifier
											.size(topMenuIconSize)
											.rotate(chevronRotation),
									)
								},
								label = chevronLabel,
								labelColor = coverArtColorState.secondaryTextColor,
								labelMaxLines = 1,
							)

							val addFileToPlaybackLabel = stringResource(id = R.string.btn_add_file_to_playback)
							ColumnMenuIcon(
								onClick = { viewModel.addToNowPlaying() },
								icon = {
									Image(
										painter = painterResource(id = R.drawable.ic_add_item_white_36dp),
										colorFilter = ColorFilter.tint(coverArtColorState.secondaryTextColor),
										contentDescription = addFileToPlaybackLabel,
										modifier = Modifier.size(topMenuIconSize),
									)
								},
								label = addFileToPlaybackLabel,
								labelColor = coverArtColorState.secondaryTextColor,
								labelMaxLines = 1,
							)

							val playLabel = stringResource(id = R.string.btn_play)
							ColumnMenuIcon(
								onClick = { viewModel.play() },
								icon = {
									Image(
										painter = painterResource(id = R.drawable.av_play_white),
										colorFilter = ColorFilter.tint(coverArtColorState.secondaryTextColor),
										contentDescription = playLabel,
										modifier = Modifier.size(topMenuIconSize),
									)
								},
								label = playLabel,
								labelColor = coverArtColorState.secondaryTextColor,
								labelMaxLines = 1,
							)
						}
					}

					items(fileProperties) {
						FilePropertyRow(viewModel, it, coverArtColorState)
					}
				}
			} else {
				CircularProgressIndicator(
					color = coverArtColorState.primaryTextColor,
					modifier = Modifier.align(Alignment.Center)
				)
			}

			Box(
				modifier = Modifier
					.height(appBarHeight)
					.fillMaxWidth()
					.align(Alignment.TopStart)
					.background(coverArtColorState.backgroundColor),
			) {
				BackButton(
					onBack = navigateApplication::navigateUp,
					modifier = Modifier
						.padding(topRowOuterPadding)
						.align(Alignment.CenterStart)
				)

				UnlabelledRefreshButton(
					onClick = {
						viewModel.promiseLoadedActiveFile()
					},
					modifier = Modifier
						.padding(
							vertical = topRowOuterPadding,
							horizontal = viewPaddingUnit * 2
						)
						.align(Alignment.CenterEnd)
				)
			}

			if (!isLoading) {
				val topTitlePadding by remember {
					derivedStateOf {
						linearInterpolation(expandedTitlePadding, 0.dp, headerCollapseProgress)
					}
				}
				Box(
					modifier = Modifier
						.padding(top = topTitlePadding)
						.requiredHeight(appBarHeight)
						.fillMaxWidth(),
					contentAlignment = Alignment.CenterStart,
				) {
					val startPadding by rememberTitleStartPadding(heightScaler.getProgressState())
					val endPadding by remember {
						derivedStateOf {
							linearInterpolation(
								viewPadding,
								topMenuIconSize + viewPaddingUnit * 4,
								headerCollapseProgress
							)
						}
					}
					FilePropertyHeader(
						viewModel,
						coverArtColorState,
						modifier = Modifier.padding(start = startPadding, end = endPadding),
						titleFontSize = titleFontSize,
						isMarqueeEnabled = !lazyListState.isScrollInProgress
					)
				}
			}
		}
	}

	@Composable
	fun BoxWithConstraintsScope.fileDetailsTwoColumn() {
		if (!isLoading) {
			Row(modifier = Modifier.fillMaxSize()) {
				Column(
					modifier = Modifier
						.fillMaxHeight()
						.width(250.dp)
						.padding(
							start = viewPadding,
							end = viewPadding * 2,
							bottom = viewPadding,
							top = viewPadding,
						)
				) {
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.weight(1.0f)
							.align(Alignment.CenterHorizontally)
					) {
						val coverArtState by remember {
							derivedStateOf {
								coverArtBitmap
									?.asImageBitmap()
							}
						}

						coverArtState
							?.let {
								val artist by viewModel.artist.subscribeAsState()
								val album by viewModel.album.subscribeAsState()

								Image(
									bitmap = it,
									contentDescription = stringResource(
										id = R.string.lbl_cover_art,
										album,
										artist
									),
									contentScale = ContentScale.FillWidth,
									modifier = Modifier
										.fillMaxWidth()
										.clip(RoundedCornerShape(5.dp))
										.align(Alignment.Center)
										.border(
											1.dp,
											shape = RoundedCornerShape(5.dp),
											color = coverArtColorState.secondaryTextColor
										),
								)
							}
					}

					StaticFileMenu(viewModel, coverArtColorState)
				}

				Column(modifier = Modifier.fillMaxWidth()) {
					val fileProperties by viewModel.fileProperties.subscribeAsState()
					val lazyListState = rememberLazyListState()

					Row(
						verticalAlignment = Alignment.CenterVertically,
						modifier = Modifier.height(appBarHeight),
					) {
						FilePropertyHeader(
							viewModel,
							coverArtColorState,
							modifier = Modifier
								.background(coverArtColorState.backgroundColor)
								.padding(horizontal = viewPaddingUnit)
								.fillMaxWidth()
								.weight(1f),
							isMarqueeEnabled = !lazyListState.isScrollInProgress
						)

						UnlabelledRefreshButton(
							onClick = {
								viewModel.promiseLoadedActiveFile()
							},
							modifier = Modifier.padding(horizontal = viewPaddingUnit * 2)
						)

						Image(
							painter = painterResource(id = R.drawable.ic_remove_item_white_36dp),
							contentDescription = "Close",
							colorFilter = ColorFilter.tint(coverArtColorState.secondaryTextColor),
							modifier = Modifier
								.padding(horizontal = viewPaddingUnit * 2)
								.navigable(onClick = navigateApplication::navigateUp),
						)
					}

					LazyColumn(modifier = Modifier.weight(1f), state = lazyListState) {
						items(fileProperties) {
							FilePropertyRow(viewModel, it, coverArtColorState)
						}
					}
				}
			}
		} else {
			CircularProgressIndicator(
				color = coverArtColorState.primaryTextColor,
				modifier = Modifier.align(Alignment.Center)
			)
		}
	}

	DetermineWindowControlColors(coverArtColorState.backgroundColor)

	Box(modifier = Modifier
		.fillMaxSize()
		.background(coverArtColorState.actionBarColor)
	) {
		val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

		ControlSurface(
			color = coverArtColorState.backgroundColor,
			contentColor = coverArtColorState.primaryTextColor,
			controlColor = coverArtColorState.secondaryTextColor,
			modifier = Modifier.padding(systemBarsPadding)
		) {
			BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
				when {
					maxWidth >= 450.dp -> fileDetailsTwoColumn()
					else -> fileDetailsSingleColumn()
				}
			}
		}
	}
}
