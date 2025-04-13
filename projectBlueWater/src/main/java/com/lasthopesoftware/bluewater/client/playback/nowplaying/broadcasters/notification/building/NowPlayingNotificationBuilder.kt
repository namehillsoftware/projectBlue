package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building

import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pendingNextIntent
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pendingPauseIntent
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pendingPlayingIntent
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pendingPreviousIntent
import com.lasthopesoftware.bluewater.shared.images.bytes.GetImageBytes
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.bitmaps.ProduceBitmaps
import com.namehillsoftware.handoff.promises.Promise

class NowPlayingNotificationBuilder(
	private val context: Context,
	private val mediaStyleNotificationSetup: SetupMediaStyleNotifications,
	private val urlKeyProvider: ProvideUrlKey,
	private val filePropertiesProvider: ProvideLibraryFileProperties,
	private val imageProvider: GetImageBytes,
	private val bitmapProducer: ProduceBitmaps,
) : BuildNowPlayingNotificationContent, AutoCloseable {
	private val notificationSync = Any()

	private var viewStructure: ViewStructure? = null

	override fun promiseNowPlayingNotification(libraryId: LibraryId, serviceFile: ServiceFile, isPlaying: Boolean): Promise<NotificationCompat.Builder?> = synchronized(notificationSync) {
		return urlKeyProvider
			.promiseUrlKey(libraryId, serviceFile)
			.eventually { urlKeyHolder ->
				if (viewStructure?.urlKeyHolder != urlKeyHolder) {
					viewStructure?.release()
					viewStructure = null
				}

				if (urlKeyHolder == null) {
					return@eventually mediaStyleNotificationSetup.getMediaStyleNotification(libraryId).toPromise()
				}

				val viewStructure = viewStructure ?: ViewStructure(urlKeyHolder).also { viewStructure = it }
				viewStructure.promisedNowPlayingImage =
					viewStructure.promisedNowPlayingImage ?: imageProvider.promiseImageBytes(libraryId, serviceFile).eventually(bitmapProducer::promiseBitmap)

				filePropertiesProvider
					.promiseFileProperties(libraryId, serviceFile)
					.then { fileProperties ->
						val artist = fileProperties[NormalizedFileProperties.Artist]
						val name = fileProperties[NormalizedFileProperties.Name]
						addButtons(mediaStyleNotificationSetup.getMediaStyleNotification(libraryId), libraryId, isPlaying)
							.setOngoing(isPlaying)
							.setContentTitle(name)
							.setContentText(artist)
					}
					.eventually { builder ->
						if (viewStructure.urlKeyHolder != urlKeyHolder) builder.toPromise()
						else viewStructure.promisedNowPlayingImage
							?.then(
								{ bitmap -> bitmap?.let{ builder.setLargeIcon(it) } },
								{ builder }
							)
							.keepPromise(builder)
					}
			}
	}

	override fun promiseLoadingNotification(libraryId: LibraryId, isPlaying: Boolean): Promise<NotificationCompat.Builder?> =
		Promise(if (isPlaying) getPlayingLoadingNotification(libraryId) else getNotPlayingLoadingNotification(libraryId))

	override fun close() { viewStructure?.release() }

	private fun getPlayingLoadingNotification(libraryId: LibraryId): NotificationCompat.Builder {
		return addButtons(mediaStyleNotificationSetup.getMediaStyleNotification(libraryId), libraryId, true)
			.setOngoing(true)
			.setContentTitle(context.getString(R.string.lbl_loading))
	}

	private fun getNotPlayingLoadingNotification(libraryId: LibraryId): NotificationCompat.Builder {
		return addButtons(mediaStyleNotificationSetup.getMediaStyleNotification(libraryId), libraryId, false)
			.setOngoing(false)
			.setContentTitle(context.getString(R.string.lbl_loading))
	}

	private fun addButtons(builder: NotificationCompat.Builder, libraryId: LibraryId, isPlaying: Boolean): NotificationCompat.Builder =
		builder
			.addAction(
				NotificationCompat.Action(
					R.drawable.av_previous_white,
					context.getString(R.string.btn_previous),
					pendingPreviousIntent(context, libraryId)
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
					pendingPlayingIntent(context, libraryId)
				)
			)
			.addAction(
				NotificationCompat.Action(
					R.drawable.av_next_white,
					context.getString(R.string.btn_next),
					pendingNextIntent(context, libraryId)
				)
			)

	private class ViewStructure(val urlKeyHolder: UrlKeyHolder<ServiceFile>) {
		var promisedNowPlayingImage: Promise<Bitmap?>? = null

		fun release() {
			promisedNowPlayingImage?.cancel()
		}
	}
}
