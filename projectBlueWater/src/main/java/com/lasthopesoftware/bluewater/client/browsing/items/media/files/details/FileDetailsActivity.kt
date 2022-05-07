package com.lasthopesoftware.bluewater.client.browsing.items.media.files.details

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

//		NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton(findViewById(R.id.viewFileDetailsRelativeLayout))
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

		private val imgFileThumbnailLayoutParams by lazy {
			val layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
			layoutParams
		}
	}
}

@Preview
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun FileDetailsView(@PreviewParameter(FileDetailsPreviewProvider::class) viewModel: FileDetailsViewModel) {
	val coverArtBitmaps = remember { viewModel.coverArt.map { a -> a?.asImageBitmap() } }
	val coverArtState by coverArtBitmaps.collectAsState(null)

	val defaultMediaStylePalette = MediaStylePalette(
		Color.White,
		MaterialTheme.colors.primaryVariant,
		MaterialTheme.colors.background,
		MaterialTheme.colors.surface
	)

	val activity = LocalContext.current as? Activity ?: return

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

	val artist by viewModel.artist.collectAsState()
	val fileName by viewModel.fileName.collectAsState("Loading...")
	val fileProperties by viewModel.fileProperties.collectAsState()

	Box(modifier = Modifier
		.fillMaxSize()
		.background(coverArtColorState.backgroundColor)) {
		LazyColumn(
			modifier = Modifier
				.padding(4.dp)
				.fillMaxSize(),
		) {
			item {
				Box(modifier = Modifier.fillParentMaxWidth()) {
					Box(modifier = Modifier
						.height(300.dp)
						.padding(
							top = 40.dp,
							start = 40.dp,
							end = 40.dp,
							bottom = 10.dp
						)
						.align(Alignment.Center)
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
				}
			}

			stickyHeader {
				Column(
					modifier = Modifier
						.background(coverArtColorState.backgroundColor)
						.fillParentMaxWidth()
				) {
					Row {
						Text(
							text = fileName,
							color = coverArtColorState.primaryTextColor,
							fontSize = 24.sp,
						)
					}

					Row {
						Text(
							text = artist,
							color = coverArtColorState.primaryTextColor,
							fontSize = 16.sp,
						)
					}
				}
			}

			items(fileProperties) {
				Row {
					Text(
						text = it.key,
						color = coverArtColorState.primaryTextColor,
						modifier = Modifier.weight(1f)
					)
					Text(
						text = it.value,
						color = coverArtColorState.primaryTextColor,
						modifier = Modifier.weight(2f)
					)
				}
			}
		}

		Image(
			painter = painterResource(id = R.drawable.ic_remove_item_white_36dp),
			contentDescription = "Close",
			colorFilter = ColorFilter.tint(coverArtColorState.secondaryTextColor),
			modifier = Modifier
				.align(Alignment.TopEnd)
				.padding(8.dp)
				.clickable {
					activity.finish()
				},
		)
	}
}
