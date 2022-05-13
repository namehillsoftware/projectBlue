package com.lasthopesoftware.bluewater.client.browsing.items.media.files.details

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.colors.MediaStylePalette
import com.lasthopesoftware.bluewater.shared.android.colors.MediaStylePaletteProvider
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toAsync
import kotlinx.coroutines.flow.map
import kotlin.math.cos
import kotlin.math.sin

class FileDetailsActivity : ComponentActivity() {

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

		val fileKey = intent.getIntExtra(fileKey, -1)

		setView(ServiceFile(fileKey))
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

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)

		// Update the intent
		setIntent(intent)
		val fileKey = intent.getIntExtra(fileKey, -1)
		setView(ServiceFile(fileKey))
	}

	override fun onStart() {
		super.onStart()
		restoreSelectedConnection(this)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			finish()
			return true
		}
		return super.onOptionsItemSelected(item)
	}

	companion object {
		val fileKey by lazy { MagicPropertyBuilder.buildMagicPropertyName<FileDetailsActivity>("FILE_KEY") }

		private const val trackNameMarqueeDelay = 1500L
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
					?.toAsync()
					?.await()
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
		val fileName by viewModel.fileName.collectAsState("Loading...")

		Column(modifier = modifier) {
			Row {
				Text(
					text = fileName,
					color = coverArtColorState.primaryTextColor,
					fontSize = 24.sp,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)
			}

			Row {
				Text(
					text = artist,
					color = coverArtColorState.primaryTextColor,
					fontSize = 16.sp,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)
			}
		}
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

			Text(
				text = property.value,
				color = coverArtColorState.primaryTextColor,
				modifier = Modifier
					.weight(2f)
					.padding(start = itemPadding, top = itemPadding, end = viewPadding, bottom = itemPadding),
			)
		}
	}

	@Composable
	fun fileDetailsPortrait() {
		val coverArtBitmaps = remember { viewModel.coverArt.map { a -> a?.asImageBitmap() } }
		val coverArtState by coverArtBitmaps.collectAsState(null)

		val rating by viewModel.rating.collectAsState()

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
										.clip(RoundedCornerShape(5.dp)),
								)
							}
					}

					RatingBar(
						rating = rating,
						color = coverArtColorState.secondaryTextColor,
						backgroundColor = coverArtColorState.primaryTextColor,
						modifier = Modifier
							.fillMaxWidth()
							.height(36.dp)
					)
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
	fun fileDetailsLandscape() {
		val coverArtBitmaps = remember { viewModel.coverArt.map { a -> a?.asImageBitmap() } }
		val coverArtState by coverArtBitmaps.collectAsState(null)

		val rating by viewModel.rating.collectAsState()

		val fileProperties by viewModel.fileProperties.collectAsState()

		Row(modifier = Modifier.fillMaxSize()) {
			Column(modifier = Modifier
				.fillMaxHeight()
				.width(380.dp)
				.padding(viewPadding)) {
				Box(
					modifier = Modifier
						.width(300.dp)
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
									.align(Alignment.Center),
							)
						}
				}

				RatingBar(
					rating = rating,
					color = coverArtColorState.secondaryTextColor,
					backgroundColor = coverArtColorState.primaryTextColor,
					modifier = Modifier
						.fillMaxWidth()
						.height(36.dp)
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

	Box(modifier = Modifier
		.fillMaxSize()
		.background(coverArtColorState.backgroundColor)) {

		when {
			isLoading -> CircularProgressIndicator(
				color = coverArtColorState.primaryTextColor,
				modifier = Modifier.align(Alignment.Center))
			LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE -> fileDetailsLandscape()
			else -> fileDetailsPortrait()
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

@Composable
fun RatingBar(
	rating: Float,
	modifier: Modifier = Modifier,
	color: Color = Color.Yellow,
	backgroundColor: Color = Color.Gray,
) {
	Row(modifier = modifier.wrapContentSize()) {
		(1..5).forEach { step ->
			val stepRating = when {
				rating > step -> 1f
				step.rem(rating) < 1 -> rating - (step - 1f)
				else -> 0f
			}
			RatingStar(stepRating, color, backgroundColor)
		}
	}
}

@Composable
private fun RatingStar(
	rating: Float,
	ratingColor: Color = Color.Yellow,
	backgroundColor: Color = Color.Gray
) {
	BoxWithConstraints(modifier = Modifier.padding(horizontal = 2.dp)) {
		BoxWithConstraints(
			modifier = Modifier
				.fillMaxHeight()
				.aspectRatio(1f)
				.clip(starShape)
				.border(width = 1.dp, color = backgroundColor, shape = starShape)
		) {
			Canvas(modifier = Modifier.size(maxHeight)) {
//			drawRect(
//				brush = SolidColor(backgroundColor),
//				size = Size(
//					height = size.height * 1.4f,
//					width = size.width * 1.4f
//				),
//				topLeft = Offset(
//					x = -(size.width * 0.1f),
//					y = -(size.height * 0.1f)
//				)
//			)
				if (rating > 0) {
					drawRect(
						brush = SolidColor(ratingColor),
						size = Size(
							height = size.height * 1.1f,
							width = size.width * rating
						)
					)
				}
			}
		}
	}
}

private val starShape = GenericShape { size, _ ->
	addPath(starPath(size.height))
}

private val starPath = { size: Float ->
	Path().apply {
		val outerRadius: Float = size / 1.8f
		val innerRadius: Double = outerRadius / 2.5
		var rot: Double = Math.PI / 2 * 3
		val cx: Float = size / 2
		val cy: Float = size / 20 * 11
		val step = Math.PI / 5

		moveTo(cx, cy - outerRadius)
		repeat(5) {
			var x = (cx + cos(rot) * outerRadius).toFloat()
			var y = (cy + sin(rot) * outerRadius).toFloat()
			lineTo(x, y)
			rot += step

			x = (cx + cos(rot) * innerRadius).toFloat()
			y = (cy + sin(rot) * innerRadius).toFloat()
			lineTo(x, y)
			rot += step
		}
		close()
	}
}

@Preview
@Composable
fun RatingBarPreview() {
	Column(
		Modifier
			.fillMaxSize()
			.background(Color.White)
	) {
		RatingBar(
			3.8f,
			modifier = Modifier.height(20.dp)
		)
	}
}
