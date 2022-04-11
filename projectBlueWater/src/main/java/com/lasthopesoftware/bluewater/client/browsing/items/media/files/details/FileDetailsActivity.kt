package com.lasthopesoftware.bluewater.client.browsing.items.media.files.details

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import kotlin.math.min

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

		vm.coverArt.filterNotNull().onEach {
//			imgFileThumbnailBuilder.setImageBitmap(it)
//			binding.pbLoadingFileThumbnail.visibility = View.INVISIBLE
//			imgFileThumbnailBuilder.visibility = View.VISIBLE
		}.launchIn(lifecycleScope)

		vm.fileProperties.onEach {
//			binding.lvFileDetails.adapter = FileDetailsAdapter(this, R.id.linFileDetailsRow, it)
		}.launchIn(lifecycleScope)

		vm.fileName.onEach { title = it }.launchIn(lifecycleScope)

		setView(ServiceFile(fileKey))

//		NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton(findViewById(R.id.viewFileDetailsRelativeLayout))
	}

	private fun setView(serviceFile: ServiceFile) {
		if (serviceFile.key < 0) {
			finish()
			return
		}

		vm.loadFile(serviceFile)
//			.then { binding.tvFileName.postDelayed({ binding.tvFileName.isSelected = true }, trackNameMarqueeDelay) }
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

@Composable
@Preview
fun FileDetailsView(@PreviewParameter(FileDetailsPreviewProvider::class) viewModel: FileDetailsViewModel) {
	val emptyBitmap = remember { ImageBitmap(1, 1) }
	val state = rememberCollapsingToolbarScaffoldState()
	val scrollState = rememberLazyListState()
	val scrollOffset = min(
		1f,
		1 - (scrollState.firstVisibleItemScrollOffset / 600f + scrollState.firstVisibleItemIndex)
	)
	val imageSize by animateDpAsState(targetValue = max(0.dp, 300.dp * scrollOffset))

	val coverArtBitmaps = remember { viewModel.coverArt.map { a -> a?.asImageBitmap() ?: emptyBitmap } }
	val coverArtState by coverArtBitmaps.collectAsState(emptyBitmap)

	val defaultMediaStylePalette = MediaStylePalette(
		Color.White,
		MaterialTheme.colors.primaryVariant,
		MaterialTheme.colors.background,
		MaterialTheme.colors.surface
	)

	val paletteProvider = MediaStylePaletteProvider(LocalContext.current)
	val coverArtColors = remember {
		viewModel.coverArt
			.map { a -> a?.let {
					if (it.width > 0 && it.height > 0) paletteProvider.promisePalette(it).toAsync().await()
					else null
				}
			}
			.map { p -> p ?: defaultMediaStylePalette }
	}
	val coverArtColorState by coverArtColors.collectAsState(defaultMediaStylePalette)

	val artist by viewModel.artist.collectAsState()
	val fileName by viewModel.fileName.collectAsState()
	val fileProperties by viewModel.fileProperties.collectAsState()

	Column {
		Row(modifier = Modifier.background(coverArtColorState.backgroundColor)) {
			Column(modifier = Modifier.padding(4.dp)) {
				Image(
					bitmap = coverArtState,
					contentDescription = null,
					contentScale = ContentScale.FillHeight,
					alignment = Alignment.Center,
					modifier = Modifier
						.graphicsLayer { alpha = state.toolbarState.progress }
						.height(imageSize)
						.padding(10.dp)
						.fillMaxWidth()
						.clip(RoundedCornerShape(4.dp)),
				)

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
		LazyColumn(modifier = Modifier.fillMaxSize(), state = scrollState) {
			items(fileProperties) {
				Row(modifier = Modifier.padding(4.dp)) {
					Text(
						text = it.key,
						modifier = Modifier.weight(1f)
					)
					Text(
						text = it.value,
						modifier = Modifier.weight(2f)
					)
				}
			}
		}
	}

//	CollapsingToolbarScaffold(
//		state = state,
//		toolbar = {
//			Image(
//				bitmap = coverArtState,
//				contentDescription = null,
//				contentScale = ContentScale.FillHeight,
//				alignment = Alignment.Center,
//				modifier = Modifier
//					.graphicsLayer { alpha = state.toolbarState.progress }
//					.parallax()
//					.height(300.dp)
//					.padding(10.dp)
//					.fillMaxWidth()
//					.clip(RoundedCornerShape(4.dp)),
//			)
//
//			Column(modifier = Modifier.padding(4.dp)) {
//				Row {
//					Text(
//						text = fileName,
//						color = coverArtColorState.primaryTextColor,
//						fontSize = 24.sp,
//					)
//				}
//
//				Row {
//					Text(
//						text = artist,
//						color = coverArtColorState.primaryTextColor,
//						fontSize = 16.sp,
//					)
//				}
//			}
//		},
//		scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
//		modifier = Modifier.fillMaxSize(),
//		toolbarModifier = Modifier.background(coverArtColorState.backgroundColor)
//	) {
//		LazyColumn(modifier = Modifier.fillMaxSize()) {
//			items(fileProperties) {
//				Row(modifier = Modifier.padding(4.dp)) {
//					Text(
//						text = it.key,
//						modifier = Modifier.weight(1f)
//					)
//					Text(
//						text = it.value,
//						modifier = Modifier.weight(2f)
//					)
//				}
//			}
//		}
//	}
}
