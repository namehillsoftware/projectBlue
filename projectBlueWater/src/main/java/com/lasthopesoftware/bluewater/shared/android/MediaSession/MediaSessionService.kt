package com.lasthopesoftware.bluewater.shared.android.MediaSession

import android.app.Service
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.ApplicationDependenciesContainer.applicationDependencies
import com.lasthopesoftware.bluewater.android.services.GenericBinder
import com.lasthopesoftware.bluewater.client.browsing.files.properties.LibraryFilePropertiesDependentsRegistry
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionRegistry
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.MediaSessionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.receivers.MediaSessionCallbackReceiver
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.getSafely
import com.lasthopesoftware.promises.extensions.toFuture
import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.lasthopesoftware.resources.closables.PromisingCloseableManager
import java.util.concurrent.TimeoutException

@UnstableApi class MediaSessionService : Service() {
	companion object {
		private val logger by lazyLogger<MediaSessionService>()
	}

	private val binder by lazy { GenericBinder(this) }

	private val promisingCloseableManager = PromisingCloseableManager()

	private val libraryConnectionDependencies by lazy { LibraryConnectionRegistry(applicationDependencies) }

	val mediaSession by lazy {
		val newMediaSession = MediaSessionCompat(this, MediaSessionConstants.mediaSessionTag)
		with (applicationDependencies) {
			newMediaSession.setCallback(
				MediaSessionCallbackReceiver(
					playbackServiceController,
					selectedLibraryIdProvider,
					libraryConnectionDependencies.libraryFilesProvider,
				)
			)

			val libraryFilePropertiesDependents = LibraryFilePropertiesDependentsRegistry(
				applicationDependencies,
				libraryConnectionDependencies
			)
			val mediaSessionController = promisingCloseableManager.manage(MediaSessionController(newMediaSession))
			promisingCloseableManager.manage(
				MediaSessionBroadcaster(
					nowPlayingState,
					libraryConnectionDependencies.libraryFilePropertiesProvider,
					libraryFilePropertiesDependents.imageBytesProvider,
					bitmapProducer,
					mediaSessionController,
					selectedLibraryIdProvider,
					intentBuilder,
					registerForApplicationMessages,
				) as PromisingCloseable
			)
		}

		newMediaSession
	}

	override fun onBind(intent: Intent) = binder

	override fun onDestroy() {
		try {
			promisingCloseableManager
				.promiseClose()
				.toFuture()
				.getSafely()
		} catch (e: TimeoutException) {
			logger.warn("Timed out closing the resources.", e)
		} catch (e: Exception) {
			logger.error("An unexpected error occurred closing resources.", e)
		}
		super.onDestroy()
	}
}
