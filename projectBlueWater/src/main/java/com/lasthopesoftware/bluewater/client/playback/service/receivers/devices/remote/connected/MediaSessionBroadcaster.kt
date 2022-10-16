package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ScopedCachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideScopedUrlKey
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.IRemoteBroadcaster
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response

class MediaSessionBroadcaster(
	private val handler: Handler,
	private val scopedUrlKeyProvider: ProvideScopedUrlKey,
	private val scopedCachedFilePropertiesProvider: ScopedCachedFilePropertiesProvider,
	private val imageProvider: ProvideImages,
	private val mediaSession: MediaSessionCompat
) : IRemoteBroadcaster {
	@Volatile
	private var activeUrlKeyPair: Pair<ServiceFile, UrlKeyHolder<ServiceFile>>? = null

	@Volatile
	private var playbackState = PlaybackStateCompat.STATE_STOPPED

	@Volatile
	private var trackPosition: Long = -1

	@Volatile
	private var mediaMetadata = MediaMetadataCompat.Builder().build()

	@Volatile
	private var capabilities = standardCapabilities
	private var remoteClientBitmap: Bitmap? = null

	@Volatile
	private var isPlaying = false

	override fun setPlaying() {
		isPlaying = true
		val builder = PlaybackStateCompat.Builder()
		capabilities = PlaybackStateCompat.ACTION_PAUSE or standardCapabilities
		builder.setActions(capabilities)
		playbackState = PlaybackStateCompat.STATE_PLAYING
		builder.setState(playbackState, trackPosition, playbackSpeed)
		mediaSession.setPlaybackState(builder.build())
	}

	override fun setPaused() {
		isPlaying = false
		val builder = PlaybackStateCompat.Builder()
		capabilities = PlaybackState.ACTION_PLAY or standardCapabilities
		builder.setActions(capabilities)
		playbackState = PlaybackState.STATE_PAUSED
		builder.setState(playbackState, trackPosition, playbackSpeed)
		mediaSession.setPlaybackState(builder.build())
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
		clearClientBitmap()
	}

	override fun updateNowPlaying(serviceFile: ServiceFile) {
		scopedUrlKeyProvider.promiseUrlKey(serviceFile).then {
			it?.also { urlKeyHolder -> activeUrlKeyPair = Pair(serviceFile, urlKeyHolder) }
		}

		val promisedBitmap = imageProvider.promiseFileBitmap(serviceFile)

		scopedCachedFilePropertiesProvider
			.promiseFileProperties(serviceFile)
			.eventually { fileProperties ->
				val artist = fileProperties[KnownFileProperties.Artist]
				val name = fileProperties[KnownFileProperties.Name]
				val album = fileProperties[KnownFileProperties.Album]
				val duration =
					FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties)
						.toLong()

				val metadataBuilder = MediaMetadataCompat.Builder(mediaMetadata)
				metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
				metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM, album)
				metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, name)
				metadataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, duration)

				val trackNumberString = fileProperties[KnownFileProperties.Track]
				val trackNumber = trackNumberString?.toLong()
				if (trackNumber != null) {
					metadataBuilder.putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, trackNumber)
				}

				promisedBitmap.eventually(response({
					if (remoteClientBitmap != it) {
						metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it)
						remoteClientBitmap = it
					}
					mediaSession.setMetadata(metadataBuilder.build().also { mediaMetadata = it })
				}, handler))
			}
			.excuse { e ->
				logger.warn(
					"There was an error updating the media session for `" + serviceFile.key + "`",
					e
				)
			}
	}

	override fun filePropertiesUpdated(updatedKey: UrlKeyHolder<ServiceFile>) {
		val (serviceFile, urlKey) = activeUrlKeyPair ?: return
		if (updatedKey == urlKey)
			updateNowPlaying(serviceFile)
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
	private fun clearClientBitmap() {
		if (remoteClientBitmap == null) return
		val metadataBuilder = MediaMetadataCompat.Builder(mediaMetadata)
		metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, null)
		mediaSession.setMetadata(metadataBuilder.build().also { mediaMetadata = it })
		remoteClientBitmap = null
	}

	companion object {
		private val logger by lazyLogger<MediaSessionBroadcaster>()
		private const val playbackSpeed = 1.0f

		private const val standardCapabilities = PlaybackStateCompat.ACTION_PLAY_PAUSE or
			PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
			PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
			PlaybackStateCompat.ACTION_STOP or
			PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
			PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
	}
}
