package com.lasthopesoftware.bluewater

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.startup.AppInitializer
import com.lasthopesoftware.bluewater.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.client.browsing.files.cached.DiskFileCache
import com.lasthopesoftware.bluewater.client.browsing.files.cached.access.CachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.AudioCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.ImageCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.files.cached.disk.AndroidDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.DiskFileAccessTimeUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.DiskFileCachePersistence
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier.DiskFileCacheStreamSupplier
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryNameLookup
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.CachedLibrarySettingsAccess
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.LibrarySettingsAccess
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.StoreLibrarySettings
import com.lasthopesoftware.bluewater.client.connection.PacketSender
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideProgressingLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerInfoXmlRequest
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.ValidConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.waking.AlarmConfiguration
import com.lasthopesoftware.bluewater.client.connection.waking.ServerAlarm
import com.lasthopesoftware.bluewater.client.connection.waking.ServerWakeSignal
import com.lasthopesoftware.bluewater.client.playback.caching.datasource.CachedDataSourceServerConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.CachingNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookupInitializer
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.sync.SyncSchedulerInitializer
import com.lasthopesoftware.bluewater.settings.repository.access.ApplicationSettingsRepository
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.ui.ScreenDimensions
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.lasthopesoftware.resources.bitmaps.DefaultAwareCachingBitmapProducer
import com.lasthopesoftware.resources.bitmaps.QueuedBitmapProducer
import com.lasthopesoftware.resources.network.ActiveNetworkFinder
import com.lasthopesoftware.resources.strings.Base64Encoder
import com.lasthopesoftware.resources.strings.StringResources

object ApplicationDependenciesContainer {

	private val connectionsRepository by lazy { PromisedConnectionsRepository() }

	private val sync = Any()

	@Volatile
	@OptIn(UnstableApi::class)
	private var attachedDependencies: AttachedDependencies? = null

	val Context.applicationDependencies: ApplicationDependencies
		// Double-checked initialization
		@OptIn(UnstableApi::class)
		get() = attachedDependencies
			?.takeIf { it.context == applicationContext }
			?: synchronized(sync) {
				attachedDependencies
					?.takeIf { it.context == applicationContext }
					?: run {
						val newDependencies = AttachedDependencies(applicationContext)
						attachedDependencies = newDependencies
						newDependencies
					}
			}

	@UnstableApi
	private class AttachedDependencies(val context: Context) : ApplicationDependencies {

		private val libraryRepository by lazy { LibraryRepository(context) }

		private val imageCachedFilesProvider by lazy { CachedFilesProvider(context, ImageCacheConfiguration) }

		private val imageDiskCacheDirectory by lazy { AndroidDiskCacheDirectoryProvider(context, ImageCacheConfiguration) }

		private val audioDiskCacheDirectoryProvider by lazy { AndroidDiskCacheDirectoryProvider(context, AudioCacheConfiguration) }

		private val audioCacheFilesProvider by lazy { CachedFilesProvider(context, AudioCacheConfiguration) }

		override val okHttpClients by lazy { OkHttpFactory(context) }

		override val audioCacheStreamSupplier by lazy {
			DiskFileCacheStreamSupplier(
				audioDiskCacheDirectoryProvider,
				DiskFileCachePersistence(
					context,
					AudioCacheConfiguration,
					audioCacheFilesProvider,
					diskFileAccessTimeUpdater
				),
				audioCacheFilesProvider
			)
		}

		override val audioFileCache by lazy {
			DiskFileCache(
				context,
				audioDiskCacheDirectoryProvider,
				AudioCacheConfiguration,
				audioCacheStreamSupplier,
				audioCacheFilesProvider,
				diskFileAccessTimeUpdater
			)
		}

		override val bitmapProducer by lazy { DefaultAwareCachingBitmapProducer(QueuedBitmapProducer, defaultImageProvider) }

		override val applicationSettings by lazy {
			CachingApplicationSettingsRepository(
				ApplicationSettingsRepository(context, sendApplicationMessages)
			)
		}

