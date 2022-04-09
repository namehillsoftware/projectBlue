package com.lasthopesoftware.bluewater.client.browsing.items.media.files.details

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.databinding.ActivityViewFileDetailsBinding
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.view.ScaledWrapImageView
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class FileDetailsActivity : AppCompatActivity() {

	private val imageProvider by lazy { CachedImageProvider.getInstance(this) }

	private val defaultImageProvider by lazy { DefaultImageProvider(this) }

	private val vm by buildViewModelLazily {
		FileDetailsViewModel(
			SelectedConnectionProvider(this),
			defaultImageProvider,
			imageProvider
		)
	}

	private val binding by lazy {
		val binding =
			DataBindingUtil.setContentView<ActivityViewFileDetailsBinding>(this, R.layout.activity_view_file_details)
		binding.lifecycleOwner = this

		binding.vm = vm

		binding
	}

	private val fileThumbnailContainer by lazy { binding.rlFileThumbnailContainer }

	private val imgFileThumbnailBuilder by lazy {
		val rlFileThumbnailContainer = fileThumbnailContainer

		val imgFileThumbnail = ScaledWrapImageView(this)
		imgFileThumbnail.setBackgroundResource(R.drawable.drop_shadow)
		imgFileThumbnail.layoutParams = imgFileThumbnailLayoutParams

		rlFileThumbnailContainer.addView(imgFileThumbnail)
		imgFileThumbnail
	}

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setSupportActionBar(findViewById(R.id.fileDetailsToolbar))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

//		setContent {
//			ProjectBlueTheme {
//				FileDetailsView()
//			}
//		}

		val fileKey = intent.getIntExtra(fileKey, -1)

		vm.coverArt.filterNotNull().onEach {
			imgFileThumbnailBuilder.setImageBitmap(it)
			binding.pbLoadingFileThumbnail.visibility = View.INVISIBLE
			imgFileThumbnailBuilder.visibility = View.VISIBLE
		}.launchIn(lifecycleScope)

		vm.fileProperties.onEach {
			binding.lvFileDetails.adapter = FileDetailsAdapter(this, R.id.linFileDetailsRow, it)
		}.launchIn(lifecycleScope)

		setView(ServiceFile(fileKey))

		NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton(findViewById(R.id.viewFileDetailsRelativeLayout))
	}

	private fun setView(serviceFile: ServiceFile) {
		if (serviceFile.key < 0) {
			finish()
			return
		}

		vm.loadFile(serviceFile)
			.then { binding.tvFileName.postDelayed({ binding.tvFileName.isSelected = true }, trackNameMarqueeDelay) }
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
fun FileDetailsView() {

}
