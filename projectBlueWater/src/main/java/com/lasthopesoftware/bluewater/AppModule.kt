package com.lasthopesoftware.bluewater

import android.content.Context
import android.os.Handler
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.SelectedConnectionFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.SelectedConnectionFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.UpdateFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckScopedRevisions
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.authentication.ScopedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.authentication.SelectedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.polling.PollForConnections
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist.NowPlayingPlaylistMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.messages.RegisterForMessages
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.messages.RegisterForTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.TypedMessageBus
import com.lasthopesoftware.resources.strings.GetStringResources
import com.lasthopesoftware.resources.strings.StringResources
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
	single { LocalBroadcastManager.getInstance(get()) }
	single<IFilePropertiesContainerRepository> { FilePropertyCache.getInstance() }
	single<PollForConnections> { ConnectionPoller(get()) }
	single<GetStringResources> { StringResources(get()) }
	single { CachedImageProvider.getInstance(get()) }

	factory<ProvideSelectedConnection> { SelectedConnectionProvider(get()) }
	factory<ControlPlaybackService> { PlaybackServiceController(get()) }
	factory<CheckScopedRevisions> { ScopedRevisionProvider(get()) }
	factory<ProvideDefaultImage> { DefaultImageProvider(get()) }

	scope<NowPlayingActivity> {
		scoped<GetNowPlayingState> { LiveNowPlayingLookup.getInstance() }

		scoped { Handler(get<Context>().mainLooper) }

		scoped { MessageBus(get()) }
		scoped<SendMessages> { get<MessageBus>() }
		scoped<RegisterForMessages> { get<MessageBus>() }

		scoped { TypedMessageBus<NowPlayingPlaylistMessage>(get()) }
		scoped<SendTypedMessages<NowPlayingPlaylistMessage>> { get<TypedMessageBus<NowPlayingPlaylistMessage>>() }
		scoped<RegisterForTypedMessages<NowPlayingPlaylistMessage>> { get<TypedMessageBus<NowPlayingPlaylistMessage>>() }

		factory<ProvideScopedFileProperties> {
			SelectedConnectionFilePropertiesProvider(get()) {
				ScopedFilePropertiesProvider(it, get(), get())
			}
		}

		factory<CheckIfScopedConnectionIsReadOnly> {
			SelectedConnectionAuthenticationChecker(get(), ::ScopedConnectionAuthenticationChecker)
		}

		factory<UpdateFileProperties> {
			SelectedConnectionFilePropertiesStorage(get()) {
				ScopedFilePropertiesStorage(it, get(), get(), get())
			}
		}

		viewModel { NowPlayingPlaylistViewModel(get(), get(), get()) }
		viewModel { NowPlayingScreenViewModel(get(), InMemoryNowPlayingDisplaySettings, get()) }
		viewModel {
			NowPlayingFilePropertiesViewModel(
				get(),
				get(),
				get(),
				get(),
				get(),
				get(),
				get(),
				get(),
				get(),
				get(),
				get()
			)
		}
		viewModel { NowPlayingCoverArtViewModel(get(), get(), get(), get(), get(), get()) }
	}
}
