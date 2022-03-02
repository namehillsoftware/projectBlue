package com.lasthopesoftware.bluewater.shared.android.MediaSession

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.service.receivers.MediaSessionCallbackReceiver
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.makePendingIntentImmutable
import com.lasthopesoftware.bluewater.shared.android.services.GenericBinder

class MediaSessionService : Service() {
	private val binder by lazy { GenericBinder(this) }

	private val lazyMediaSession = lazy {
		val newMediaSession = MediaSessionCompat(this, MediaSessionConstants.mediaSessionTag)
		val connectionProvider = ConnectionSessionManager.get(this)
		newMediaSession.setCallback(
			MediaSessionCallbackReceiver(
				this,
				SelectedBrowserLibraryIdentifierProvider(getApplicationSettingsRepository()),
				ItemStringListProvider(
					ItemProvider(connectionProvider),
					FileListParameters,
					LibraryFileStringListProvider(connectionProvider)
				)
			)
		)

		val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)

		val mediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0.makePendingIntentImmutable())
		newMediaSession.setMediaButtonReceiver(mediaPendingIntent)
		newMediaSession
	}

	val mediaSession
		get() = lazyMediaSession.value

	override fun onBind(intent: Intent) = binder

	override fun onCreate() {
		lazyMediaSession.value.isActive = true
	}

	override fun onDestroy() {
		if (lazyMediaSession.isInitialized()) {
			with (lazyMediaSession.value) {
				isActive = false
				release()
			}
		}
		super.onDestroy()
	}
}
