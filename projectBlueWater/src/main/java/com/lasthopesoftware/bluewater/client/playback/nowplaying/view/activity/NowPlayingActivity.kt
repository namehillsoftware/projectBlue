package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity

import android.os.Bundle
import android.os.Handler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.client.browsing.files.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusablePlaylistFileItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.RateControlledFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionLostNotification
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionServiceProxy
import com.lasthopesoftware.bluewater.client.connection.polling.WaitForConnectionDialog
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingView
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.closables.lazyScoped
import com.lasthopesoftware.resources.strings.StringResources
import com.namehillsoftware.handoff.promises.Promise

class NowPlayingActivity :
	AppCompatActivity(),
	(ConnectionLostNotification) -> Unit
{

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

	private val freshLibraryFileProperties by lazy {
		RateControlledFilePropertiesProvider(
			FilePropertiesProvider(
				libraryConnectionProvider,
				revisionProvider,
				FilePropertyCache,
			),
			PromisingRateLimiter(1),
		)
	}

	private val libraryFilePropertiesProvider by lazy {
		CachedFilePropertiesProvider(
			libraryConnectionProvider,
			FilePropertyCache,
			freshLibraryFileProperties,
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

	private val urlKeyProvider by lazy { UrlKeyProvider(libraryConnectionProvider) }

	private val stringResources by lazy { StringResources(this) }

	private val defaultImageProvider by lazy { DefaultImageProvider(this) }

	private val nowPlayingLookup by lazy { LiveNowPlayingLookup.getInstance() }

	private val menuMessageBus by buildViewModelLazily { ViewModelMessageBus<ItemListMenuMessage>() }

	private val itemListMenuBackPressedHandler by lazyScoped { ItemListMenuBackPressedHandler(menuMessageBus) }

	private val nowPlayingScreenViewModel by buildViewModelLazily {
		NowPlayingScreenViewModel(
			applicationMessageBus,
			InMemoryNowPlayingDisplaySettings,
			PlaybackServiceController(this),
		)
	}

	private val nowPlayingFilePropertiesViewModel by buildViewModelLazily {
		NowPlayingFilePropertiesViewModel(
			applicationMessageBus,
			nowPlayingLookup,
			freshLibraryFileProperties,
			UrlKeyProvider(libraryConnectionProvider),
			filePropertiesStorage,
			connectionAuthenticationChecker,
			PlaybackServiceController(this),
			PollConnectionServiceProxy(this),
			stringResources,
		)
	}

	private val nowPlayingCoverArtViewModel by buildViewModelLazily {
		NowPlayingCoverArtViewModel(
			applicationMessageBus,
			nowPlayingLookup,
			lazySelectedConnectionProvider,
			defaultImageProvider,
			imageProvider,
			PollConnectionServiceProxy(this),
		)
	}

	private val playlistViewModel by buildViewModelLazily {
		NowPlayingPlaylistViewModel(
			applicationMessageBus,
			LiveNowPlayingLookup.getInstance()
		)
	}

	private val childViewItemProvider by buildViewModelLazily {
		ReusablePlaylistFileItemViewModelProvider(
			libraryFilePropertiesProvider,
			urlKeyProvider,
			stringResources,
			menuMessageBus,
			applicationMessageBus,
		)
	}

	private val applicationNavigation by lazy { ActivityApplicationNavigation(this, com.lasthopesoftware.bluewater.shared.android.intents.IntentBuilder(this)) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			NowPlayingView(
				nowPlayingCoverArtViewModel = nowPlayingCoverArtViewModel,
				nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
				screenOnState = nowPlayingScreenViewModel,
				playbackServiceController = PlaybackServiceController(this),
				playlistViewModel = playlistViewModel,
				childItemViewModelProvider = childViewItemProvider,
				applicationNavigation = applicationNavigation,
				itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
			)
		}

		activityScopedMessageBus.registerReceiver(this)
	}

	override fun onStart() {
		super.onStart()

		if (isTaskRoot) {
			finish()
			return
		}

		restoreSelectedConnection(this)
			.eventually { nowPlayingLookup.promiseNowPlaying() }
			.eventually { np ->
				np?.libraryId
					?.let { libraryId ->
						Promise.whenAll(
							nowPlayingFilePropertiesViewModel.initializeViewModel().keepPromise(Unit),
							nowPlayingCoverArtViewModel.initializeViewModel().keepPromise(Unit)
						).eventuallyExcuse { e ->
							if (ConnectionLostExceptionFilter.isConnectionLostException(e))
								PollConnectionService.pollSessionConnection(this, libraryId)
							else
								Promise(e)
						}
					}
					.keepPromise()
			}
			.eventuallyExcuse(LoopedInPromise.response(UnexpectedExceptionToasterResponse(this), messageHandler))
			.then { finish() }
	}

	override fun invoke(p1: ConnectionLostNotification) {
		nowPlayingLookup.promiseNowPlaying().eventually(
			LoopedInPromise.response({ np ->
				np?.libraryId?.also { libraryId ->
					WaitForConnectionDialog.show(this, libraryId)
				}
			}, messageHandler))
	}
}
