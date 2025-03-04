package com.lasthopesoftware.bluewater.client.browsing.files.details

import android.graphics.Bitmap
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
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
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyType
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.shared.NullBox
import com.lasthopesoftware.bluewater.shared.android.colors.MediaStylePalette
import com.lasthopesoftware.bluewater.shared.android.colors.MediaStylePaletteProvider
import com.lasthopesoftware.bluewater.shared.android.ui.components.BackButton
import com.lasthopesoftware.bluewater.shared.android.ui.components.ColumnMenuIcon
import com.lasthopesoftware.bluewater.shared.android.ui.components.GradientSide
import com.lasthopesoftware.bluewater.shared.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.shared.android.ui.components.RatingBar
import com.lasthopesoftware.bluewater.shared.android.ui.components.memorableScrollConnectedScaler
import com.lasthopesoftware.bluewater.shared.android.ui.components.rememberSystemUiController
import com.lasthopesoftware.bluewater.shared.android.ui.indicateFocus
import com.lasthopesoftware.bluewater.shared.android.ui.navigable
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.promises.extensions.toState
import com.lasthopesoftware.resources.bitmaps.ProduceBitmaps
import kotlinx.coroutines.launch
import kotlin.math.pow

private val viewPadding = Dimensions.viewPaddingUnit

