package com.lasthopesoftware.bluewater

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.client.browsing.files.cached.DiskFileCache
import com.lasthopesoftware.bluewater.client.browsing.files.cached.access.CachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.ImageCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.files.cached.disk.AndroidDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.DiskFileAccessTimeUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.DiskFileCachePersistence
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier.DiskFileCacheStreamSupplier
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.connection.PacketSender
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveUrlProvider
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsValidation
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester
import com.lasthopesoftware.bluewater.client.connection.waking.AlarmConfiguration
import com.lasthopesoftware.bluewater.client.connection.waking.ServerAlarm
import com.lasthopesoftware.bluewater.client.connection.waking.ServerWakeSignal
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.settings.repository.access.ApplicationSettingsRepository
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.ui.ScreenDimensions
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
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

		private val applicationSettingsRepository by lazy {
			CachingApplicationSettingsRepository(
				ApplicationSettingsRepository(context, sendApplicationMessages)
			)
		}

		override val selectedLibraryIdProvider by lazy {
			CachedSelectedLibraryIdProvider(SelectedLibraryIdProvider(applicationSettingsRepository))
		}

		override val libraryProvider: ILibraryProvider
			get() = libraryRepository

		override val libraryStorage: ILibraryStorage
			get() = libraryRepository

		override val storedItemAccess by lazy { StoredItemAccess(context) }

		override val defaultImageProvider by lazy { DefaultImageProvider(context) }

		override val diskFileAccessTimeUpdater by lazy { DiskFileAccessTimeUpdater(context) }

		override val intentBuilder by lazy { IntentBuilder(context) }

		override val syncScheduler by lazy { SyncScheduler(context) }

		override val connectionSessions by lazy {
			val connectionSettingsLookup = ConnectionSettingsLookup(LibraryRepository(context))
			val serverLookup = ServerLookup(
				connectionSettingsLookup,
				ServerInfoXmlRequest(LibraryRepository(context), OkHttpFactory),
			)

			val activeNetwork = ActiveNetworkFinder(context)
			ConnectionSessionManager(
				ConnectionTester,
				LibraryConnectionProvider(
					ConnectionSettingsValidation,
					connectionSettingsLookup,
					ServerAlarm(serverLookup, activeNetwork, ServerWakeSignal(PacketSender())),
					LiveUrlProvider(
						activeNetwork,
						UrlScanner(
							Base64Encoder,
							ConnectionTester,
							serverLookup,
							connectionSettingsLookup,
							OkHttpFactory
						)
					),
					OkHttpFactory,
					AlarmConfiguration.standard
				),
				connectionsRepository,
				sendApplicationMessages,
			)
		}

		override val libraryConnectionProvider
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
	}
}
