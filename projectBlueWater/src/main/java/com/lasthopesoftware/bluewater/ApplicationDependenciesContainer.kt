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
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.shared.android.ui.ScreenDimensions
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages

object ApplicationDependenciesContainer {

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

		private val imageCachedFilesProvider by lazy { CachedFilesProvider(context, ImageCacheConfiguration) }

		private val imageDiskCacheDirectory by lazy { AndroidDiskCacheDirectoryProvider(context, ImageCacheConfiguration) }

		override val defaultImageProvider by lazy { DefaultImageProvider(context) }

		override val diskFileAccessTimeUpdater by lazy { DiskFileAccessTimeUpdater(context) }

		override val intentBuilder by lazy { IntentBuilder(context) }

		override val syncScheduler by lazy { SyncScheduler(context) }

		override val connectionSessions by lazy { context.buildNewConnectionSessionManager() }

		override val libraryConnectionProvider by lazy { connectionSessions }

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
	}
}
