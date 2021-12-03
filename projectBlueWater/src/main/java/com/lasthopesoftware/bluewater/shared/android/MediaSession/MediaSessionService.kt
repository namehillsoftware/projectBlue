package com.lasthopesoftware.bluewater.shared.android.MediaSession

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.service.receivers.MediaSessionCallbackReceiver
import com.lasthopesoftware.bluewater.client.playback.service.receivers.RemoteControlReceiver
import com.lasthopesoftware.bluewater.shared.GenericBinder
import com.lasthopesoftware.bluewater.shared.makePendingIntentImmutable

class MediaSessionService : Service() {
	private val binder by lazy { GenericBinder(this) }

	private val remoteControlReceiver = lazy { ComponentName(packageName, RemoteControlReceiver::class.java.name) }

	private val lazyMediaSession = lazy {
		val newMediaSession = MediaSessionCompat(this, MediaSessionConstants.mediaSessionTag)
		newMediaSession.setCallback(
			MediaSessionCallbackReceiver(
				this,
				FileListParameters.getInstance(),
				FileStringListProvider(SelectedConnectionProvider(this))
			)
		)

		val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
		mediaButtonIntent.component = remoteControlReceiver.value

		val mediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0.makePendingIntentImmutable())
		newMediaSession.setMediaButtonReceiver(mediaPendingIntent)
		newMediaSession
	}

	val mediaSession = lazyMediaSession.value

	override fun onBind(intent: Intent) = binder

	override fun onCreate() {
		lazyMediaSession.value.isActive = true
		super.onCreate()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

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
