package com.lasthopesoftware.bluewater.client.browsing.files.details

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FormattedScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.SelectedConnectionFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.colors.MediaStylePalette
import com.lasthopesoftware.bluewater.shared.android.colors.MediaStylePaletteProvider
import com.lasthopesoftware.bluewater.shared.android.ui.components.GradientSide
import com.lasthopesoftware.bluewater.shared.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.shared.android.ui.components.RatingBar
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.suspend
import kotlinx.coroutines.flow.map
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import kotlin.math.pow

class FileDetailsActivity : ComponentActivity() {

	companion object {

		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(FileDetailsActivity::class.java) }
		private val playlist by lazy { magicPropertyBuilder.buildProperty("playlist") }
		private val playlistPosition by lazy { magicPropertyBuilder.buildProperty("playlistPosition") }

		fun Context.launchFileDetailsActivity(playlist: Collection<ServiceFile>, position: Int) {
			startActivity(Intent(this, FileDetailsActivity::class.java).apply {
				putExtra(playlistPosition, position)
				putExtra(Companion.playlist, playlist.map { it.key }.toIntArray())
			})
		}
	}

	private val imageProvider by lazy { CachedImageProvider.getInstance(this) }

	private val defaultImageProvider by lazy { DefaultImageProvider(this) }

	private val filePropertiesProvider by lazy {
		SelectedConnectionFilePropertiesProvider(SelectedConnectionProvider(this)) { c ->
			val filePropertyCache = FilePropertyCache.getInstance()
			FormattedScopedFilePropertiesProvider(
				ScopedFilePropertiesProvider(
					c,
					ScopedRevisionProvider(c),
					filePropertyCache
				),
			)
		}
	}

	private val vm by buildViewModelLazily {
		FileDetailsViewModel(
			filePropertiesProvider,
			defaultImageProvider,
			imageProvider,
			PlaybackServiceController(this),
		)
	}

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			ProjectBlueTheme {
				FileDetailsView(vm)
			}
		}

		restoreSelectedConnection(this).eventually(LoopedInPromise.response({
			val position = intent.getIntExtra(playlistPosition, -1)
			val playlist = intent.getIntArrayExtra(playlist)?.map(::ServiceFile) ?: emptyList()
			setView(playlist, position)
		}, this))
	}

	private fun setView(playlist: List<ServiceFile>, position: Int) {
		if (position < 0) {
			finish()
			return
		}

		vm.loadFromList(playlist, position)
			.excuse(HandleViewIoException(this) { setView(playlist, position) })
			.eventuallyExcuse(LoopedInPromise.response(UnexpectedExceptionToasterResponse(this), this))
			.then { finish() }
	}
}

