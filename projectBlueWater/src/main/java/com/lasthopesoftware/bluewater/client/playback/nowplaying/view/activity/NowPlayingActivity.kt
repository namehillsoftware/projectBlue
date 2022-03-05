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
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemNowPlayingRegistrar
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService
import com.lasthopesoftware.bluewater.client.connection.polling.WaitForConnectionDialog
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.NowPlayingTopFragment
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist.NowPlayingPlaylistFragment
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.databinding.ActivityViewNowPlayingBinding
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToaster
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.activityScope
import org.koin.androidx.viewmodel.ext.android.viewModel

class NowPlayingActivity :
	AppCompatActivity(),
	IItemListMenuChangeHandler,
	AndroidScopeComponent
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

	private val messageHandler by inject<Handler>()
	private val messageBus = lazy { get<MessageBus>() }
	private val fileListItemNowPlayingRegistrar = lazy { FileListItemNowPlayingRegistrar(messageBus.value) }

	private val onConnectionLostListener =
		ReceiveBroadcastEvents { WaitForConnectionDialog.show(this) }

	private val nowPlayingViewModel by viewModel<NowPlayingScreenViewModel>()

	private val filePropertiesViewModel by viewModel<NowPlayingFilePropertiesViewModel>()

	private val coverArtViewModel by viewModel<NowPlayingCoverArtViewModel>()

	private val binding by lazy {
		val binding = DataBindingUtil.setContentView<ActivityViewNowPlayingBinding>(this, R.layout.activity_view_now_playing)
		binding.lifecycleOwner = this

		binding.filePropertiesVm = filePropertiesViewModel
		binding.coverArtVm = coverArtViewModel
		binding.vm = nowPlayingViewModel

		binding
	}

	override val scope by activityScope()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding.pager.adapter = PagerAdapter()

		filePropertiesViewModel.unexpectedError.filterNotNull().onEach {
			UnexpectedExceptionToaster.announce(this, it)
		}.launchIn(lifecycleScope)

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
