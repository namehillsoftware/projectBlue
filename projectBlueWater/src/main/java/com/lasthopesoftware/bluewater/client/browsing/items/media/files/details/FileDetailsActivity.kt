package com.lasthopesoftware.bluewater.client.browsing.items.media.files.details

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FormattedScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ScaledWrapImageView
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise

class FileDetailsActivity : AppCompatActivity() {

	private val lvFileDetails = LazyViewFinder<ListView>(this, R.id.lvFileDetails)
	private val pbLoadingFileDetails = LazyViewFinder<ProgressBar>(this, R.id.pbLoadingFileDetails)
	private val pbLoadingFileThumbnail = LazyViewFinder<ProgressBar>(this, R.id.pbLoadingFileThumbnail)
	private val fileNameTextViewFinder = LazyViewFinder<TextView>(this, R.id.tvFileName)
	private val artistTextViewFinder = LazyViewFinder<TextView>(this, R.id.tvArtist)
	private val fileThumbnailContainer = LazyViewFinder<RelativeLayout>(this, R.id.rlFileThumbnailContainer)

	private val imgFileThumbnailBuilder by lazy {
		val rlFileThumbnailContainer = fileThumbnailContainer.findView()

		val imgFileThumbnail = ScaledWrapImageView(this)
		imgFileThumbnail.setBackgroundResource(R.drawable.drop_shadow)
		imgFileThumbnail.layoutParams = imgFileThumbnailLayoutParams

		rlFileThumbnailContainer.addView(imgFileThumbnail)
		imgFileThumbnail
	}

	private val imageProvider by lazy { CachedImageProvider.getInstance(this) }

	private val defaultImageProvider by lazy { DefaultImageProvider(this) }
	private var fileKey = -1

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_view_file_details)
		setSupportActionBar(findViewById(R.id.fileDetailsToolbar))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		fileKey = intent.getIntExtra(FILE_KEY, -1)
		setView(fileKey)
		NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton(findViewById(R.id.viewFileDetailsRelativeLayout))
	}

	private fun setView(fileKey: Int) {
		if (fileKey < 0) {
			finish()
			return
		}

		lvFileDetails.findView().visibility = View.INVISIBLE
		pbLoadingFileDetails.findView().visibility = View.VISIBLE
		imgFileThumbnailBuilder.visibility = View.INVISIBLE
		pbLoadingFileThumbnail.findView().visibility = View.VISIBLE
		fileNameTextViewFinder.findView().text = getText(R.string.lbl_loading)
		artistTextViewFinder.findView().text = getText(R.string.lbl_loading)

		val selectedConnectionProvider = SelectedConnectionProvider(this)
		selectedConnectionProvider.promiseSessionConnection()
			.eventually { connectionProvider ->
				connectionProvider
					?.let { c -> ScopedFilePropertiesProvider(c,  ScopedRevisionProvider(c), FilePropertyCache.getInstance()) }
					?.let(::FormattedScopedFilePropertiesProvider)
					?.promiseFileProperties(ServiceFile(fileKey))
					?.eventually(LoopedInPromise.response({ fileProperties ->
						setFileNameFromProperties(fileProperties)

						val artist = fileProperties[KnownFileProperties.ARTIST]
						artistTextViewFinder.findView().text = artist

						val filePropertyList = fileProperties.entries
							.filter { e -> !propertiesToSkip.contains(e.key) }
							.sortedBy { e -> e.key }

						lvFileDetails.findView().adapter = FileDetailsAdapter(this, R.id.linFileDetailsRow, filePropertyList)
						pbLoadingFileDetails.findView().visibility = View.INVISIBLE
						lvFileDetails.findView().visibility = View.VISIBLE
					}, this))
					?: Unit.toPromise()
			}
			.excuse(HandleViewIoException(this) { setView(fileKey) })
			.eventuallyExcuse(LoopedInPromise.response(UnexpectedExceptionToasterResponse(this), this))
			.then { finish() }

		imageProvider
			.promiseFileBitmap(ServiceFile(fileKey))
			.eventually { bitmap ->
				bitmap?.toPromise() ?: defaultImageProvider.promiseFileBitmap()
			}
			.eventually(LoopedInPromise.response({ result ->
				imgFileThumbnailBuilder.setImageBitmap(result)
				pbLoadingFileThumbnail.findView().visibility = View.INVISIBLE
				imgFileThumbnailBuilder.visibility = View.VISIBLE
			}, this))
	}

	private fun setFileNameFromProperties(fileProperties: Map<String, String>) {
		val fileName = fileProperties[KnownFileProperties.NAME] ?: return
		val fileNameTextView = fileNameTextViewFinder.findView()
		fileNameTextView.text = fileName
		fileNameTextView.postDelayed({ fileNameTextView.isSelected = true }, trackNameMarqueeDelay.toLong())
		val spannableString = SpannableString(getString(R.string.lbl_details).format(fileName))
		spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, fileName.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
		title = spannableString
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)

		// Update the intent
		setIntent(intent)
		fileKey = intent.getIntExtra(FILE_KEY, -1)
		setView(fileKey)
	}

	public override fun onStart() {
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
		@JvmField
		val FILE_KEY: String = MagicPropertyBuilder.buildMagicPropertyName(FileDetailsActivity::class.java, "FILE_KEY")

		private const val trackNameMarqueeDelay = 1500

		private val propertiesToSkip = setOf(
			KnownFileProperties.AUDIO_ANALYSIS_INFO,
			KnownFileProperties.GET_COVER_ART_INFO,
			KnownFileProperties.IMAGE_FILE,
			KnownFileProperties.KEY,
			KnownFileProperties.STACK_FILES,
			KnownFileProperties.STACK_TOP,
			KnownFileProperties.STACK_VIEW,
			KnownFileProperties.WAVEFORM,
			KnownFileProperties.LengthInPcmBlocks)

		private val imgFileThumbnailLayoutParams by lazy {
			val layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
			layoutParams
		}
	}
}
