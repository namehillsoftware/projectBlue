package com.lasthopesoftware.bluewater.shared.android.MediaSession

import android.app.Service
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.ApplicationContextAttachedApplicationDependencies.applicationDependencies
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.MediaSessionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.InMemoryNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.client.playback.service.receivers.MediaSessionCallbackReceiver
import com.lasthopesoftware.bluewater.shared.android.services.GenericBinder
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus
import com.lasthopesoftware.promises.toFuture
import java.util.concurrent.TimeUnit

@UnstableApi class MediaSessionService : Service() {
	private val binder by lazy { GenericBinder(this) }

	private val nowPlayingRepository by lazy {
		val libraryRepository = LibraryRepository(this)

		NowPlayingRepository(
			getCachedSelectedLibraryIdProvider(),
			libraryRepository,
			libraryRepository,
			InMemoryNowPlayingState,
		)
	}

	private val libraryConnectionProvider by lazy { ConnectionSessionManager.get(this) }

	private val revisionProvider by lazy { LibraryRevisionProvider(libraryConnectionProvider) }

	private val freshLibraryFileProperties by lazy {
		FilePropertiesProvider(
			GuaranteedLibraryConnectionProvider(libraryConnectionProvider),
			revisionProvider,
			FilePropertyCache,
		)
	}

	private val libraryFilePropertiesProvider by lazy {
		CachedFilePropertiesProvider(
			libraryConnectionProvider,
			FilePropertyCache,
			freshLibraryFileProperties,
		)
	}

	private	val imageProvider by lazy { CachedImageProvider.getInstance(this) }

	private val libraryIdProvider by lazy { getCachedSelectedLibraryIdProvider() }

	private val lazyMediaSession = lazy {
		val newMediaSession = MediaSessionCompat(this, MediaSessionConstants.mediaSessionTag)
		val connectionProvider = ConnectionSessionManager.get(this)
		newMediaSession.setCallback(
			MediaSessionCallbackReceiver(
				PlaybackServiceController(this),
				libraryIdProvider,
				ItemStringListProvider(
					FileListParameters,
					LibraryFileStringListProvider(connectionProvider)
				)
			)
		)

		val broadcaster = MediaSessionBroadcaster(
			nowPlayingRepository,
			libraryFilePropertiesProvider,
			imageProvider,
			MediaSessionController(newMediaSession),
			ApplicationMessageBus.getApplicationMessageBus()
		)

		Pair(broadcaster, newMediaSession)
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

			val futureLibraryId = libraryIdProvider.promiseSelectedLibraryId().toFuture()

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
