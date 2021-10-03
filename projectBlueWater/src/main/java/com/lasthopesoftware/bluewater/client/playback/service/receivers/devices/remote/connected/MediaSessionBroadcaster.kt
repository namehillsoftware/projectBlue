package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ProvideImages
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.IRemoteBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected.MediaSessionBroadcaster
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response
import org.slf4j.LoggerFactory

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class MediaSessionBroadcaster(
	private val context: Context,
	private val scopedCachedFilePropertiesProvider: ScopedCachedFilePropertiesProvider,
	private val imageProvider: ProvideImages,
	private val mediaSession: MediaSessionCompat
) : IRemoteBroadcaster {
	@Volatile
	private var playbackState = PlaybackState.STATE_STOPPED

	@Volatile
	private var trackPosition: Long = -1

	@Volatile
	private var mediaMetadata = MediaMetadataCompat.Builder().build()

	@Actions
	@Volatile
	private var capabilities = standardCapabilities
	private var remoteClientBitmap: Bitmap? = null

	@Volatile
	private var isPlaying = false
	override fun setPlaying() {
		isPlaying = true
		val builder = PlaybackStateCompat.Builder()
		capabilities = PlaybackState.ACTION_PAUSE or standardCapabilities
		builder.setActions(capabilities)
		playbackState = PlaybackState.STATE_PLAYING
		builder.setState(playbackState, trackPosition, playbackSpeed)
		mediaSession.setPlaybackState(builder.build())
	}

	override fun setPaused() {
		isPlaying = false
		val builder = PlaybackStateCompat.Builder()
		capabilities = PlaybackState.ACTION_PLAY or standardCapabilities
		builder.setActions(capabilities)
		playbackState = PlaybackState.STATE_PAUSED
		builder.setState(
			playbackState,
			trackPosition,
			playbackSpeed
		)
		mediaSession.setPlaybackState(builder.build())
		updateClientBitmap(null)
	}

	override fun setStopped() {
		isPlaying = false
		val builder = PlaybackStateCompat.Builder()
		capabilities = PlaybackState.ACTION_PLAY or standardCapabilities
		builder.setActions(capabilities)
		playbackState = PlaybackState.STATE_STOPPED
		builder.setState(
			playbackState,
			trackPosition,
			playbackSpeed
		)
		mediaSession.setPlaybackState(builder.build())
		updateClientBitmap(null)
	}

	override fun updateNowPlaying(serviceFile: ServiceFile) {
		scopedCachedFilePropertiesProvider
			.promiseFileProperties(serviceFile)
			.eventually(response({ fileProperties ->
				val artist = fileProperties[KnownFileProperties.ARTIST]
				val name = fileProperties[KnownFileProperties.NAME]
				val album = fileProperties[KnownFileProperties.ALBUM]
				val duration =
					FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties)
						.toLong()

				val metadataBuilder = MediaMetadataCompat.Builder(mediaMetadata)
				metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
				metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM, album)
				metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, name)
				metadataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, duration)

				val trackNumberString = fileProperties[KnownFileProperties.TRACK]
				val trackNumber = trackNumberString?.toLong()
				if (trackNumber != null) {
					metadataBuilder.putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, trackNumber)
				}
				mediaSession.setMetadata(metadataBuilder.build().also { mediaMetadata = it })
			}, context))

		if (!isPlaying) {
			updateClientBitmap(null)
			return
		}

		imageProvider
			.promiseFileBitmap(serviceFile)
			.eventually(response(::updateClientBitmap, context))
			.excuse { e ->
				logger.warn(
					"There was an error getting the image for the file with id `" + serviceFile.key + "`",
					e
				)
			}
	}

	override fun updateTrackPosition(trackPosition: Long) {
		val builder = PlaybackStateCompat.Builder()
		builder.setActions(capabilities)
		builder.setState(
			playbackState,
			trackPosition.also {
				this.trackPosition = it
			},
			playbackSpeed
		)
		mediaSession.setPlaybackState(builder.build())
	}

	@Synchronized
	private fun updateClientBitmap(bitmap: Bitmap?) {
		if (remoteClientBitmap == bitmap) return
		val metadataBuilder = MediaMetadataCompat.Builder(mediaMetadata)
		metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)
		mediaSession.setMetadata(metadataBuilder.build().also { mediaMetadata = it })
		remoteClientBitmap = bitmap
	}

	@IntDef(
		flag = true,
		value = [PlaybackState.ACTION_STOP.toInt(), PlaybackState.ACTION_PAUSE.toInt(), PlaybackState.ACTION_PLAY.toInt(), PlaybackState.ACTION_REWIND.toInt(), PlaybackState.ACTION_SKIP_TO_PREVIOUS.toInt(), PlaybackState.ACTION_SKIP_TO_NEXT.toInt(), PlaybackState.ACTION_FAST_FORWARD.toInt(), PlaybackState.ACTION_SET_RATING.toInt(), PlaybackState.ACTION_SEEK_TO.toInt(), PlaybackState.ACTION_PLAY_PAUSE.toInt(), PlaybackState.ACTION_PLAY_FROM_MEDIA_ID.toInt(), PlaybackState.ACTION_PLAY_FROM_SEARCH.toInt(), PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM.toInt()]
	)
	private annotation class Actions

	companion object {
		private val logger = LoggerFactory.getLogger(
			MediaSessionBroadcaster::class.java
		)
		private const val playbackSpeed = 1.0f

		@Actions
		private val standardCapabilities = PlaybackState.ACTION_PLAY_PAUSE or
			PlaybackState.ACTION_SKIP_TO_NEXT or
			PlaybackState.ACTION_SKIP_TO_PREVIOUS or
			PlaybackState.ACTION_STOP
	}
}
