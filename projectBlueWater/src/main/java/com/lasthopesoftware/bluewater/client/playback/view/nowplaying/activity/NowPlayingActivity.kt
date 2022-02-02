package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.widget.*
import android.widget.ImageView.ScaleType
import android.widget.RatingBar.OnRatingBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemNowPlayingRegistrar
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.SelectedConnectionFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.SelectedConnectionFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.SelectedConnectionRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.authentication.ScopedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.authentication.SelectedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService.Companion.addOnConnectionLostListener
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService.Companion.pollSessionConnection
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService.Companion.removeOnConnectionLostListener
import com.lasthopesoftware.bluewater.client.connection.polling.WaitForConnectionDialog
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.list.NowPlayingFileListAdapter
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.menu.NowPlayingFileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils.getThemedDrawable
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToaster
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CancellationException
import kotlin.math.roundToInt

class NowPlayingActivity :
	AppCompatActivity(),
	IItemListMenuChangeHandler
{
	companion object {
		private val logger by lazy { LoggerFactory.getLogger(NowPlayingActivity::class.java) }

		fun startNowPlayingActivity(context: Context) {
			val viewIntent = Intent(context, NowPlayingActivity::class.java)
			viewIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
			context.startActivity(viewIntent)
		}

		private var isScreenKeptOn = false
		private var viewStructure: ViewStructure? = null
		private fun setRepeatingIcon(imageButton: ImageButton?, isRepeating: Boolean) {
			imageButton?.setImageDrawable(
				imageButton.context.getThemedDrawable(if (isRepeating) R.drawable.av_repeat_dark else R.drawable.av_no_repeat_dark))
		}
	}

	private var connectionRestoreCode: Int? = null
	private var viewAnimator: ViewAnimator? = null
	private val messageHandler by lazy { Handler(mainLooper) }
	private val playButton = LazyViewFinder<ImageButton>(this, R.id.btnPlay)
	private val miniPlayButton = LazyViewFinder<ImageButton>(this, R.id.miniPlay)
	private val pauseButton = LazyViewFinder<ImageButton>(this, R.id.btnPause)
	private val miniPauseButton = LazyViewFinder<ImageButton>(this, R.id.miniPause)
	private val songRating = LazyViewFinder<RatingBar>(this, R.id.rbSongRating)
	private val miniSongRating = LazyViewFinder<RatingBar>(this, R.id.miniSongRating)
	private val contentView = LazyViewFinder<View>(this, R.id.nowPlayingContentView)
	private val bottomSheet = LazyViewFinder<RelativeLayout>(this, R.id.nowPlayingBottomSheet)
	private val songProgressBar = LazyViewFinder<ProgressBar>(this, R.id.pbNowPlaying)
	private val miniSongProgressBar = LazyViewFinder<ProgressBar>(this, R.id.miniNowPlayingBar)
	private val nowPlayingImageViewFinder = LazyViewFinder<ImageView>(this, R.id.imgNowPlaying)
	private val nowPlayingArtist = LazyViewFinder<TextView>(this, R.id.tvSongArtist)
	private val isScreenKeptOnButton = LazyViewFinder<ImageButton>(this, R.id.isScreenKeptOnButton)
	private val nowPlayingTitle = LazyViewFinder<TextView>(this, R.id.tvSongTitle)
	private val nowPlayingImageLoading = LazyViewFinder<ImageView>(this, R.id.imgNowPlayingLoading)
	private val loadingProgressBar = LazyViewFinder<ProgressBar>(this, R.id.pbLoadingImg)
	private val readOnlyConnectionLabel = LazyViewFinder<TextView>(this, R.id.readOnlyConnectionLabel)
	private val miniReadOnlyConnectionLabel = LazyViewFinder<TextView>(this, R.id.miniReadOnlyConnectionLabel)
	private val nowPlayingHeaderContainer = LazyViewFinder<RelativeLayout>(this, R.id.nowPlayingHeaderContainer)
	private val closeNowPlayingListButton = LazyViewFinder<ImageButton>(this, R.id.closeNowPlayingList)
	private val viewNowPlayingListButton = LazyViewFinder<ImageButton>(this, R.id.viewNowPlayingListButton)
	private val nowPlayingControlsContainer = LazyViewFinder<RelativeLayout>(this, R.id.nowPlayingControlsContainer)

	private val messageBus = lazy { MessageBus(LocalBroadcastManager.getInstance(this)) }

	private val nowPlayingToggledVisibilityControls by lazy {
		NowPlayingToggledVisibilityControls(
			LazyViewFinder(this, R.id.llNpButtons),
			LazyViewFinder(this, R.id.menuControlsLinearLayout),
			LazyViewFinder(this, R.id.songRatingLinearLayout)
		)
	}

	private val selectedLibraryIdProvider by lazy { SelectedBrowserLibraryIdentifierProvider(getApplicationSettingsRepository()) }

	private val nowPlayingRepository by lazy {
		val libraryRepository = LibraryRepository(this)
		selectedLibraryIdProvider.selectedLibraryId
			.then { l ->
				NowPlayingRepository(
					SpecificLibraryProvider(l!!, libraryRepository),
					libraryRepository)
			}
	}

	private val fileListItemNowPlayingRegistrar = lazy { FileListItemNowPlayingRegistrar(messageBus.value) }

	private val nowPlayingListAdapter by lazy {
		nowPlayingRepository.eventually(LoopedInPromise.response({ r ->
			val nowPlayingFileListMenuBuilder = NowPlayingFileListItemMenuBuilder(
				r,
				fileListItemNowPlayingRegistrar.value)

			nowPlayingFileListMenuBuilder.setOnViewChangedListener(
				ViewChangedHandler()
					.setOnViewChangedListener(this)
					.setOnAnyMenuShown(this)
					.setOnAllMenusHidden(this))

			NowPlayingFileListAdapter(this, nowPlayingFileListMenuBuilder)
		}, messageHandler))
	}

	private val nowPlayingListView by lazy {
		val listView = findViewById<RecyclerView>(R.id.nowPlayingListView)
		nowPlayingListAdapter.eventually(LoopedInPromise.response({ a ->
			listView.adapter = a
			listView.layoutManager = LinearLayoutManager(this)
			listView
		}, messageHandler))
	}

	private val imageProvider by lazy { CachedImageProvider.getInstance(this) }

	private val lazySelectedConnectionProvider by lazy { SelectedConnectionProvider(this) }

	private val lazySessionRevisionProvider by lazy {
		SelectedConnectionRevisionProvider(lazySelectedConnectionProvider)
	}

	private val lazyFilePropertiesProvider by lazy {
		SelectedConnectionFilePropertiesProvider(lazySelectedConnectionProvider) { c ->
			ScopedFilePropertiesProvider(
				c,
				lazySessionRevisionProvider,
				FilePropertyCache.getInstance()
			)
		}
	}

	private val lazySelectedConnectionAuthenticationChecker by lazy {
		SelectedConnectionAuthenticationChecker(
			lazySelectedConnectionProvider,
			::ScopedConnectionAuthenticationChecker)
	}

	private val filePropertiesStorage by lazy {
		SelectedConnectionFilePropertiesStorage(lazySelectedConnectionProvider) { c ->
			ScopedFilePropertiesStorage(
				c,
				lazySelectedConnectionAuthenticationChecker,
				lazySessionRevisionProvider,
				FilePropertyCache.getInstance())
		}
	}

	private val defaultImage by lazy { DefaultImageProvider(this).promiseFileBitmap() }

	private val bottomSheetBehavior by lazy { BottomSheetBehavior.from(bottomSheet.findView()) }

	private val onConnectionLostListener = Runnable { WaitForConnectionDialog.show(this) }

	private val onPlaybackChangedReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			if (!isDrawerOpened) updateNowPlayingListViewPosition()
			showNowPlayingControls()
			updateKeepScreenOnStatus()
			setView()
		}
	}

	private val onPlaybackStartedReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			togglePlayingButtons(true)
			updateKeepScreenOnStatus()
		}
	}

	private val onPlaybackStoppedReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			togglePlayingButtons(false)
			disableKeepScreenOn()
		}
	}

	private val onPlaylistChangedReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			nowPlayingRepository.then { r ->
				r.nowPlaying
					.then { np ->
						nowPlayingListAdapter
							.then { adapter ->
								adapter
									.updateListEventually(np.playlist.mapIndexed { i, s -> PositionedFile(i, s) })
									.eventually(LoopedInPromise.response({
										updateNowPlayingListViewPosition()
										setView()
									}, messageHandler))
							}
					}
			}
		}
	}

	private val onTrackPositionChanged = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			val fileDuration = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.fileDuration, -1)
			if (fileDuration > -1) setTrackDuration(fileDuration)
			val filePosition = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1)
			if (filePosition > -1) setTrackProgress(filePosition)
		}
	}

	private var timerTask: TimerTask? = null
	private var isDrawerOpened = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_view_now_playing)

		nowPlayingToggledVisibilityControls.toggleVisibility(false)

		val playbackStoppedIntentFilter = IntentFilter().apply {
			addAction(PlaylistEvents.onPlaylistPause)
			addAction(PlaylistEvents.onPlaylistInterrupted)
			addAction(PlaylistEvents.onPlaylistStop)
		}

		with(messageBus.value) {
			registerReceiver(onPlaybackStoppedReceiver, playbackStoppedIntentFilter)
			registerReceiver(onPlaybackStartedReceiver, IntentFilter(PlaylistEvents.onPlaylistStart))
			registerReceiver(onPlaybackChangedReceiver, IntentFilter(PlaylistEvents.onPlaylistTrackChange))
			registerReceiver(onPlaylistChangedReceiver, IntentFilter(PlaylistEvents.onPlaylistChange))
			registerReceiver(onTrackPositionChanged, IntentFilter(TrackPositionBroadcaster.trackPositionUpdate))
		}

		addOnConnectionLostListener(onConnectionLostListener)

		setNowPlayingBackgroundBitmap()

		contentView.findView().setOnClickListener { showNowPlayingControls() }

		val playButtonClick = View.OnClickListener { v ->
			if (!nowPlayingToggledVisibilityControls.isVisible) return@OnClickListener
			PlaybackService.play(v.context)
			togglePlayingButtons(true)
		}

		playButton.findView().setOnClickListener { v ->
			if (!nowPlayingToggledVisibilityControls.isVisible) return@setOnClickListener
			PlaybackService.play(v.context)
			togglePlayingButtons(true)
		}

		miniPlayButton.findView().setOnClickListener { v ->
			PlaybackService.play(v.context)
			togglePlayingButtons(true)
		}

		pauseButton.findView().setOnClickListener { v ->
			if (!nowPlayingToggledVisibilityControls.isVisible) return@setOnClickListener
			PlaybackService.pause(v.context)
			togglePlayingButtons(false)
		}

		miniPauseButton.findView().setOnClickListener { v ->
			PlaybackService.pause(v.context)
			togglePlayingButtons(false)
		}

		findViewById<ImageButton>(R.id.btnNext)?.setOnClickListener { v ->
			if (nowPlayingToggledVisibilityControls.isVisible) PlaybackService.next(v.context)
		}

		findViewById<ImageButton>(R.id.btnPrevious)?.setOnClickListener { v ->
			if (nowPlayingToggledVisibilityControls.isVisible) PlaybackService.previous(v.context)
		}

		val repeatButton = findViewById<ImageButton>(R.id.repeatButton)
		setRepeatingIcon(repeatButton)
		repeatButton?.setOnClickListener { v ->
			nowPlayingRepository
				.then { r ->
					r.nowPlaying.eventually(LoopedInPromise.response({ result ->
						val isRepeating = !result.isRepeating
						if (isRepeating) PlaybackService.setRepeating(v.context)
						else PlaybackService.setCompleting(v.context)
						setRepeatingIcon(repeatButton, isRepeating)
					}, messageHandler))
				}
		}

		isScreenKeptOnButton.findView().setOnClickListener {
			isScreenKeptOn = !isScreenKeptOn
			updateKeepScreenOnStatus()
		}

		nowPlayingRepository.then { npr ->
			npr.nowPlaying
				.then { nowPlaying ->
					nowPlayingListAdapter
						.eventually { npa ->
							npa.updateListEventually(nowPlaying.playlist.mapIndexed { i, sf -> PositionedFile(i, sf) })
						}
						.eventually(LoopedInPromise.response({ updateNowPlayingListViewPosition() }, messageHandler))
				}
		}

		val bottomSheet = bottomSheet.findView()
		bottomSheet.setOnClickListener { showNowPlayingControls() }

		bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
			override fun onStateChanged(bottomSheet: View, newState: Int) {
				isDrawerOpened = newState == BottomSheetBehavior.STATE_EXPANDED
				with (nowPlayingHeaderContainer.findView()) {
					alpha = when (newState) {
						BottomSheetBehavior.STATE_COLLAPSED -> 1f
						BottomSheetBehavior.STATE_EXPANDED -> 0f
						else -> alpha
					}
				}

				with (closeNowPlayingListButton.findView()) {
					alpha = when (newState) {
						BottomSheetBehavior.STATE_COLLAPSED -> 0f
						BottomSheetBehavior.STATE_EXPANDED -> 1f
						else -> alpha
					}
				}

				nowPlayingControlsContainer.findView().visibility =
					ViewUtils.getVisibility(newState == BottomSheetBehavior.STATE_COLLAPSED)
			}

			override fun onSlide(bottomSheet: View, slideOffset: Float) {
				nowPlayingHeaderContainer.findView().alpha = 1 - slideOffset
				closeNowPlayingListButton.findView().alpha = slideOffset
			}
		})

		val toggleListClickHandler = View.OnClickListener {
			with(bottomSheetBehavior) {
				state = when (state) {
					BottomSheetBehavior.STATE_COLLAPSED -> BottomSheetBehavior.STATE_EXPANDED
					BottomSheetBehavior.STATE_EXPANDED -> BottomSheetBehavior.STATE_COLLAPSED
					else -> state
				}
			}
		}

		closeNowPlayingListButton.findView().setOnClickListener(toggleListClickHandler)
		viewNowPlayingListButton.findView().setOnClickListener(toggleListClickHandler)
	}

	override fun onStart() {
		super.onStart()
		updateKeepScreenOnStatus()

		restoreSelectedConnection(this).eventually(LoopedInPromise.response({
			connectionRestoreCode = it
			if (it == null) initializeView()
		}, messageHandler))
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)

		if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) return

		nowPlayingHeaderContainer.findView().alpha = 0f
		closeNowPlayingListButton.findView().alpha = 1f
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == connectionRestoreCode) initializeView()
		super.onActivityResult(requestCode, resultCode, data)
	}

	override fun onStop() {
		super.onStop()
		disableKeepScreenOn()
	}

	override fun onDestroy() {
		super.onDestroy()
		timerTask?.cancel()
		removeOnConnectionLostListener(onConnectionLostListener)

		if (fileListItemNowPlayingRegistrar.isInitialized()) fileListItemNowPlayingRegistrar.value.clear()
		if (messageBus.isInitialized()) messageBus.value.clear()
	}

	override fun onAllMenusHidden() {}
	override fun onAnyMenuShown() {}

	override fun onViewChanged(viewAnimator: ViewAnimator) {
		this.viewAnimator = viewAnimator
	}

	override fun onBackPressed() {
		if (LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)) return
		if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
			bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
			return
		}
		super.onBackPressed()
	}

	private fun updateNowPlayingListViewPosition() {
		nowPlayingRepository.then { npr ->
			npr
				.nowPlaying
				.then { nowPlaying ->
					val newPosition = nowPlaying.playlistPosition
					if (newPosition > -1 && newPosition < nowPlaying.playlist.size)
						nowPlayingListView.eventually(LoopedInPromise.response({ lv -> lv.scrollToPosition(newPosition) }, messageHandler))
				}
		}
	}

	private fun setNowPlayingBackgroundBitmap() =
		defaultImage
			.eventually(LoopedInPromise.response({ bitmap ->
				val nowPlayingImageLoadingView = nowPlayingImageLoading.findView()
				nowPlayingImageLoadingView.setImageBitmap(bitmap)
				nowPlayingImageLoadingView.scaleType = ScaleType.CENTER_CROP
			}, messageHandler))

	private fun initializeView() {
		togglePlayingButtons(false)
		nowPlayingRepository
			.eventually { npr ->
				npr
					.nowPlaying
					.eventually { np ->
						lazySelectedConnectionProvider
							.promiseSessionConnection()
							.eventually(LoopedInPromise.response({ connectionProvider ->
								val serviceFile = np.playlist[np.playlistPosition]
								val filePosition = connectionProvider?.urlProvider?.baseUrl
									?.let { baseUrl ->
										viewStructure
											?.takeIf { it.urlKeyHolder == UrlKeyHolder(baseUrl, serviceFile) }
									}
									?.filePosition
									?: np.filePosition
								setView(serviceFile, filePosition)
							}, messageHandler))
					}
			}
			.excuse { error -> logger.warn("An error occurred initializing `NowPlayingActivity`", error) }

		PlaybackService.promiseIsMarkedForPlay(this).then(::togglePlayingButtons)
	}

	private fun setRepeatingIcon(imageButton: ImageButton?) {
		setRepeatingIcon(imageButton, false)
		nowPlayingRepository
			.then { npr ->
				npr
					.nowPlaying
					.eventually(LoopedInPromise.response({ result ->
						if (result != null) setRepeatingIcon(imageButton, result.isRepeating)
					}, messageHandler))
			}
	}

	private fun updateKeepScreenOnStatus() {
		isScreenKeptOnButton.findView().setImageDrawable(getThemedDrawable(
			if (isScreenKeptOn) R.drawable.ic_screen_on_white_36dp
			else R.drawable.ic_screen_off_white_36dp))
		if (isScreenKeptOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else disableKeepScreenOn()
	}

	private fun disableKeepScreenOn() {
		window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
	}

	private fun togglePlayingButtons(isPlaying: Boolean) {
		ViewUtils.getVisibility(!isPlaying)
			.apply(playButton.findView()::setVisibility)
			.apply(miniPlayButton.findView()::setVisibility)

		ViewUtils.getVisibility(isPlaying)
			.apply(pauseButton.findView()::setVisibility)
			.apply(miniPauseButton.findView()::setVisibility)
	}

	private fun setView() {
		nowPlayingRepository
			.then { npr ->
				npr.nowPlaying
					.eventually { np ->
						if (np.playlistPosition >= np.playlist.size) Unit.toPromise()
						else lazySelectedConnectionProvider
							.promiseSessionConnection()
							.eventually(LoopedInPromise.response({ connectionProvider ->
								connectionProvider?.urlProvider?.baseUrl?.let { baseUrl ->
									val serviceFile = np.playlist[np.playlistPosition]
									val filePosition = viewStructure
										?.takeIf { it.urlKeyHolder == UrlKeyHolder(baseUrl, serviceFile) }
										?.filePosition
										?: 0
									setView(serviceFile, filePosition)
								}
							}, messageHandler))
					}
			}
			.excuse { e -> logger.error("An error occurred while getting the Now Playing data", e) }
	}

	private fun setView(serviceFile: ServiceFile, initialFilePosition: Long) {
		fun setNowPlayingImage(viewStructure: ViewStructure) {
			val nowPlayingImage = nowPlayingImageViewFinder.findView()
			loadingProgressBar.findView().visibility = View.VISIBLE
			nowPlayingImage.visibility = View.INVISIBLE
			if (viewStructure.promisedNowPlayingImage == null) {
				viewStructure.promisedNowPlayingImage = imageProvider.promiseFileBitmap(serviceFile)
			}
			viewStructure.promisedNowPlayingImage
				?.eventually { bitmap ->
					if (viewStructure !== Companion.viewStructure) Unit.toPromise()
					else LoopedInPromise(MessageWriter {
						nowPlayingImage.setImageBitmap(bitmap)
						loadingProgressBar.findView().visibility = View.INVISIBLE
						if (bitmap != null) {
							nowPlayingImage.scaleType = ScaleType.CENTER_CROP
							nowPlayingImage.visibility = View.VISIBLE
						}
					}, messageHandler)
				}
				?.excuse { e ->
					if (e is CancellationException)	logger.info("Bitmap retrieval cancelled", e)
					else logger.error("There was an error retrieving the image for serviceFile $serviceFile", e)
				}
		}

		fun setFileProperties(fileProperties: Map<String, String>, isReadOnly: Boolean) {
			fun updateReadOnlyLabel() {
				readOnlyConnectionLabel.findView().visibility = if (isReadOnly) View.VISIBLE else View.GONE
				miniReadOnlyConnectionLabel.findView().visibility = if (isReadOnly) View.VISIBLE else View.GONE
			}

			val artist = fileProperties[KnownFileProperties.ARTIST]
			nowPlayingArtist.findView().text = artist
			val title = fileProperties[KnownFileProperties.NAME]

			with (nowPlayingTitle.findView()) {
				text = title
				isSelected = true
			}

			val duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties)
			setTrackDuration(if (duration > 0) duration.toLong() else 100.toLong())
			setTrackProgress(initialFilePosition)

			val stringRating = fileProperties[KnownFileProperties.RATING]
			val fileRating = stringRating?.toFloatOrNull() ?: 0f

			updateReadOnlyLabel()

			with (miniSongRating.findView()) {
				rating = fileRating
				isEnabled = !isReadOnly

				onRatingBarChangeListener =
					if (isReadOnly) null
					else OnRatingBarChangeListener { _, newRating, fromUser ->
						if (fromUser) {
							songRating.findView().rating = newRating
							val ratingToString = newRating.roundToInt().toString()
							filePropertiesStorage
								.promiseFileUpdate(serviceFile, KnownFileProperties.RATING, ratingToString, false)
								.eventuallyExcuse(LoopedInPromise.response(::handleIoException, messageHandler))
							viewStructure?.fileProperties?.put(KnownFileProperties.RATING, ratingToString)
						}
					}
			}

			with (songRating.findView()) {
				rating = fileRating
				isEnabled = !isReadOnly

				onRatingBarChangeListener =
					if (isReadOnly) null
					else OnRatingBarChangeListener { _, newRating, fromUser ->
						if (fromUser && nowPlayingToggledVisibilityControls.isVisible) {
							miniSongRating.findView().rating = newRating
							val ratingToString = newRating.roundToInt().toString()
							filePropertiesStorage
								.promiseFileUpdate(serviceFile, KnownFileProperties.RATING, ratingToString, false)
								.eventuallyExcuse(LoopedInPromise.response(::handleIoException, messageHandler))
							viewStructure?.fileProperties?.put(KnownFileProperties.RATING, ratingToString)
						}
					}
			}
		}

		fun disableViewWithMessage() {
			nowPlayingTitle.findView().setText(R.string.lbl_loading)
			nowPlayingArtist.findView().text = ""

			with (songRating.findView()) {
				rating = 0f
				isEnabled = false
			}
		}

		fun handleException(exception: Throwable) {
			val isIoException = handleIoException(exception)
			if (!isIoException) return

			pollSessionConnection(this).then {
				if (serviceFile == viewStructure?.serviceFile) {
					viewStructure?.promisedNowPlayingImage?.cancel()
					viewStructure?.promisedNowPlayingImage = null
				}
				setView(serviceFile, initialFilePosition)
			}
			WaitForConnectionDialog.show(this)
		}

		lazySelectedConnectionProvider.promiseSessionConnection()
			.eventually(LoopedInPromise.response(ImmediateResponse { connectionProvider ->
				val baseUrl = connectionProvider?.urlProvider?.baseUrl ?: return@ImmediateResponse

				val urlKeyHolder = UrlKeyHolder(baseUrl, serviceFile)
				if (viewStructure?.urlKeyHolder != urlKeyHolder) {
					viewStructure?.release()
					viewStructure = null
				}

				val localViewStructure = viewStructure ?: ViewStructure(urlKeyHolder, serviceFile)
				viewStructure = localViewStructure

				setNowPlayingImage(localViewStructure)

				val cachedFileProperties = localViewStructure.fileProperties
				val isReadOnly = localViewStructure.isFilePropertiesReadOnly
				if (cachedFileProperties != null && isReadOnly != null) {
					setFileProperties(cachedFileProperties, isReadOnly)
					return@ImmediateResponse
				}

				disableViewWithMessage()
				val promisedIsConnectionReadOnly = lazySelectedConnectionAuthenticationChecker.promiseIsReadOnly()
				lazyFilePropertiesProvider
					.promiseFileProperties(serviceFile)
					.eventually { fileProperties ->
						if (localViewStructure !== viewStructure) Unit.toPromise()
						else promisedIsConnectionReadOnly.eventually { isReadOnly ->
							if (localViewStructure !== viewStructure) Unit.toPromise()
							else LoopedInPromise(MessageWriter {
								localViewStructure.fileProperties = fileProperties.toMutableMap()
								localViewStructure.isFilePropertiesReadOnly = isReadOnly
								setFileProperties(fileProperties, isReadOnly)
							}, messageHandler)
						}
					}
					.eventuallyExcuse(LoopedInPromise.response(::handleException, messageHandler))
			}, messageHandler))
	}

	private fun setTrackDuration(duration: Long) {
		songProgressBar.findView().max = duration.toInt()
		miniSongProgressBar.findView().max = duration.toInt()
		viewStructure?.fileDuration = duration
	}

	private fun setTrackProgress(progress: Long) {
		songProgressBar.findView().progress = progress.toInt()
		miniSongProgressBar.findView().progress = progress.toInt()
		viewStructure?.filePosition = progress
	}

	private fun handleIoException(exception: Throwable) =
		if (ConnectionLostExceptionFilter.isConnectionLostException(exception)) true
		else {
			UnexpectedExceptionToaster.announce(this, exception)
			false
		}

	private fun showNowPlayingControls() {
		nowPlayingToggledVisibilityControls.toggleVisibility(true)
		contentView.findView().invalidate()
		timerTask?.cancel()
		val newTimerTask = object : TimerTask() {
			var cancelled = false
			override fun run() {
				if (!cancelled) nowPlayingToggledVisibilityControls.toggleVisibility(false)
			}

			override fun cancel(): Boolean {
				cancelled = true
				return super.cancel()
			}
		}.apply { timerTask = this }
		messageHandler.postDelayed(newTimerTask, 5000)
	}

	private class ViewStructure(val urlKeyHolder: UrlKeyHolder<ServiceFile>, val serviceFile: ServiceFile) {
		var fileProperties: MutableMap<String, String>? = null
		var promisedNowPlayingImage: Promise<Bitmap?>? = null
		var filePosition: Long = 0
		var fileDuration: Long = 0
		var isFilePropertiesReadOnly: Boolean? = null

		fun release() {
			promisedNowPlayingImage?.cancel()
		}
	}
}
