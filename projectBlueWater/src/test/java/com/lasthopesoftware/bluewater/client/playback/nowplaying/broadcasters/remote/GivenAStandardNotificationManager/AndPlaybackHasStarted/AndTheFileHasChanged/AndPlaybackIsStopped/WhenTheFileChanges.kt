package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsStopped

import android.media.MediaMetadata
import android.support.v4.media.session.PlaybackStateCompat
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.FakeNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.MediaSessionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.singleNowPlaying
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.android.MediaSession.ControlMediaSession
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.bitmaps.ImmediateBitmapProducer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WhenTheFileChanges : AndroidContext() {
	companion object {
		private const val libraryId = 152
		private const val serviceFileId = "667"

		private val mediaSessionCompat by lazy { mockk<ControlMediaSession>(relaxUnitFun = true) }
	}

	override fun before() {
		val messageBus = RecordingApplicationMessageBus()
		val nowPlaying = singleNowPlaying(LibraryId(libraryId), ServiceFile(serviceFileId))
		MediaSessionBroadcaster(
			FakeNowPlayingRepository(nowPlaying),
            mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns mapOf(
					Pair(NormalizedFileProperties.Name, "kill"),
					Pair(NormalizedFileProperties.Rating, "861"),
					Pair(NormalizedFileProperties.Artist, "minister"),
					Pair(NormalizedFileProperties.Album, "vessel"),
					Pair(NormalizedFileProperties.Duration, "259267"),
					Pair(NormalizedFileProperties.Track, "919"),
				).toPromise()
			},
			mockk {
				every { promiseImageBytes(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns byteArrayOf((912).toByte(), (368).toByte(), (395).toByte()).toPromise()
			},
			ImmediateBitmapProducer,
			mediaSessionCompat,
			messageBus,
		).use {
			with(messageBus) {
				sendMessage(PlaybackMessage.PlaybackStarted)
				sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
				sendMessage(PlaybackMessage.PlaybackStopped)
				sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
			}
		}
	}

	@Test
	fun `then the state is set to playing`() {
		verify { mediaSessionCompat.setPlaybackState(match { c -> c.state == PlaybackStateCompat.STATE_PLAYING }) }
	}

	@Test
	fun `then the state transitions to stopped`() {
		verify { mediaSessionCompat.setPlaybackState(match { c -> c.state == PlaybackStateCompat.STATE_STOPPED }) }
	}

	@Test
	fun `then metadata with images is fired correctly`() {
		verify(exactly = 2) {
			mediaSessionCompat.setMetadata(match { m ->
				m.description.title == "kill" &&
					m.getString(MediaMetadata.METADATA_KEY_ARTIST) == "minister" &&
					m.getString(MediaMetadata.METADATA_KEY_ALBUM) == "vessel" &&
					m.getLong(MediaMetadata.METADATA_KEY_DURATION) == 259267L * 1000 &&
					m.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER) == 919L &&
					m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null
			})
		}
	}

	@Test
	fun `then metadata without images is fired correctly`() {
		verify(exactly = 1) {
			mediaSessionCompat.setMetadata(match { m ->
				m.description.title == "kill" &&
					m.getString(MediaMetadata.METADATA_KEY_ARTIST) == "minister" &&
					m.getString(MediaMetadata.METADATA_KEY_ALBUM) == "vessel" &&
					m.getLong(MediaMetadata.METADATA_KEY_DURATION) == 259267L * 1000 &&
					m.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER) == 919L &&
					m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) == null
			})
		}
	}
}
