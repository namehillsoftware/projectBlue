package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ViewAnimator
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.TaskStackBuilder
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.BrowserActivity
import com.lasthopesoftware.bluewater.client.browsing.files.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener.Companion.tryFlipToPreviousView
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService
import com.lasthopesoftware.bluewater.client.connection.polling.WaitForConnectionDialog
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.NowPlayingTopFragment
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist.NowPlayingPlaylistFragment
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist.NowPlayingPlaylistMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.databinding.ActivityViewNowPlayingBinding
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModel
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToaster
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.closables.lazyScoped
import com.lasthopesoftware.resources.strings.StringResources
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class NowPlayingActivity :
	AppCompatActivity(),
	(PollConnectionService.ConnectionLostNotification) -> Unit,
	IItemListMenuChangeHandler,
	Runnable
{
	private var viewAnimator: ViewAnimator? = null

	private val messageHandler by lazy { Handler(mainLooper) }

	private val applicationMessageBus by lazy { getApplicationMessageBus() }

	private val activityScopedMessageBus by lazyScoped { applicationMessageBus.getScopedMessageBus() }

	private val imageProvider by lazy { CachedImageProvider.getInstance(this) }

	private val libraryConnectionProvider by lazy { buildNewConnectionSessionManager() }

	private val connectionAuthenticationChecker by lazy {
		ConnectionAuthenticationChecker(libraryConnectionProvider)
	}

	private val revisionProvider by lazy { LibraryRevisionProvider(libraryConnectionProvider) }

	private val lazySelectedConnectionProvider by lazy { SelectedConnectionProvider(this) }

	private val libraryFilePropertiesProvider by lazy {
		FilePropertiesProvider(
			libraryConnectionProvider,
			revisionProvider,
			FilePropertyCache,
		)
	}

	private val filePropertiesStorage by lazy {
		FilePropertyStorage(
			libraryConnectionProvider,
			connectionAuthenticationChecker,
			revisionProvider,
			FilePropertyCache,
			applicationMessageBus
		)
	}

	private val defaultImageProvider by lazy { DefaultImageProvider(this) }

	private val nowPlayingLookup by lazy { LiveNowPlayingLookup.getInstance() }

	private val viewModelMessageBus by buildViewModelLazily { ViewModelMessageBus<NowPlayingPlaylistMessage>() }

	private val nowPlayingViewModel by buildViewModelLazily {
		NowPlayingScreenViewModel(
			applicationMessageBus,
			InMemoryNowPlayingDisplaySettings,
			PlaybackServiceController(this),
		)
	}

	private val binding by lazy {
		val binding = DataBindingUtil.setContentView<ActivityViewNowPlayingBinding>(this, R.layout.activity_view_now_playing)
		binding.lifecycleOwner = this

		binding.filePropertiesVm = buildViewModel {
			NowPlayingFilePropertiesViewModel(
				applicationMessageBus,
				nowPlayingLookup,
                libraryFilePropertiesProvider,
                UrlKeyProvider(libraryConnectionProvider),
                filePropertiesStorage,
                connectionAuthenticationChecker,
                PlaybackServiceController(this),
                ConnectionPoller(this),
                StringResources(this),
            )
		}

		binding.coverArtVm = buildViewModel {
			NowPlayingCoverArtViewModel(
				applicationMessageBus,
				nowPlayingLookup,
				lazySelectedConnectionProvider,
				defaultImageProvider,
				imageProvider,
				ConnectionPoller(this),
			)
		}

		binding.vm = nowPlayingViewModel

		binding
	}

	private val playlistViewModel by buildViewModelLazily {
		NowPlayingPlaylistViewModel(
			applicationMessageBus,
			LiveNowPlayingLookup.getInstance(),
			viewModelMessageBus
		)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val taskStackBuilder = TaskStackBuilder.create(this)
		taskStackBuilder.addParentStack(cls<BrowserActivity>())

		binding.pager.adapter = PagerAdapter()

		binding.filePropertiesVm?.unexpectedError?.filterNotNull()?.onEach {
			UnexpectedExceptionToaster.announce(this, it)
		}?.launchIn(lifecycleScope)

		binding.coverArtVm?.unexpectedError?.filterNotNull()?.onEach {
			UnexpectedExceptionToaster.announce(this, it)
		}?.launchIn(lifecycleScope)

		with (binding.pager) {
			setPageTransformer(FadeToTopPageTransformer)

			nowPlayingViewModel.isDrawerShownState.onEach { isShown ->
				setCurrentItem(if (isShown) 1 else 0, true)
			}.launchIn(lifecycleScope)

			registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
				override fun onPageSelected(position: Int) {
					when (position) {
						1 -> nowPlayingViewModel.showDrawer()
						else -> {
							nowPlayingViewModel.hideDrawer()
							playlistViewModel.finishPlaylistEdit()
							viewAnimator?.tryFlipToPreviousView()
						}
					}
				}
			})
		}

		activityScopedMessageBus.registerReceiver(this)

		onBackPressedDispatcher.addCallback {
			when {
				viewAnimator?.tryFlipToPreviousView() == true -> {}
				playlistViewModel.isEditingPlaylist -> playlistViewModel.finishPlaylistEdit()
				binding.pager.currentItem == 1 -> binding.pager.setCurrentItem(0, true)
				else -> finish()
			}
		}
	}

	override fun onStart() {
		super.onStart()

		if (isTaskRoot) {
			finish()
			return
		}

		run()
	}

	override fun run() {
		restoreSelectedConnection(this)
			.eventually { nowPlayingLookup.promiseNowPlaying() }
			.eventually { np ->
				np?.libraryId
					?.let { libraryId ->
							binding.run {
								Promise.whenAll(
									filePropertiesVm?.initializeViewModel().keepPromise(Unit),
									coverArtVm?.initializeViewModel().keepPromise(Unit)
								)
							}
							.excuse(HandleViewIoException(this, libraryId, this))
					}
					.keepPromise()
			}
			.eventuallyExcuse(LoopedInPromise.response(UnexpectedExceptionToasterResponse(this), messageHandler))
			.then { finish() }
	}

	override fun invoke(p1: PollConnectionService.ConnectionLostNotification) {
		nowPlayingLookup.promiseNowPlaying().eventually(
			LoopedInPromise.response({ np ->
				np?.libraryId?.also { libraryId ->
					WaitForConnectionDialog.show(this, libraryId)
				}
			}, messageHandler))
	}

	override fun onAllMenusHidden() {}
	override fun onAnyMenuShown() {}

	override fun onViewChanged(viewAnimator: ViewAnimator) {
		this.viewAnimator = viewAnimator
	}

	private inner class PagerAdapter : FragmentStateAdapter(this@NowPlayingActivity) {
		override fun getItemCount(): Int = 2

		override fun createFragment(position: Int): Fragment = when (position) {
			0 -> NowPlayingTopFragment()
			1 -> NowPlayingPlaylistFragment().apply { setOnItemListMenuChangeHandler(this@NowPlayingActivity) }
			else -> throw IndexOutOfBoundsException()
		}
	}

	private object FadeToTopPageTransformer : ViewPager2.PageTransformer {
		override fun transformPage(page: View, position: Float) {
			with (page) {
				// Adjust alpha based off of position, only taking into account when it's scrolling to top (negative position)
				alpha = (1 + 3 * position).coerceIn(0f, 1f)
			}
		}
	}
}
