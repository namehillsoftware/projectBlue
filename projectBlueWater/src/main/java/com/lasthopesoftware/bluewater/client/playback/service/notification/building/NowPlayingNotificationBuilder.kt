package com.lasthopesoftware.bluewater.client.playback.service.notification.building

import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ProvideImages
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pendingNextIntent
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pendingPauseIntent
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pendingPlayingIntent
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pendingPreviousIntent
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class NowPlayingNotificationBuilder(
	private val context: Context,
	private val mediaStyleNotificationSetup: SetupMediaStyleNotifications,
	private val connectionProvider: IConnectionProvider,
	private val scopedCachedFilePropertiesProvider: ScopedCachedFilePropertiesProvider,
	private val imageProvider: ProvideImages
) : BuildNowPlayingNotificationContent, AutoCloseable {
	private val lazyPlayingLoadingNotification by lazy {
		addButtons(mediaStyleNotificationSetup.mediaStyleNotification, true)
			.setOngoing(true)
			.setContentTitle(context.getString(R.string.lbl_loading))
	}

	private val lazyNotPlayingLoadingNotification by lazy {
		addButtons(mediaStyleNotificationSetup.mediaStyleNotification, false)
			.setOngoing(false)
			.setContentTitle(context.getString(R.string.lbl_loading))
	}

	private var viewStructure: ViewStructure? = null

	@Synchronized
	override fun promiseNowPlayingNotification(serviceFile: ServiceFile, isPlaying: Boolean): Promise<NotificationCompat.Builder> {
		val urlKeyHolder = UrlKeyHolder(connectionProvider.urlProvider.baseUrl!!, serviceFile.key)
		if (viewStructure?.urlKeyHolder != urlKeyHolder) {
			viewStructure?.release()
			viewStructure = null
		}

		val viewStructure = viewStructure ?: ViewStructure(urlKeyHolder).also { viewStructure = it }
		viewStructure.promisedNowPlayingImage = viewStructure.promisedNowPlayingImage ?: imageProvider.promiseFileBitmap(serviceFile)

		val promisedFileProperties = viewStructure.promisedFileProperties
			?: scopedCachedFilePropertiesProvider.promiseFileProperties(serviceFile).also { viewStructure.promisedFileProperties = it }

		return promisedFileProperties
			.eventually { fileProperties ->
				val artist = fileProperties[KnownFileProperties.ARTIST]
				val name = fileProperties[KnownFileProperties.NAME]
				val builder = addButtons(mediaStyleNotificationSetup.mediaStyleNotification, isPlaying)
					.setOngoing(isPlaying)
					.setContentTitle(name)
					.setContentText(artist)
				if (viewStructure.urlKeyHolder != urlKeyHolder) return@eventually Promise(builder)
				viewStructure.promisedNowPlayingImage
					?.then(
						{ bitmap -> bitmap?.let(builder::setLargeIcon) },
						{ builder }
					)
					.keepPromise(builder)
			}
	}

	override fun getLoadingNotification(isPlaying: Boolean): NotificationCompat.Builder =
		if (isPlaying) lazyPlayingLoadingNotification else lazyNotPlayingLoadingNotification

	override fun close() { viewStructure?.release() }

	private fun addButtons(builder: NotificationCompat.Builder, isPlaying: Boolean): NotificationCompat.Builder =
		builder
			.addAction(
				NotificationCompat.Action(
					R.drawable.av_previous_dark,
					context.getString(R.string.btn_previous),
					pendingPreviousIntent(context)
				)
			)
			.addAction(
				if (isPlaying) NotificationCompat.Action(
					R.drawable.av_pause_dark,
					context.getString(R.string.btn_pause),
					pendingPauseIntent(context)
				) else NotificationCompat.Action(
					R.drawable.av_play_dark,
					context.getString(R.string.btn_play),
					pendingPlayingIntent(context)
				)
			)
			.addAction(
				NotificationCompat.Action(
					R.drawable.av_next_dark,
					context.getString(R.string.btn_next),
					pendingNextIntent(context)
				)
			)

	private class ViewStructure(val urlKeyHolder: UrlKeyHolder<Int>) {
		var promisedFileProperties: Promise<Map<String, String>>? = null
		var promisedNowPlayingImage: Promise<Bitmap?>? = null

		fun release() {
			promisedNowPlayingImage?.cancel()
		}
	}
}
