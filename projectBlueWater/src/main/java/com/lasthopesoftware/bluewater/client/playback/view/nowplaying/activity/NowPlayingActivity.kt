package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity

import android.content.*
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.*
import android.widget.ImageView.ScaleType
import android.widget.RatingBar.OnRatingBarChangeListener
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ImageProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache.MemoryCachedImageAccess
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.StaticLibraryIdentifierProvider
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
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
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

class NowPlayingActivity : AppCompatActivity(), IItemListMenuChangeHandler {

	companion object {
		private val logger = LoggerFactory.getLogger(NowPlayingActivity::class.java)
		@JvmStatic
		fun startNowPlayingActivity(context: Context) {
			val viewIntent = Intent(context, NowPlayingActivity::class.java)
			viewIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
			context.startActivity(viewIntent)
		}

		private var isScreenKeptOn = false
		private var viewStructure: ViewStructure? = null
		private fun setRepeatingIcon(imageButton: ImageButton?, isRepeating: Boolean) {
			imageButton?.setImageDrawable(
				ViewUtils.getDrawable(imageButton.context, if (isRepeating) R.drawable.av_repeat_dark else R.drawable.av_no_repeat_dark))
		}
	}

	private var connectionRestoreCode: Int? = null
	private var viewAnimator: ViewAnimator? = null
	private val messageHandler by lazy { Handler(mainLooper) }
	private val playButton = LazyViewFinder<ImageButton>(this, R.id.btnPlay)
	private val pauseButton = LazyViewFinder<ImageButton>(this, R.id.btnPause)
	private val songRating = LazyViewFinder<RatingBar>(this, R.id.rbSongRating)
	private val contentView = LazyViewFinder<RelativeLayout>(this, R.id.rlCtlNowPlaying)
	private val songProgressBar = LazyViewFinder<ProgressBar>(this, R.id.pbNowPlaying)
	private val nowPlayingImageViewFinder = LazyViewFinder<ImageView>(this, R.id.imgNowPlaying)
	private val nowPlayingArtist = LazyViewFinder<TextView>(this, R.id.tvSongArtist)
	private val isScreenKeptOnButton = LazyViewFinder<ImageButton>(this, R.id.isScreenKeptOnButton)
	private val nowPlayingTitle = LazyViewFinder<TextView>(this, R.id.tvSongTitle)
	private val nowPlayingImageLoading = LazyViewFinder<ImageView>(this, R.id.imgNowPlayingLoading)
	private val loadingProgressBar = LazyViewFinder<ProgressBar>(this, R.id.pbLoadingImg)
	private val viewNowPlayingListButton = LazyViewFinder<ImageButton>(this, R.id.viewNowPlayingListButton)
	private val drawerLayout = LazyViewFinder<DrawerLayout>(this, R.id.nowPlayingDrawer)
	private val readOnlyConnectionLabel = LazyViewFinder<TextView>(this, R.id.readOnlyConnectionLabel)

	private val localBroadcastManager by lazy { LocalBroadcastManager.getInstance(this) }

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

	private val nowPlayingListAdapter by lazy {
		nowPlayingRepository.eventually(LoopedInPromise.response({ r ->
			val nowPlayingFileListMenuBuilder = NowPlayingFileListItemMenuBuilder(
				r,
				FileListItemNowPlayingRegistrar(localBroadcastManager))

			nowPlayingFileListMenuBuilder.setOnViewChangedListener(
				ViewChangedHandler()
					.setOnViewChangedListener(this)
					.setOnAnyMenuShown(this)
					.setOnAllMenusHidden(this))

			NowPlayingFileListAdapter(this, nowPlayingFileListMenuBuilder)
		}, messageHandler))
	}

	private val nowPlayingDrawerListView by lazy {
		val listView = findViewById<RecyclerView>(R.id.nowPlayingDrawerListView)
		nowPlayingListAdapter.eventually(LoopedInPromise.response({ a ->
			listView.adapter = a
			listView.layoutManager = LinearLayoutManager(this)
			listView
		}, messageHandler))
	}

	private val lazyImageProvider by lazy {
			ImageProvider(
				StaticLibraryIdentifierProvider(SelectedBrowserLibraryIdentifierProvider(getApplicationSettingsRepository())),
				MemoryCachedImageAccess.getInstance(this))
		}

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

	private val lazyDefaultImage by lazy { DefaultImageProvider(this).promiseFileBitmap() }

