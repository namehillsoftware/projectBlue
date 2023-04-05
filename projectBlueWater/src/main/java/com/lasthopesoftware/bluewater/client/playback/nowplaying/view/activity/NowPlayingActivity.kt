package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ViewAnimator
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.lasthopesoftware.bluewater.R
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
import com.lasthopesoftware.resources.closables.lazyScoped
import com.lasthopesoftware.resources.strings.StringResources
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class NowPlayingActivity :
	AppCompatActivity(),
	(PollConnectionService.ConnectionLostNotification) -> Unit,
	IItemListMenuChangeHandler,
	Runnable
{
	companion object {
		fun Context.startNowPlayingActivity() {
			val viewIntent = Intent(this, cls<NowPlayingActivity>())
			viewIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
			startActivity(viewIntent)
		}
	}

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

		val liveNowPlayingLookup = LiveNowPlayingLookup.getInstance()
		binding.filePropertiesVm = buildViewModel {
			NowPlayingFilePropertiesViewModel(
				applicationMessageBus,
                liveNowPlayingLookup,
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
				liveNowPlayingLookup,
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

		run()
	}

	override fun run() {
		restoreSelectedConnection(this)
			.eventually(LoopedInPromise.response({
				binding.also { b ->
					b.filePropertiesVm?.initializeViewModel()
					b.coverArtVm?.initializeViewModel()
				}
			}, messageHandler))
			.excuse(HandleViewIoException(this, this))
			.eventuallyExcuse(LoopedInPromise.response(UnexpectedExceptionToasterResponse(this), this))
			.then { finish() }
	}

	override fun invoke(p1: PollConnectionService.ConnectionLostNotification) {
		WaitForConnectionDialog.show(this)
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