		override val selectedLibraryIdProvider by lazy {
			CachedSelectedLibraryIdProvider(SelectedLibraryIdProvider(applicationSettings))
		}

		override val nowPlayingStateMaintenance by lazy {
			CachingNowPlayingRepository(
				selectedLibraryIdProvider,
				NowPlayingRepository(
					selectedLibraryIdProvider,
					libraryProvider,
					libraryStorage,
				),
			)
		}

		override val nowPlayingState by lazy {
			AppInitializer
				.getInstance(context)
				.initializeComponent(cls<LiveNowPlayingLookupInitializer>())
		}

		override val libraryProvider: ILibraryProvider
			get() = libraryRepository

		override val libraryStorage: ILibraryStorage
			get() = libraryRepository

		override val librarySettingsProvider by lazy {
			val access = LibrarySettingsAccess(libraryProvider, libraryStorage)
			CachedLibrarySettingsAccess(access, access)
		}

		override val librarySettingsStorage: StoreLibrarySettings
			get() = librarySettingsProvider

		override val libraryNameLookup by lazy { LibraryNameLookup(librarySettingsProvider) }

		override val storedItemAccess by lazy { StoredItemAccess(context) }

		override val defaultImageProvider by lazy { DefaultImageProvider(context) }

		override val diskFileAccessTimeUpdater by lazy { DiskFileAccessTimeUpdater(context) }

		override val intentBuilder by lazy { IntentBuilder(context) }

		override val syncScheduler by lazy {
			AppInitializer
				.getInstance(context)
				.initializeComponent(cls<SyncSchedulerInitializer>())
		}

		override val connectionSettingsLookup by lazy {
			ValidConnectionSettingsLookup(ConnectionSettingsLookup(librarySettingsProvider))
		}

		override val connectionSessions by lazy {
			val serverLookup = ServerLookup(
				connectionSettingsLookup,
				ServerInfoXmlRequest(connectionSettingsLookup, okHttpClients),
			)

			val activeNetwork = ActiveNetworkFinder(context)
			ConnectionSessionManager(
                LibraryConnectionProvider(
					connectionSettingsLookup,
					ServerAlarm(serverLookup, activeNetwork, ServerWakeSignal(PacketSender())),
					CachedDataSourceServerConnectionProvider(
						LiveServerConnectionProvider(
							activeNetwork,
							Base64Encoder,
							serverLookup,
							connectionSettingsLookup,
							okHttpClients,
							okHttpClients
						),
						audioCacheStreamSupplier,
					),
					AlarmConfiguration.standard
				),
				connectionsRepository,
				sendApplicationMessages,
			)
		}

		override val libraryConnectionProvider
			get() = connectionSessions

		override val progressingLibraryConnectionProvider: ProvideProgressingLibraryConnections
			get() = connectionSessions

		override val sendApplicationMessages: SendApplicationMessages
			get() = ApplicationMessageBus.getApplicationMessageBus()

		override val registerForApplicationMessages: RegisterForApplicationMessages
			get() = ApplicationMessageBus.getApplicationMessageBus()

		override val playbackServiceController by lazy { PlaybackServiceController(context) }

		override val imageDiskFileCache by lazy {
			DiskFileCache(
				context,
				imageDiskCacheDirectory,
				ImageCacheConfiguration,
				DiskFileCacheStreamSupplier(
					imageDiskCacheDirectory,
					DiskFileCachePersistence(
						context,
						ImageCacheConfiguration,
						imageCachedFilesProvider,
						diskFileAccessTimeUpdater
					),
					imageCachedFilesProvider
				),
				imageCachedFilesProvider,
				diskFileAccessTimeUpdater
			)
		}

		override val screenDimensions by lazy { ScreenDimensions(context) }

		override val stringResources by lazy { StringResources(context) }

		override val nowPlayingDisplaySettings by lazy { InMemoryNowPlayingDisplaySettings() }
	}
}