	private val drawerToggle by lazy {
		nowPlayingDrawerListView.eventually(LoopedInPromise.response({ lv ->
			object : ActionBarDrawerToggle(
				this@NowPlayingActivity,  /* host Activity */
				drawerLayout.findView(),  /* DrawerLayout object */
				R.string.drawer_open,  /* "open drawer" description */
				R.string.drawer_close /* "close drawer" description */
			) {
				/** Called when a drawer has settled in a completely closed state.  */
				override fun onDrawerClosed(view: View) {
					super.onDrawerClosed(view)
					isDrawerOpened = false
				}

				/** Called when a drawer has settled in a completely open state.  */
				override fun onDrawerOpened(drawerView: View) {
					super.onDrawerOpened(drawerView)
					isDrawerOpened = true
					lv.bringToFront()
					drawerLayout.findView().requestLayout()
				}
			}
		}, messageHandler))
	}

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
										if (!isDrawerOpened) updateNowPlayingListViewPosition()
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

		val playbackStoppedIntentFilter = IntentFilter()
		playbackStoppedIntentFilter.addAction(PlaylistEvents.onPlaylistPause)
		playbackStoppedIntentFilter.addAction(PlaylistEvents.onPlaylistStop)
		localBroadcastManager.registerReceiver(onPlaybackStoppedReceiver, playbackStoppedIntentFilter)
		localBroadcastManager.registerReceiver(onPlaybackStartedReceiver, IntentFilter(PlaylistEvents.onPlaylistStart))
		localBroadcastManager.registerReceiver(onPlaybackChangedReceiver, IntentFilter(PlaylistEvents.onPlaylistTrackChange))
		localBroadcastManager.registerReceiver(onPlaylistChangedReceiver, IntentFilter(PlaylistEvents.onPlaylistChange))
		localBroadcastManager.registerReceiver(onTrackPositionChanged, IntentFilter(TrackPositionBroadcaster.trackPositionUpdate))

		addOnConnectionLostListener(onConnectionLostListener)

		setNowPlayingBackgroundBitmap()

		contentView.findView().setOnClickListener { showNowPlayingControls() }

		playButton.findView().setOnClickListener { v ->
			if (!nowPlayingToggledVisibilityControls.isVisible) return@setOnClickListener
			PlaybackService.play(v.context)
			playButton.findView().visibility = View.INVISIBLE
			pauseButton.findView().visibility = View.VISIBLE
		}

		pauseButton.findView().setOnClickListener { v ->
			if (!nowPlayingToggledVisibilityControls.isVisible) return@setOnClickListener
			PlaybackService.pause(v.context)
			playButton.findView().visibility = View.VISIBLE
			pauseButton.findView().visibility = View.INVISIBLE
		}

		findViewById<ImageButton>(R.id.btnNext)?.setOnClickListener { v ->
			if (nowPlayingToggledVisibilityControls.isVisible) PlaybackService.next(v.context)
		}

		findViewById<ImageButton>(R.id.btnPrevious)?.setOnClickListener { v ->
			if (nowPlayingToggledVisibilityControls.isVisible) PlaybackService.previous(v.context)
		}

		val shuffleButton = findViewById<ImageButton>(R.id.repeatButton)
		setRepeatingIcon(shuffleButton)
		shuffleButton?.setOnClickListener { v ->
			nowPlayingRepository
				.then { r ->
					r.nowPlaying.eventually(LoopedInPromise.response({ result ->
						val isRepeating = !result.isRepeating
						if (isRepeating) PlaybackService.setRepeating(v.context)
						else PlaybackService.setCompleting(v.context)
						setRepeatingIcon(shuffleButton, isRepeating)
					}, messageHandler))
				}
		}

		isScreenKeptOnButton.findView().setOnClickListener {
			isScreenKeptOn = !isScreenKeptOn
			updateKeepScreenOnStatus()
		}