@Preview
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FileDetailsView(@PreviewParameter(FileDetailsPreviewProvider::class) viewModel: FileDetailsViewModel) {
	val activity = LocalContext.current as? Activity ?: return

	val defaultMediaStylePalette = MediaStylePalette(
		MaterialTheme.colors.onPrimary,
		MaterialTheme.colors.secondary,
		MaterialTheme.colors.primary,
		MaterialTheme.colors.secondary
	)

	val paletteProvider = MediaStylePaletteProvider(activity)
	val coverArtColors = remember {
		viewModel.coverArt
			.map { a -> a
					?.takeIf { it.width > 0 && it.height > 0 }
					?.let(paletteProvider::promisePalette)
					?.suspend()
					?: defaultMediaStylePalette
			}
	}
	val coverArtColorState by coverArtColors.collectAsState(defaultMediaStylePalette)
	val systemUiController = rememberSystemUiController()
	systemUiController.setStatusBarColor(coverArtColorState.actionBarColor)

	val viewPadding = 4.dp

	val artist by viewModel.artist.collectAsState()
	val album by viewModel.album.collectAsState()

	@Composable
	fun filePropertyHeader(modifier: Modifier, titleFontSize: TextUnit = 24.sp) {
		val fileName by viewModel.fileName.collectAsState(stringResource(id = R.string.lbl_loading))

		Column(modifier = modifier) {
			val gradientSides = setOf(GradientSide.End)

			Row {
				MarqueeText(
					text = fileName,
					color = coverArtColorState.primaryTextColor,
					gradientEdgeColor = coverArtColorState.backgroundColor,
					fontSize = titleFontSize,
					overflow = TextOverflow.Ellipsis,
					gradientSides = gradientSides,
				)
			}

			Row {
				MarqueeText(
					text = artist,
					color = coverArtColorState.primaryTextColor,
					gradientEdgeColor = coverArtColorState.backgroundColor,
					fontSize = 16.sp,
					overflow = TextOverflow.Ellipsis,
					gradientSides = gradientSides,
				)
			}
		}
	}

	@Composable
	fun fileMenu() {
		Row(modifier = Modifier
			.height(dimensionResource(id = R.dimen.standard_row_height))
			.padding(viewPadding + 8.dp)
		) {
			Image(
				painter = painterResource(id = R.drawable.ic_add_item_white_36dp),
				colorFilter = ColorFilter.tint(coverArtColorState.secondaryTextColor),
				contentDescription = stringResource(id = R.string.btn_add_file),
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.clickable { viewModel.addToNowPlaying() }
					.align(Alignment.CenterVertically),
			)

			Image(
				painter = painterResource(id = R.drawable.av_play_white),
				colorFilter = ColorFilter.tint(coverArtColorState.secondaryTextColor),
				contentDescription = stringResource(id = R.string.btn_play),
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.clickable {
						viewModel.play()
					}
					.align(Alignment.CenterVertically),
			)
		}
	}

	@Composable
	fun fileRating(modifier: Modifier) {
		val rating by viewModel.rating.collectAsState()

		RatingBar(
			rating = rating,
			color = coverArtColorState.primaryTextColor,
			backgroundColor = coverArtColorState.primaryTextColor.copy(.1f),
			modifier = modifier
		)
	}

	@Composable
	fun filePropertyRow(property: Map.Entry<String, String>) {
		val itemPadding = 2.dp

		Row {
			Text(
				text = property.key,
				color = coverArtColorState.primaryTextColor,
				modifier = Modifier
					.weight(1f)
					.padding(start = viewPadding, top = itemPadding, end = itemPadding, bottom = itemPadding),
			)

			when (property.key) {
				KnownFileProperties.RATING -> {
					Box(modifier = Modifier
						.weight(2f)
						.align(Alignment.CenterVertically)
					) {
						val height = with (LocalDensity.current) {
							MaterialTheme.typography.h6.fontSize.toDp()
						}

						fileRating(
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
						text = property.value,
						color = coverArtColorState.primaryTextColor,
						modifier = Modifier
							.weight(2f)
							.padding(start = itemPadding, top = itemPadding, end = viewPadding, bottom = itemPadding),
					)
				}
			}
		}
	}

	@Composable
	fun fileDetailsSingleColumn() {
		val coverArtBitmaps = remember { viewModel.coverArt.map { a -> a?.asImageBitmap() } }
		val coverArtState by coverArtBitmaps.collectAsState(null)

		val fileProperties by viewModel.fileProperties.collectAsState()

		val toolbarState = rememberCollapsingToolbarScaffoldState()
		val headerHidingProgress by derivedStateOf { 1 - toolbarState.toolbarState.progress }

		CollapsingToolbarScaffold(
			enabled = true,
			state = toolbarState,
			scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
			modifier = Modifier.fillMaxSize(),
			toolbar = {
				val appBarHeight = 56.dp
				val coverArtTopPadding = viewPadding + appBarHeight
				val coverArtBottomPadding = viewPadding + 8.dp
				val coverArtContainerHeight = 300.dp

				val coverArtScrollOffset by derivedStateOf { -coverArtContainerHeight * headerHidingProgress }
				Box(
					modifier = Modifier
						.height(coverArtContainerHeight)
						.padding(
							top = coverArtTopPadding,
							start = viewPadding + 40.dp,
							end = viewPadding + 40.dp,
						)
						.offset(y = coverArtScrollOffset)
						.fillMaxWidth()
				) {
					coverArtState
						?.let {
							Image(
								bitmap = it,
								contentDescription = stringResource(id = R.string.lbl_cover_art, album, artist),
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

				Box(modifier = Modifier
					.height(appBarHeight)
					.background(coverArtColorState.backgroundColor)
					.fillMaxWidth()
				) {
					Icon(
						Icons.Default.ArrowBack,
						contentDescription = "",
						tint = coverArtColorState.secondaryTextColor,
						modifier = Modifier
							.padding(16.dp)
							.align(Alignment.CenterStart)
							.clickable(
								interactionSource = remember { MutableInteractionSource() },
								indication = null,
								onClick = activity::finish
							)
					)
				}

				val expandedTitlePadding = coverArtContainerHeight + coverArtBottomPadding
				val expandedIconSize = 36.dp
				val expandedMenuVerticalPadding = 12.dp
				val titleFontSize = MaterialTheme.typography.h5.fontSize
				val subTitleFontSize = MaterialTheme.typography.h6.fontSize
				val guessedRowSpacing = 4.dp
				val titleHeight = LocalDensity.current.run { titleFontSize.toDp() + subTitleFontSize.toDp() } + guessedRowSpacing * 3
				val boxHeight = expandedTitlePadding + titleHeight + expandedIconSize + expandedMenuVerticalPadding * 2

				val topTitlePadding by derivedStateOf { expandedTitlePadding * toolbarState.toolbarState.progress }
				BoxWithConstraints(
					modifier = Modifier
						.height(boxHeight)
						.padding(top = topTitlePadding)
						.fillMaxWidth()
				) {
					val minimumMenuWidth = (2 * 32).dp

					val acceleratedProgress by derivedStateOf { 1 - toolbarState.toolbarState.progress.pow(3).coerceIn(0f, 1f) }

					val startPadding by derivedStateOf { viewPadding + 48.dp * headerHidingProgress }
					val endPadding by derivedStateOf { viewPadding + minimumMenuWidth * acceleratedProgress }
					filePropertyHeader(
						modifier = Modifier.padding(start = startPadding, end = endPadding),
						titleFontSize = titleFontSize,
					)

					val menuWidth by derivedStateOf { (maxWidth - (maxWidth - minimumMenuWidth) * acceleratedProgress) }
					val expandedTopRowPadding = titleHeight + expandedMenuVerticalPadding
					val topRowPadding by derivedStateOf { expandedTopRowPadding - (expandedTopRowPadding - 14.dp) * headerHidingProgress }
					Row(modifier = Modifier
						.padding(top = topRowPadding, start = 8.dp, end = 8.dp)
						.width(menuWidth)
						.align(Alignment.TopEnd)
					) {
						val iconSize by derivedStateOf { expandedIconSize - (12 * headerHidingProgress).dp }

						Image(
							painter = painterResource(id = R.drawable.ic_add_item_white_36dp),
							colorFilter = ColorFilter.tint(coverArtColorState.secondaryTextColor),
							contentDescription = stringResource(id = R.string.btn_add_file),
							modifier = Modifier
								.fillMaxWidth()
								.weight(1f)
								.size(iconSize)
								.clickable { viewModel.addToNowPlaying() }
								.align(Alignment.CenterVertically),
						)

						Image(
							painter = painterResource(id = R.drawable.av_play_white),
							colorFilter = ColorFilter.tint(coverArtColorState.secondaryTextColor),
							contentDescription = stringResource(id = R.string.btn_play),
							modifier = Modifier
								.fillMaxWidth()
								.weight(1f)
								.size(iconSize)
								.clickable {
									viewModel.play()
								}
								.align(Alignment.CenterVertically),
						)
					}
				}
			}
		) {
			LazyColumn(modifier = Modifier.fillMaxSize()) {
				items(fileProperties) {
					filePropertyRow(it)
				}
			}
		}
	}

	@Composable
	fun BoxWithConstraintsScope.fileDetailsTwoColumn() {
		val coverArtBitmaps = remember { viewModel.coverArt.map { a -> a?.asImageBitmap() } }
		val coverArtState by coverArtBitmaps.collectAsState(null)

		val fileProperties by viewModel.fileProperties.collectAsState()

		Row(modifier = Modifier.fillMaxSize()) {
			Column(modifier = Modifier
				.fillMaxHeight()
				.width(250.dp)
				.padding(viewPadding)
				.padding(
					start = viewPadding,
					end = 10.dp,
					bottom = viewPadding,
					top = viewPadding,
				)
			) {
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.weight(1.0f)
						.padding(bottom = 10.dp)
						.align(Alignment.CenterHorizontally)
				) {
					coverArtState
						?.let {
							Image(
								bitmap = it,
								contentDescription = stringResource(id = R.string.lbl_cover_art, album, artist),
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

				fileMenu()
			}

			LazyColumn(modifier = Modifier.fillMaxWidth()) {
				stickyHeader {
					filePropertyHeader(
						modifier = Modifier
							.background(coverArtColorState.backgroundColor)
							.padding(
								start = viewPadding,
								top = viewPadding,
								bottom = viewPadding,
								end = 40.dp + viewPadding
							)
							.fillParentMaxWidth()
					)
				}

				items(fileProperties) {
					filePropertyRow(property = it)
				}
			}
		}

		Image(
			painter = painterResource(id = R.drawable.ic_remove_item_white_36dp),
			contentDescription = "Close",
			colorFilter = ColorFilter.tint(coverArtColorState.secondaryTextColor),
			modifier = Modifier
				.align(Alignment.TopEnd)
				.padding(top = 12.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
				.clickable {
					activity.finish()
				},
		)
	}

	val isLoading by viewModel.isLoading.collectAsState()

	BoxWithConstraints(modifier = Modifier
		.fillMaxSize()
		.background(coverArtColorState.backgroundColor)
	) {

		when {
			isLoading -> CircularProgressIndicator(
				color = coverArtColorState.primaryTextColor,
				modifier = Modifier.align(Alignment.Center))
			maxWidth >= 450.dp -> fileDetailsTwoColumn()
			else -> fileDetailsSingleColumn()
		}
	}
}
