package com.lasthopesoftware.bluewater

import android.content.Context
import android.os.Handler
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.SelectedConnectionFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.SelectedConnectionFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.ScopedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.authentication.SelectedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist.NowPlayingPlaylistMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.messages.TypedMessageBus
import com.lasthopesoftware.resources.strings.StringResources
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
	single { LocalBroadcastManager.getInstance(get()) }
	single { FilePropertyCache.getInstance() }
	single { CachedImageProvider.getInstance(get()) }
	single { LiveNowPlayingLookup.getInstance() }

	factory { ConnectionPoller(get()) }
	factory { StringResources(get()) }
	factory { SelectedConnectionProvider(get()) }
	factory { PlaybackServiceController(get()) }
	factory { ScopedRevisionProvider(get()) }
	factory { DefaultImageProvider(get()) }

	scope<NowPlayingActivity> {
		scoped { Handler(get<Context>().mainLooper) }
		scoped { MessageBus(get()) }
		scoped { TypedMessageBus<NowPlayingPlaylistMessage>(get()) }

		factory {
			SelectedConnectionFilePropertiesProvider(get<SelectedConnectionProvider>()) {
				ScopedFilePropertiesProvider(
					it,
					get<ScopedRevisionProvider>(),
					get<FilePropertyCache>()
				)
			}
		}

		factory {
			SelectedConnectionAuthenticationChecker(
				get<SelectedConnectionProvider>(),
				::ScopedConnectionAuthenticationChecker)
		}

		factory {
			SelectedConnectionFilePropertiesStorage(get<SelectedConnectionProvider>()) {
				ScopedFilePropertiesStorage(
					it,
					get<SelectedConnectionAuthenticationChecker>(),
					get<ScopedRevisionProvider>(),
					get<FilePropertyCache>())
			}
		}

		viewModel {
			NowPlayingPlaylistViewModel(
				get<MessageBus>(),
				get<LiveNowPlayingLookup>(),
				get<TypedMessageBus<NowPlayingPlaylistMessage>>()
			)
		}

		viewModel {
			NowPlayingScreenViewModel(
				get<MessageBus>(),
				InMemoryNowPlayingDisplaySettings,
				get<PlaybackServiceController>()
			)
		}

		viewModel {
			NowPlayingFilePropertiesViewModel(
				get<MessageBus>(),
				get<LiveNowPlayingLookup>(),
				get<SelectedConnectionProvider>(),
				get<SelectedConnectionFilePropertiesProvider>(),
				get<SelectedConnectionFilePropertiesStorage>(),
				get<SelectedConnectionAuthenticationChecker>(),
				get<PlaybackServiceController>(),
				get<ConnectionPoller>(),
				get<StringResources>(),
				get<NowPlayingScreenViewModel>(),
				get<NowPlayingScreenViewModel>()
			)
		}

		viewModel {
			NowPlayingCoverArtViewModel(
				get<MessageBus>(),
				get<LiveNowPlayingLookup>(),
				get<SelectedConnectionProvider>(),
				get<DefaultImageProvider>(),
				get<CachedImageProvider>(),
				get<ConnectionPoller>()
			)
		}
	}
}
