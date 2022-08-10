package com.lasthopesoftware.bluewater.client.browsing.files.details

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
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

class FileDetailsActivity : ComponentActivity() {

	companion object {

		val fileKey by lazy { MagicPropertyBuilder.buildMagicPropertyName<FileDetailsActivity>("FILE_KEY") }

		fun Context.launchFileDetailsActivity(serviceFile: ServiceFile) {
			startActivity(Intent(this, FileDetailsActivity::class.java).apply {
				putExtra(fileKey, serviceFile.key)
			})
		}
	}

	private val imageProvider by lazy { CachedImageProvider.getInstance(this) }

	private val defaultImageProvider by lazy { DefaultImageProvider(this) }

	private val vm by buildViewModelLazily {
		FileDetailsViewModel(
			SelectedConnectionProvider(this),
			defaultImageProvider,
			imageProvider
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
			val fileKey = intent.getIntExtra(fileKey, -1)
			setView(ServiceFile(fileKey))
		}, this))
	}

	private fun setView(serviceFile: ServiceFile) {
		if (serviceFile.key < 0) {
			finish()
			return
		}

		vm.loadFile(serviceFile)
			.excuse(HandleViewIoException(this) { setView(serviceFile) })
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

	@Composable
	fun filePropertyHeader(modifier: Modifier) {
		val artist by viewModel.artist.collectAsState()
		val fileName by viewModel.fileName.collectAsState(stringResource(id = R.string.lbl_loading))

		Column(modifier = modifier) {
			val gradientSides = setOf(GradientSide.End)

			Row {
				MarqueeText(
					text = fileName,
					color = coverArtColorState.primaryTextColor,
					gradientEdgeColor = coverArtColorState.backgroundColor,
					fontSize = 24.sp,
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
					fileRating(
						modifier = Modifier
							.weight(2f)
							.height(20.dp)
							.align(Alignment.CenterVertically)
							.padding(start = itemPadding, top = itemPadding, end = viewPadding, bottom = itemPadding)
					)
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

		LazyColumn(modifier = Modifier.fillMaxSize()) {
			item {
				Column(modifier = Modifier
					.fillParentMaxWidth()
					.padding(viewPadding)) {
					Box(
						modifier = Modifier
							.height(300.dp)
							.padding(
								top = 40.dp,
								start = 40.dp,
								end = 40.dp,
								bottom = 10.dp
							)
							.align(Alignment.CenterHorizontally)
					) {
						coverArtState
							?.let {
								Image(
									bitmap = it,
									contentDescription = null,
									contentScale = ContentScale.FillHeight,
									modifier = Modifier
										.fillParentMaxHeight()
										.clip(RoundedCornerShape(5.dp))
										.border(
											1.dp,
											shape = RoundedCornerShape(5.dp),
											color = coverArtColorState.secondaryTextColor
										),
								)
							}
					}

					Row(modifier = Modifier
						.height(dimensionResource(id = R.dimen.standard_row_height))
						.padding(8.dp)
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
							painter = painterResource(id = R.drawable.ic_menu_white_36dp),
							colorFilter = ColorFilter.tint(coverArtColorState.secondaryTextColor),
							contentDescription = stringResource(id = R.string.btn_view_files),
							modifier = Modifier
								.fillMaxWidth()
								.clickable { viewModel.viewFileDetails() }
								.weight(1f)
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
			}

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
				filePropertyRow(it)
			}
		}
	}

	@Composable
	fun fileDetailsTwoColumn() {
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
								contentDescription = null,
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

				fileRating(
					modifier = Modifier
						.fillMaxWidth()
						.height(46.dp)
						.padding(bottom = 10.dp)
						.align(Alignment.CenterHorizontally)
				)
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
}
