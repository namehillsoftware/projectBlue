package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote

import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.durationInMs
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.PlaybackNotificationRouter
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.bluewater.shared.android.MediaSession.ControlMediaSession
import com.lasthopesoftware.bluewater.shared.images.bytes.GetImageBytes
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.resources.emptyByteArray

private val logger by lazyLogger<MediaSessionBroadcaster>()
private const val playbackSpeed = 1.0f

private const val standardCapabilities = PlaybackStateCompat.ACTION_PLAY_PAUSE or
	PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
	PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
	PlaybackStateCompat.ACTION_STOP or
	PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
	PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH

class MediaSessionBroadcaster(
    private val nowPlayingState: GetNowPlayingState,
    private val filePropertiesProvider: ProvideLibraryFileProperties,
    private val imageProvider: GetImageBytes,
    private val mediaSession: ControlMediaSession,
    applicationMessages: RegisterForApplicationMessages,
) : PlaybackNotificationRouter(applicationMessages) {

	private val trackPositionUpdatesSubscription = applicationMessages.registerReceiver { m: TrackPositionUpdate ->
		updateTrackPosition(m.filePosition.millis)
	}

	@Volatile
	private var playbackState = PlaybackStateCompat.STATE_STOPPED

	@Volatile
	private var trackPosition: Long = -1

	@Volatile
	private var mediaMetadata = MediaMetadataCompat.Builder().build()

	@Volatile
	private var capabilities = standardCapabilities
	private var remoteClientBitmap: ByteArray = emptyByteArray

	@Volatile
	private var isPlaying = false

	override fun close() {
		trackPositionUpdatesSubscription.close()
		clearClientBitmap()
		super.close()
	}

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
	}

	override fun notifyInterrupted() {
		notifyPaused()
	}

	override fun notifyPlayingFileUpdated() {
		nowPlayingState.promiseActiveNowPlaying().then { np ->
			np?.playingFile?.serviceFile?.also { sf -> updateNowPlaying(np.libraryId, sf) }
		}
	}

	private fun updateTrackPosition(trackPosition: Long) {
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
		if (remoteClientBitmap.isEmpty()) return
		val metadataBuilder = MediaMetadataCompat.Builder(mediaMetadata)
		metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, null)
		mediaSession.setMetadata(metadataBuilder.build().also { mediaMetadata = it })
		remoteClientBitmap = emptyByteArray
	}

	private fun updateNowPlaying(libraryId: LibraryId, serviceFile: ServiceFile) {
		val promisedBitmap = imageProvider.promiseImageBytes(libraryId, serviceFile)

		filePropertiesProvider
			.promiseFileProperties(libraryId, serviceFile)
			.eventually { fileProperties ->
				val artist = fileProperties[KnownFileProperties.Artist]
				val name = fileProperties[KnownFileProperties.Name]
				val album = fileProperties[KnownFileProperties.Album]
				val duration = fileProperties.durationInMs ?: -1

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
					if (remoteClientBitmap !== bitmap) {
						metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeByteArray(bitmap, 0, bitmap.size))
						remoteClientBitmap = bitmap
					}
					mediaSession.setMetadata(metadataBuilder.build().also { mediaMetadata = it })
				}
			}
			.excuse { e -> logger.warn("There was an error updating the media session for `$serviceFile`", e) }
	}
}
