package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.widget.RatingBar.OnRatingBarChangeListener
import android.widget.RelativeLayout
import android.widget.ViewAnimator
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemNowPlayingRegistrar
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
import com.lasthopesoftware.bluewater.client.connection.authentication.ScopedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.authentication.SelectedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService.Companion.addOnConnectionLostListener
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService.Companion.removeOnConnectionLostListener
import com.lasthopesoftware.bluewater.client.connection.polling.WaitForConnectionDialog
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.list.NowPlayingFileListAdapter
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.menu.NowPlayingFileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.databinding.ActivityViewNowPlayingBinding
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModel
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToaster
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.resources.strings.StringResources
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class NowPlayingActivity :
	AppCompatActivity(),
	IItemListMenuChangeHandler
{
	companion object {
		fun startNowPlayingActivity(context: Context) {
			val viewIntent = Intent(context, NowPlayingActivity::class.java)
			viewIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
			context.startActivity(viewIntent)
		}
	}

	private var connectionRestoreCode: Int? = null
	private var viewAnimator: ViewAnimator? = null
	private val messageHandler by lazy { Handler(mainLooper) }

	private val messageBus = lazy { MessageBus(LocalBroadcastManager.getInstance(this)) }

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

	private val defaultImageProvider by lazy { DefaultImageProvider(this) }

	private lateinit var bottomSheetBehavior: BottomSheetBehavior<RelativeLayout>

	private val onConnectionLostListener = Runnable { WaitForConnectionDialog.show(this) }

	private val binding by lazy {
		val binding = DataBindingUtil.setContentView<ActivityViewNowPlayingBinding>(this, R.layout.activity_view_now_playing)
		binding.lifecycleOwner = this
		bottomSheetBehavior = BottomSheetBehavior.from(binding.control.bottomSheet)

		val listView = binding.control.nowPlayingListView
		val promisedListViewSetup = nowPlayingListAdapter.eventually(LoopedInPromise.response({ a ->
			listView.adapter = a
			listView.layoutManager = LinearLayoutManager(this)
		}, messageHandler))

		val promisedViewModelSetup = nowPlayingRepository
			.eventually(LoopedInPromise.response({
				binding.vm = buildViewModel {
					NowPlayingViewModel(
						messageBus.value,
						it,
						lazySelectedConnectionProvider,
						defaultImageProvider,
						imageProvider,
						lazyFilePropertiesProvider,
						filePropertiesStorage,
						lazySelectedConnectionAuthenticationChecker,
						PlaybackServiceController(this),
						ConnectionPoller(this),
						StringResources(this)
					)
				}
			}, messageHandler))

		Promise
			.whenAll(promisedViewModelSetup, promisedListViewSetup)
			.then { binding }
	}

	private var isDrawerOpened = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding.then { binding ->
			val vm = binding.vm ?: return@then

			vm.isScreenOn.onEach {
				if (it) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
				else disableKeepScreenOn()
			}.launchIn(lifecycleScope)

			with (binding.control) {
				vm.nowPlayingList.onEach { l ->
					nowPlayingListAdapter
						.eventually { npa -> npa.updateListEventually(l) }
				}.launchIn(lifecycleScope)

				vm.nowPlayingFile.filterNotNull().onEach {
					if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
						nowPlayingListView.scrollToPosition(it.playlistPosition)
				}.launchIn(lifecycleScope)

				vm.unexpectedError.filterNotNull().onEach {
					UnexpectedExceptionToaster.announce(this@NowPlayingActivity, it)
				}.launchIn(lifecycleScope)

				btnPlay.setOnClickListener { v ->
					if (!vm.isScreenControlsVisible.value) return@setOnClickListener
					PlaybackService.play(v.context)
					vm.togglePlaying(true)
				}

				miniPlay.setOnClickListener { v ->
					PlaybackService.play(v.context)
					vm.togglePlaying(true)
				}

				btnPause.setOnClickListener { v ->
					if (!vm.isScreenControlsVisible.value) return@setOnClickListener
					PlaybackService.pause(v.context)
					vm.togglePlaying(false)
				}

				miniPause.setOnClickListener { v ->
					PlaybackService.pause(v.context)
					vm.togglePlaying(false)
				}

				btnNext.setOnClickListener { v ->
					if (vm.isScreenControlsVisible.value) PlaybackService.next(v.context)
				}

				btnPrevious.setOnClickListener { v ->
					if (vm.isScreenControlsVisible.value) PlaybackService.previous(v.context)
				}

				repeatButton.setOnClickListener { vm.toggleRepeating() }

				isScreenKeptOnButton.setOnClickListener { vm.toggleScreenOn() }

				nowPlayingContentView.setOnClickListener { vm.showNowPlayingControls() }

				val onRatingBarChangeListener = OnRatingBarChangeListener{ _, rating, fromUser ->
					if (fromUser) vm.updateRating(rating)
				}

				rbSongRating.onRatingBarChangeListener = onRatingBarChangeListener
				miniSongRating.onRatingBarChangeListener = onRatingBarChangeListener

				bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
					override fun onStateChanged(bottomSheet: View, newState: Int) {
						isDrawerOpened = newState == BottomSheetBehavior.STATE_EXPANDED
						with (nowPlayingMainSheet) {
							alpha = when (newState) {
								BottomSheetBehavior.STATE_COLLAPSED -> 1f
								BottomSheetBehavior.STATE_EXPANDED -> 0f
								else -> alpha
							}
						}
					}

					override fun onSlide(bottomSheet: View, slideOffset: Float) {
						nowPlayingMainSheet.alpha = 1 - slideOffset
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

				closeNowPlayingList.setOnClickListener(toggleListClickHandler)
				viewNowPlayingListButton.setOnClickListener(toggleListClickHandler)
			}
		}

		addOnConnectionLostListener(onConnectionLostListener)
	}

	override fun onStart() {
		super.onStart()

		restoreSelectedConnection(this).eventually(LoopedInPromise.response({
			connectionRestoreCode = it
			if (it == null) binding.then { b -> b.vm?.initializeViewModel() }
		}, messageHandler))
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)

		if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
			binding.then { b -> b.control.nowPlayingMainSheet.alpha = 0f }
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == connectionRestoreCode) binding.then { b -> b.vm?.initializeViewModel() }
		super.onActivityResult(requestCode, resultCode, data)
	}

	override fun onStop() {
		super.onStop()
		disableKeepScreenOn()
	}

	override fun onDestroy() {
		super.onDestroy()
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

	private fun disableKeepScreenOn() {
		window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
	}
}