		setupNowPlayingListDrawer()

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
	}

	private fun setupNowPlayingListDrawer() {
		viewNowPlayingListButton.findView()
			.setOnClickListener { drawerLayout.findView().openDrawer(GravityCompat.END) }
		drawerLayout.findView().setScrimColor(ContextCompat.getColor(this, android.R.color.transparent))
		drawerLayout.findView().setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
		drawerToggle.then(drawerLayout.findView()::addDrawerListener)

		val rotation = windowManager.defaultDisplay.rotation
		if (rotation != Surface.ROTATION_90) return

		val nowPlayingDrawerContainer = findViewById<LinearLayout>(R.id.nowPlayingDrawerContainer)
		val newLayoutParams = DrawerLayout.LayoutParams(nowPlayingDrawerContainer.layoutParams)
		newLayoutParams.gravity = GravityCompat.START
		nowPlayingDrawerContainer.layoutParams = newLayoutParams
		viewNowPlayingListButton.findView()
			.setOnClickListener { drawerLayout.findView().openDrawer(GravityCompat.START) }
		drawerLayout.findView().setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.END)
	}

	public override fun onStart() {
		super.onStart()
		updateKeepScreenOnStatus()

		restoreSelectedConnection(this).eventually(LoopedInPromise.response({
			connectionRestoreCode = it
			if (it == null) initializeView()
		}, messageHandler))
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == connectionRestoreCode) initializeView()
		super.onActivityResult(requestCode, resultCode, data)
	}

	private fun updateNowPlayingListViewPosition() {
		nowPlayingRepository.then { npr ->
			npr
				.nowPlaying
				.then { nowPlaying ->
					val newPosition = nowPlaying.playlistPosition
					if (newPosition > -1 && newPosition < nowPlaying.playlist.size)
						nowPlayingDrawerListView.eventually(LoopedInPromise.response({ lv -> lv.scrollToPosition(newPosition) }, messageHandler))
				}
		}
	}

	private fun setNowPlayingBackgroundBitmap() =
		lazyDefaultImage
			.eventually(LoopedInPromise.response({ bitmap ->
				val nowPlayingImageLoadingView = nowPlayingImageLoading.findView()
				nowPlayingImageLoadingView.setImageBitmap(bitmap)
				nowPlayingImageLoadingView.scaleType = ScaleType.CENTER_CROP
			}, messageHandler))

	private fun initializeView() {
		playButton.findView().visibility = View.VISIBLE
		pauseButton.findView().visibility = View.INVISIBLE
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
		isScreenKeptOnButton.findView().setImageDrawable(ViewUtils.getDrawable(this, if (isScreenKeptOn) R.drawable.ic_screen_on_white_36dp else R.drawable.ic_screen_off_white_36dp))
		if (isScreenKeptOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else disableKeepScreenOn()
	}

	private fun disableKeepScreenOn() {
		window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
	}

	private fun togglePlayingButtons(isPlaying: Boolean) {
		playButton.findView().visibility = ViewUtils.getVisibility(!isPlaying)
		pauseButton.findView().visibility = ViewUtils.getVisibility(isPlaying)
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
				viewStructure.promisedNowPlayingImage = lazyImageProvider.promiseFileBitmap(serviceFile)
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
			val artist = fileProperties[KnownFileProperties.ARTIST]
			nowPlayingArtist.findView().text = artist
			val title = fileProperties[KnownFileProperties.NAME]
			nowPlayingTitle.findView().text = title
			nowPlayingTitle.findView().isSelected = true
			val duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties)
			setTrackDuration(if (duration > 0) duration.toLong() else 100.toLong())
			setTrackProgress(initialFilePosition)

			val stringRating = fileProperties[KnownFileProperties.RATING]
			val fileRating = stringRating?.toFloatOrNull()

			val songRatingBar = songRating.findView()
			songRatingBar.rating = fileRating ?: 0f
			songRatingBar.isEnabled = !isReadOnly
			readOnlyConnectionLabel.findView().visibility = if (isReadOnly) View.VISIBLE else View.GONE

			if (isReadOnly) return

			songRatingBar.onRatingBarChangeListener = OnRatingBarChangeListener { _, newRating, fromUser ->
				if (fromUser && nowPlayingToggledVisibilityControls.isVisible) {
					val ratingToString = newRating.roundToInt().toString()
					filePropertiesStorage
						.promiseFileUpdate(serviceFile, KnownFileProperties.RATING, ratingToString, false)
						.eventuallyExcuse(LoopedInPromise.response(::handleIoException, messageHandler))
					viewStructure?.fileProperties?.put(KnownFileProperties.RATING, ratingToString)
				}
			}
		}

		fun disableViewWithMessage() {
			nowPlayingTitle.findView().setText(R.string.lbl_loading)
			nowPlayingArtist.findView().text = ""
			songRating.findView().rating = 0f
			songRating.findView().isEnabled = false
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
		viewStructure?.fileDuration = duration
	}

	private fun setTrackProgress(progress: Long) {
		songProgressBar.findView().progress = progress.toInt()
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

	override fun onStop() {
		super.onStop()
		disableKeepScreenOn()
	}

	public override fun onDestroy() {
		super.onDestroy()
		timerTask?.cancel()
		localBroadcastManager.unregisterReceiver(onPlaybackStoppedReceiver)
		localBroadcastManager.unregisterReceiver(onPlaybackStartedReceiver)
		localBroadcastManager.unregisterReceiver(onPlaybackChangedReceiver)
		localBroadcastManager.unregisterReceiver(onPlaylistChangedReceiver)
		localBroadcastManager.unregisterReceiver(onTrackPositionChanged)
		removeOnConnectionLostListener(onConnectionLostListener)
	}

	override fun onAllMenusHidden() {}
	override fun onAnyMenuShown() {}

	override fun onViewChanged(viewAnimator: ViewAnimator) {
		this.viewAnimator = viewAnimator
	}

	override fun onBackPressed() {
		if (LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)) return
		if (isDrawerOpened) {
			drawerLayout.findView().closeDrawers()
			return
		}
		super.onBackPressed()
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