@Composable
private fun StaticFileMenu(viewModel: FileDetailsViewModel, mediaStylePalette: MediaStylePalette) {
	val padding = viewPadding * 3

	Row(
		modifier = Modifier
			.padding(top = padding)
			.height(Dimensions.menuHeight)
	) {
		val iconColor = mediaStylePalette.secondaryTextColor
		val iconSize = Dimensions.topMenuIconSize

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
fun FilePropertyHeader(viewModel: FileDetailsViewModel, palette: MediaStylePalette, modifier: Modifier = Modifier, isMarqueeEnabled: Boolean, titleFontSize: TextUnit = 24.sp) {
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
fun FilePropertyRow(viewModel: FileDetailsViewModel, property: FileDetailsViewModel.FilePropertyViewModel, palette: MediaStylePalette) {
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
			KnownFileProperties.Rating -> {
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
					modifier = Modifier.padding(Dimensions.viewPaddingUnit * 2),
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
							fileProperty.property == KnownFileProperties.Rating -> {
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

								onDispose {  }
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
											navigateApplication.search(it, FileProperty(property, propertyValue))
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
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
fun FileDetailsView(viewModel: FileDetailsViewModel, navigateApplication: NavigateApplication, bitmapProducer: ProduceBitmaps) {
	val activity = LocalActivity.current ?: return

	val paletteProvider = MediaStylePaletteProvider(activity)
	val coverArt by viewModel.coverArt.subscribeAsState()
	val coverArtBitmap by coverArt
		.takeIf { it.isNotEmpty() }
		?.let(bitmapProducer::promiseBitmap)
		.keepPromise()
		.toState(null, coverArt)

	val coverArtColorState by rememberComputedColorPalette(paletteProvider = paletteProvider, coverArt = coverArtBitmap)

	val systemUiController = rememberSystemUiController()
	systemUiController.setStatusBarColor(coverArtColorState.actionBarColor)

	FileDetailsEditor(viewModel = viewModel, navigateApplication = navigateApplication, palette = coverArtColorState)

	val isLoading by viewModel.isLoading.subscribeAsState()

	@Composable
	fun BoxWithConstraintsScope.fileDetailsSingleColumn() {
		val fileProperties by viewModel.fileProperties.subscribeAsState()

		val coverArtContainerHeight = 300.dp
		val appBarHeight = Dimensions.appBarHeight
		val coverArtBottomPadding = viewPadding + 8.dp
		val expandedTitlePadding = coverArtContainerHeight + coverArtBottomPadding
		val titleFontSize = MaterialTheme.typography.h5.fontSize
		val subTitleFontSize = MaterialTheme.typography.h6.fontSize
		val guessedRowSpacing = Dimensions.viewPaddingUnit
		val titleHeight =
			LocalDensity.current.run { titleFontSize.toDp() + subTitleFontSize.toDp() } + guessedRowSpacing * 3
		val expandedIconSize = Dimensions.menuHeight
		val expandedMenuVerticalPadding = Dimensions.viewPaddingUnit * 3
		val boxHeight =
			expandedTitlePadding + titleHeight + expandedIconSize + expandedMenuVerticalPadding * 2
		val boxHeightPx = LocalDensity.current.run { boxHeight.toPx() }
		val heightScaler =
			memorableScrollConnectedScaler(max = boxHeightPx, min = LocalDensity.current.run { appBarHeight.toPx() })

		if (!isLoading) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.nestedScroll(heightScaler)
			) {
				val lazyListState = rememberLazyListState()

				LazyColumn(modifier = Modifier.fillMaxSize(), state = lazyListState) {
					item {
						Spacer(
							modifier = Modifier
								.requiredHeight(boxHeight - appBarHeight)
								.fillMaxWidth()
						)
					}

					stickyHeader {
						Spacer(
							modifier = Modifier
								.requiredHeight(appBarHeight)
								.fillMaxWidth()
						)
					}

					items(fileProperties) {
						FilePropertyRow(viewModel, it, coverArtColorState)
					}
				}

				val heightValue by heightScaler.getValueState()
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.align(Alignment.TopStart)
						.background(coverArtColorState.backgroundColor)
						.height(LocalDensity.current.run { heightValue.toDp() })
				) {
					val coverArtTopPadding = viewPadding + appBarHeight

					val headerCollapseProgress by heightScaler.getProgressState()
					val coverArtScrollOffset by remember { derivedStateOf { -coverArtContainerHeight * headerCollapseProgress } }
					Box(
						modifier = Modifier
							.requiredHeight(coverArtContainerHeight)
							.padding(
								top = coverArtTopPadding,
								start = viewPadding + 40.dp,
								end = viewPadding + 40.dp,
							)
							.offset { IntOffset(x = 0, y = coverArtScrollOffset.roundToPx()) }
							.fillMaxWidth()
					) {
						val coverArtState by remember { derivedStateOf { coverArtBitmap?.asImageBitmap() } }

						coverArtState
							?.let {
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

					val headerExpandProgress by remember { derivedStateOf { 1 - headerCollapseProgress } }
					val topTitlePadding by remember { derivedStateOf { expandedTitlePadding * headerExpandProgress } }
					BoxWithConstraints(
						modifier = Modifier
							.height(boxHeight)
							.padding(top = topTitlePadding)
							.fillMaxWidth()
					) {
						val minimumMenuWidth = (3 * 32).dp

						val acceleratedToolbarStateProgress by remember {
							derivedStateOf {
								headerExpandProgress.pow(3).coerceIn(0f, 1f)
							}
						}

						val acceleratedHeaderHidingProgress by remember { derivedStateOf { 1 - acceleratedToolbarStateProgress } }

						val startPadding by remember { derivedStateOf { viewPadding + 48.dp * headerCollapseProgress } }
						val endPadding by remember { derivedStateOf { viewPadding + minimumMenuWidth * acceleratedHeaderHidingProgress } }
						FilePropertyHeader(
							viewModel,
							coverArtColorState,
							modifier = Modifier.padding(start = startPadding, end = endPadding),
							titleFontSize = titleFontSize,
							isMarqueeEnabled = !lazyListState.isScrollInProgress
						)

						val menuWidth by remember { derivedStateOf { (maxWidth - (maxWidth - minimumMenuWidth) * acceleratedHeaderHidingProgress) } }
						val expandedTopRowPadding = titleHeight + expandedMenuVerticalPadding
						val topRowPadding by remember { derivedStateOf { expandedTopRowPadding - (expandedTopRowPadding - 14.dp) * headerCollapseProgress } }
						Row(
							modifier = Modifier
								.padding(top = topRowPadding, start = 8.dp, end = 8.dp)
								.width(menuWidth)
								.align(Alignment.TopEnd)
						) {
							val iconSize = Dimensions.topMenuIconSize
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
											.size(iconSize)
											.rotate(chevronRotation),
									)
								},
								label = if (acceleratedHeaderHidingProgress < 1) chevronLabel else null,
								labelColor = coverArtColorState.secondaryTextColor,
								labelModifier = Modifier.alpha(acceleratedToolbarStateProgress),
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
										modifier = Modifier.size(iconSize),
									)
								},
								label = if (acceleratedHeaderHidingProgress < 1) addFileToPlaybackLabel else null,
								labelColor = coverArtColorState.secondaryTextColor,
								labelModifier = Modifier.alpha(acceleratedToolbarStateProgress),
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
										modifier = Modifier.size(iconSize),
									)
								},
								label = if (acceleratedHeaderHidingProgress < 1) playLabel else null,
								labelColor = coverArtColorState.secondaryTextColor,
								labelModifier = Modifier.alpha(acceleratedToolbarStateProgress),
								labelMaxLines = 1,
							)
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

		BackButton(
			onBack = navigateApplication::backOut,
			modifier = Modifier
				.padding(Dimensions.topRowOuterPadding)
				.align(Alignment.TopStart)
		)
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

				val fileProperties by viewModel.fileProperties.subscribeAsState()
				val lazyListState = rememberLazyListState()
				LazyColumn(modifier = Modifier.fillMaxWidth(), state = lazyListState) {
					stickyHeader {
						FilePropertyHeader(
							viewModel,
							coverArtColorState,
							modifier = Modifier
								.background(coverArtColorState.backgroundColor)
								.padding(
									start = viewPadding,
									top = viewPadding,
									bottom = viewPadding,
									end = Dimensions.viewPaddingUnit * 10 + viewPadding
								)
								.fillMaxWidth(),
							isMarqueeEnabled = !lazyListState.isScrollInProgress
						)
					}

					items(fileProperties) {
						FilePropertyRow(viewModel, it, coverArtColorState)
					}
				}
			}
		} else {
			CircularProgressIndicator(
				color = coverArtColorState.primaryTextColor,
				modifier = Modifier.align(Alignment.Center)
			)
		}

		Image(
			painter = painterResource(id = R.drawable.ic_remove_item_white_36dp),
			contentDescription = "Close",
			colorFilter = ColorFilter.tint(coverArtColorState.secondaryTextColor),
			modifier = Modifier
				.align(Alignment.TopEnd)
				.padding(top = 12.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
				.navigable(onClick = navigateApplication::backOut),
		)
	}

	Column(modifier = Modifier.fillMaxSize()) {
		Spacer(
			modifier = Modifier
				.windowInsetsTopHeight(WindowInsets.statusBars)
				.fillMaxWidth()
				.background(coverArtColorState.actionBarColor)
		)

		ControlSurface(
			color = coverArtColorState.backgroundColor,
			contentColor = coverArtColorState.primaryTextColor,
			controlColor = coverArtColorState.secondaryTextColor,
			modifier = Modifier.weight(1f)
		) {
			BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
				when {
					maxWidth >= 450.dp -> fileDetailsTwoColumn()
					else -> fileDetailsSingleColumn()
				}
			}
		}

		Spacer(
			modifier = Modifier
				.windowInsetsBottomHeight(WindowInsets.navigationBars)
				.fillMaxWidth()
				.background(coverArtColorState.actionBarColor)
		)
	}
}
