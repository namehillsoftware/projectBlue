package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ScopedCachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.NotifyOfPlaybackEvents
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.NotifyOfTrackPositionUpdates
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.shared.android.MediaSession.ControlMediaSession
import com.lasthopesoftware.bluewater.shared.lazyLogger

private val logger by lazyLogger<MediaSessionBroadcaster>()
private const val playbackSpeed = 1.0f

private const val standardCapabilities = PlaybackStateCompat.ACTION_PLAY_PAUSE or
	PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
	PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
	PlaybackStateCompat.ACTION_STOP or
	PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
	PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH

class MediaSessionBroadcaster(
	private val nowPlayingProvider: GetNowPlayingState,
	private val scopedCachedFilePropertiesProvider: ScopedCachedFilePropertiesProvider,
	private val imageProvider: ProvideImages,
	private val mediaSession: ControlMediaSession
) : NotifyOfPlaybackEvents, NotifyOfTrackPositionUpdates {
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

	override fun notifyPlaying() {
		isPlaying = true
		val builder = PlaybackStateCompat.Builder()
		capabilities = PlaybackStateCompat.ACTION_PAUSE or standardCapabilities
		builder.setActions(capabilities)
		playbackState = PlaybackStateCompat.STATE_PLAYING
		builder.setState(playbackState, trackPosition, playbackSpeed)
		mediaSession.setPlaybackState(builder.build())
	}

	override fun notifyPaused() {
		isPlaying = false
		val builder = PlaybackStateCompat.Builder()
		capabilities = PlaybackState.ACTION_PLAY or standardCapabilities
		builder.setActions(capabilities)
		playbackState = PlaybackState.STATE_PAUSED
		builder.setState(playbackState, trackPosition, playbackSpeed)
		mediaSession.setPlaybackState(builder.build())
	}

	override fun notifyStopped() {
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

	override fun notifyInterrupted() {
		notifyPaused()
	}

	override fun notifyPlayingFileUpdated() {
		nowPlayingProvider
			.promiseNowPlaying()
			.then {
				it?.playingFile?.serviceFile?.also(::updateNowPlaying)
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
	private fun clearClientBitmap() {
		if (remoteClientBitmap == null) return
		val metadataBuilder = MediaMetadataCompat.Builder(mediaMetadata)
		metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, null)
		mediaSession.setMetadata(metadataBuilder.build().also { mediaMetadata = it })
		remoteClientBitmap = null
	}

	private fun updateNowPlaying(serviceFile: ServiceFile) {
		val promisedBitmap = imageProvider.promiseFileBitmap(serviceFile)

		scopedCachedFilePropertiesProvider
			.promiseFileProperties(serviceFile)
			.eventually { fileProperties ->
				val artist = fileProperties[KnownFileProperties.Artist]
				val name = fileProperties[KnownFileProperties.Name]
				val album = fileProperties[KnownFileProperties.Album]
				val duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties)

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

				promisedBitmap.then { bitmap ->
					if (remoteClientBitmap != bitmap) {
						metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)
						remoteClientBitmap = bitmap
					}
					mediaSession.setMetadata(metadataBuilder.build().also { mediaMetadata = it })
				}
			}
			.excuse { e -> logger.warn("There was an error updating the media session for `${serviceFile.key}`", e) }
	}
}
