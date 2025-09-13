package com.lasthopesoftware.bluewater

import android.app.NotificationManager
import android.content.Context
import androidx.startup.Initializer
import com.lasthopesoftware.bluewater.ApplicationDependenciesContainer.applicationDependencies
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionRegistry
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionSettingsChangeReceiver
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble.PlaybackFileStartedScrobbleDroidProxy
import com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble.PlaybackFileStoppedScrobbleDroidProxy
import com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble.ScrobbleIntentProvider
import com.lasthopesoftware.bluewater.client.stored.library.permissions.StoragePermissionsRequestNotificationBuilder
import com.lasthopesoftware.bluewater.client.stored.library.permissions.read.StorageReadPermissionsRequestNotificationBuilder
import com.lasthopesoftware.bluewater.client.stored.library.permissions.read.StorageReadPermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.stored.sync.notifications.SyncChannelProperties
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.SyncItemStateChangedListener
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver

class MessageReceiversInitializer : Initializer<Unit> {
	override fun create(context: Context) {
		with (context.applicationDependencies) {
			registerForApplicationMessages.registerReceiver(ConnectionSessionSettingsChangeReceiver(connectionSessions))

			val notificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

			val syncChannelProperties by lazy { SyncChannelProperties(context) }

			val storageReadPermissionsConfiguration by lazy {
				NotificationsConfiguration(
					syncChannelProperties.channelId,
					336
				)
			}

			val storagePermissionsRequestNotificationBuilder by lazy {
				StoragePermissionsRequestNotificationBuilder(
					context,
					stringResources,
					intentBuilder,
					syncChannelProperties
				)
			}

			val storageReadPermissionsRequestNotificationBuilder by lazy {
				StorageReadPermissionsRequestNotificationBuilder(storagePermissionsRequestNotificationBuilder)
			}

			registerForApplicationMessages.registerReceiver { readPermissionsNeeded : StorageReadPermissionsRequestedBroadcaster.ReadPermissionsNeeded ->
				notificationManager.notify(
					storageReadPermissionsConfiguration.notificationId,
					storageReadPermissionsRequestNotificationBuilder
						.buildReadPermissionsRequestNotification(readPermissionsNeeded.libraryId))
			}

			registerForApplicationMessages.registerReceiver(
				PlaybackFileStartedScrobbleDroidProxy(
					context,
					LibraryConnectionRegistry(this).libraryFilePropertiesProvider,
					ScrobbleIntentProvider,
				)
			)

			registerForApplicationMessages.registerReceiver(
				PlaybackFileStoppedScrobbleDroidProxy(context, ScrobbleIntentProvider)
			)

			registerForApplicationMessages.registerReceiver(SyncItemStateChangedListener(syncScheduler))
		}
	}

	override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
