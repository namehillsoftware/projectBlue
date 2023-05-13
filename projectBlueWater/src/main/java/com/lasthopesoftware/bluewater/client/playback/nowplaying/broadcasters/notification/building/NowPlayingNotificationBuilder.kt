package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building

import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ScopedCachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideScopedUrlKey
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pendingNextIntent
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pendingPauseIntent
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pendingPlayingIntent
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pendingPreviousIntent
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class NowPlayingNotificationBuilder(
	private val context: Context,
	private val mediaStyleNotificationSetup: SetupMediaStyleNotifications,
	private val scopedUrlKeys: ProvideScopedUrlKey,
	private val libraryIdProvider: ProvideSelectedLibraryId,
	private val scopedCachedFilePropertiesProvider: ScopedCachedFilePropertiesProvider,
	private val imageProvider: ProvideImages,
) : BuildNowPlayingNotificationContent, AutoCloseable {
	private val notificationSync = Any()

	private var viewStructure: ViewStructure? = null

	override fun promiseNowPlayingNotification(serviceFile: ServiceFile, isPlaying: Boolean): Promise<NotificationCompat.Builder> = synchronized(notificationSync) {
		return scopedUrlKeys
			.promiseUrlKey(serviceFile)
			.eventually { urlKeyHolder ->
				if (viewStructure?.urlKeyHolder != urlKeyHolder) {
					viewStructure?.release()
					viewStructure = null
				}

				val promisedSelectedLibraryId = libraryIdProvider.promiseSelectedLibraryId()

				if (urlKeyHolder == null) {
					return@eventually promisedSelectedLibraryId
						.then { it?.let(mediaStyleNotificationSetup::getMediaStyleNotification) }
				}

				val viewStructure = viewStructure ?: ViewStructure(urlKeyHolder).also { viewStructure = it }
				viewStructure.promisedNowPlayingImage =
					viewStructure.promisedNowPlayingImage ?: imageProvider.promiseFileBitmap(serviceFile)

				scopedCachedFilePropertiesProvider
					.promiseFileProperties(serviceFile)
					.eventually { fileProperties ->
						val artist = fileProperties[KnownFileProperties.Artist]
						val name = fileProperties[KnownFileProperties.Name]
						promisedSelectedLibraryId
							.then {
								it
									?.let(mediaStyleNotificationSetup::getMediaStyleNotification)
									?.let { builder -> addButtons(builder, isPlaying) }
									?.setOngoing(isPlaying)
									?.setContentTitle(name)
									?.setContentText(artist)
							}
					}
					.eventually { builder ->
						if (viewStructure.urlKeyHolder != urlKeyHolder) builder.toPromise()
						else viewStructure.promisedNowPlayingImage
							?.then(
								{ bitmap -> bitmap?.let{ builder?.setLargeIcon(it) } },
								{ builder }
							)
							.keepPromise(builder)
					}
			}
	}

	override fun promiseLoadingNotification(isPlaying: Boolean): Promise<NotificationCompat.Builder> =
		libraryIdProvider
			.promiseSelectedLibraryId()
			.then {
				it?.let { l ->
					if (isPlaying) getPlayingLoadingNotification(l) else getNotPlayingLoadingNotification(l)
				}
			}

	override fun close() { viewStructure?.release() }

	private fun getPlayingLoadingNotification(libraryId: LibraryId): NotificationCompat.Builder {
		return addButtons(mediaStyleNotificationSetup.getMediaStyleNotification(libraryId), true)
			.setOngoing(true)
			.setContentTitle(context.getString(R.string.lbl_loading))
	}

	private fun getNotPlayingLoadingNotification(libraryId: LibraryId): NotificationCompat.Builder {
		return addButtons(mediaStyleNotificationSetup.getMediaStyleNotification(libraryId), false)
			.setOngoing(false)
			.setContentTitle(context.getString(R.string.lbl_loading))
	}

	private fun addButtons(builder: NotificationCompat.Builder, isPlaying: Boolean): NotificationCompat.Builder =
		builder
			.addAction(
				NotificationCompat.Action(
					R.drawable.av_previous_white,
					context.getString(R.string.btn_previous),
					pendingPreviousIntent(context)
				)
			)
			.addAction(
				if (isPlaying) NotificationCompat.Action(
					R.drawable.av_pause_white,
					context.getString(R.string.btn_pause),
					pendingPauseIntent(context)
				) else NotificationCompat.Action(
					R.drawable.av_play_white,
					context.getString(R.string.btn_play),
					pendingPlayingIntent(context)
				)
			)
			.addAction(
				NotificationCompat.Action(
					R.drawable.av_next_white,
					context.getString(R.string.btn_next),
					pendingNextIntent(context)
				)
			)

	private class ViewStructure(val urlKeyHolder: UrlKeyHolder<ServiceFile>) {
		var promisedNowPlayingImage: Promise<Bitmap?>? = null

		fun release() {
			promisedNowPlayingImage?.cancel()
		}
	}
}
