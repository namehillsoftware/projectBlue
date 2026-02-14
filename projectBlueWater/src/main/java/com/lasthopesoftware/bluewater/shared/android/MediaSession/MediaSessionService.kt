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
import com.lasthopesoftware.promises.extensions.toFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@UnstableApi class MediaSessionService : Service() {
	companion object {
		private val logger by lazyLogger<MediaSessionService>()
	}

	private val binder by lazy { GenericBinder(this) }

	private val lazyMediaSession = lazy {
		val libraryConnectionDependencies = LibraryConnectionRegistry(applicationDependencies)
		val libraryFilePropertiesDependents = LibraryFilePropertiesDependentsRegistry(
			applicationDependencies,
			libraryConnectionDependencies
		)

		val newMediaSession = MediaSessionCompat(this, MediaSessionConstants.mediaSessionTag)

		with (applicationDependencies) {
			newMediaSession.setCallback(
				MediaSessionCallbackReceiver(
					playbackServiceController,
					selectedLibraryIdProvider,
					libraryConnectionDependencies.libraryFilesProvider,
				)
			)

			val broadcaster = MediaSessionBroadcaster(
				nowPlayingState,
				libraryConnectionDependencies.libraryFilePropertiesProvider,
				libraryFilePropertiesDependents.imageBytesProvider,
				applicationDependencies.bitmapProducer,
				MediaSessionController(newMediaSession),
				applicationDependencies.selectedLibraryIdProvider,
				applicationDependencies.intentBuilder,
				registerForApplicationMessages,
			)

			Pair(broadcaster, newMediaSession)
		}
	}

	val mediaSession
		get() = lazyMediaSession.value.second

	override fun onBind(intent: Intent) = binder

	override fun onCreate() {
		mediaSession.isActive = true
	}

	override fun onDestroy() {
		if (lazyMediaSession.isInitialized()) {
			val (broadcaster, mediaSession) = lazyMediaSession.value

			try {
				mediaSession.isActive = false
				broadcaster
					.promiseClose()
					.toFuture()
					.get(30, TimeUnit.SECONDS)
				mediaSession.release()
			} catch (e: TimeoutException) {
				logger.warn("Timed out closing the broadcaster.", e)
			} catch (e: Exception) {
				logger.error("An unexpected error occurred destroying the MediaSessionService.", e)
			}
		}
		super.onDestroy()
	}
}
