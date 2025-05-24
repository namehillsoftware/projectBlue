package com.lasthopesoftware.bluewater.shared.android.MediaSession

import android.app.Service
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.ApplicationDependenciesContainer.applicationDependencies
import com.lasthopesoftware.bluewater.client.browsing.files.properties.LibraryFilePropertiesDependentsRegistry
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionRegistry
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.MediaSessionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.receivers.MediaSessionCallbackReceiver
import com.lasthopesoftware.bluewater.shared.android.services.GenericBinder
import com.lasthopesoftware.promises.extensions.toFuture
import java.util.concurrent.TimeUnit

@UnstableApi class MediaSessionService : Service() {
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
					libraryConnectionDependencies.itemStringListProvider,
				)
			)

			val broadcaster = MediaSessionBroadcaster(
				nowPlayingState,
				libraryConnectionDependencies.libraryFilePropertiesProvider,
				libraryFilePropertiesDependents.imageBytesProvider,
				applicationDependencies.bitmapProducer,
				MediaSessionController(newMediaSession),
				registerForApplicationMessages,
			)

			Pair(broadcaster, newMediaSession)
		}
	}

	val mediaSession
		get() = lazyMediaSession.value.second

	override fun onBind(intent: Intent) = binder

	override fun onCreate() {
		lazyMediaSession.value.second.isActive = true
	}

	override fun onDestroy() {
		if (lazyMediaSession.isInitialized()) {
			val (broadcaster, mediaSession) = lazyMediaSession.value

			val futureLibraryId = applicationDependencies.selectedLibraryIdProvider.promiseSelectedLibraryId().toFuture()

			broadcaster.close()
			with (mediaSession) {
				isActive = false
				futureLibraryId
					.get(30, TimeUnit.SECONDS)
					?.also {
						setSessionActivity(applicationDependencies.intentBuilder.buildPendingNowPlayingIntent(it))
					}
				release()
			}
		}
		super.onDestroy()
	}
}
