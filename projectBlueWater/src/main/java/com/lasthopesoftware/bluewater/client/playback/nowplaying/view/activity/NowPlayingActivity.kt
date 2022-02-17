package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ViewAnimator
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
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
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.SelectedConnectionRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.ScopedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.authentication.SelectedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService
import com.lasthopesoftware.bluewater.client.connection.polling.WaitForConnectionDialog
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.NowPlayingBottomFragment
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.NowPlayingTopFragment
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.databinding.ActivityViewNowPlayingBinding
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModel
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToaster
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.resources.strings.StringResources
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

	private val fileListItemNowPlayingRegistrar = lazy { FileListItemNowPlayingRegistrar(messageBus.value) }

	private val imageProvider by lazy { CachedImageProvider.getInstance(this) }

	private val lazySelectedConnectionProvider by lazy { SelectedConnectionProvider(this) }

	private val lazySessionRevisionProvider by lazy { SelectedConnectionRevisionProvider(lazySelectedConnectionProvider) }

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

	private val onConnectionLostListener =
		ReceiveBroadcastEvents { WaitForConnectionDialog.show(this) }

	private val nowPlayingViewModel by buildViewModelLazily {
		NowPlayingScreenViewModel(
			messageBus.value,
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
				messageBus.value,
				liveNowPlayingLookup,
				lazySelectedConnectionProvider,
				lazyFilePropertiesProvider,
				filePropertiesStorage,
				lazySelectedConnectionAuthenticationChecker,
				PlaybackServiceController(this),
				ConnectionPoller(this),
				StringResources(this),
				nowPlayingViewModel,
				nowPlayingViewModel
			)
		}

		binding.coverArtVm = buildViewModel {
			NowPlayingCoverArtViewModel(
				messageBus.value,
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
							LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)
						}
					}
				}
			})
		}

		messageBus.value.registerReceiver(onConnectionLostListener, IntentFilter(PollConnectionService.connectionLostNotification))
	}

	override fun onStart() {
		super.onStart()

		restoreSelectedConnection(this).eventually(LoopedInPromise.response({
			connectionRestoreCode = it
			if (it == null) binding.also { b ->
				b.filePropertiesVm?.initializeViewModel()
				b.coverArtVm?.initializeViewModel()
			}
		}, messageHandler))
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == connectionRestoreCode) binding.also { b ->
			b.filePropertiesVm?.initializeViewModel()
			b.coverArtVm?.initializeViewModel()
		}
		super.onActivityResult(requestCode, resultCode, data)
	}

	override fun onDestroy() {
		super.onDestroy()

		if (fileListItemNowPlayingRegistrar.isInitialized()) fileListItemNowPlayingRegistrar.value.clear()
		if (messageBus.isInitialized()) messageBus.value.unregisterReceiver(onConnectionLostListener)
	}

	override fun onAllMenusHidden() {}
	override fun onAnyMenuShown() {}

	override fun onViewChanged(viewAnimator: ViewAnimator) {
		this.viewAnimator = viewAnimator
	}

	override fun onBackPressed() {
		if (LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)) return

		if (binding.pager.currentItem == 1) {
			binding.pager.setCurrentItem(0, true)
			return
		}

		super.onBackPressed()
	}

	private inner class PagerAdapter : FragmentStateAdapter(this@NowPlayingActivity) {
		override fun getItemCount(): Int = 2

		override fun createFragment(position: Int): Fragment = when (position) {
			0 -> NowPlayingTopFragment()
			1 -> NowPlayingBottomFragment().apply { setOnItemListMenuChangeHandler(this@NowPlayingActivity) }
			else -> throw IndexOutOfBoundsException()
		}
	}

	private object FadeToTopPageTransformer : ViewPager2.PageTransformer {
		override fun transformPage(page: View, position: Float) {
			with (page) {
				translationY = position

				// Adjust alpha based off of position, only taking into account when it's scrolling to top
				alpha = (1 + 2 * position).coerceIn(0f, 1f)
			}
		}
	}
}
