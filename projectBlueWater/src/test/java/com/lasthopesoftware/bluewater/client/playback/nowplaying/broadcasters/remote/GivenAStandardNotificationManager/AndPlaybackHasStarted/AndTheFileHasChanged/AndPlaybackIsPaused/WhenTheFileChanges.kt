package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsPaused

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
		private const val libraryId = 142
		private const val serviceFileId = "617"

		private val mediaSessionCompat = mockk<ControlMediaSession>(relaxUnitFun = true)
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
				).toPromise() andThen mapOf(
					Pair(NormalizedFileProperties.Name, "break"),
					Pair(NormalizedFileProperties.Rating, "963"),
					Pair(NormalizedFileProperties.Artist, "picture"),
					Pair(NormalizedFileProperties.Album, "student"),
					Pair(NormalizedFileProperties.Duration, "594909"),
					Pair(NormalizedFileProperties.Track, "337"),
				).toPromise()
			},
			mockk {
				every { promiseImageBytes(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns byteArrayOf((912).toByte(), (368).toByte(), (395).toByte()).toPromise()
			},
			ImmediateBitmapProducer,
			mediaSessionCompat,
			messageBus,
		)


		with(messageBus) {
			sendMessage(PlaybackMessage.PlaybackStarted)
			sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
			sendMessage(PlaybackMessage.PlaybackPaused)
			sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
		}
	}

	@Test
	fun `then the state is set to playing`() {
		verify(exactly = 1) { mediaSessionCompat.setPlaybackState(match { c -> c.state == PlaybackStateCompat.STATE_PLAYING }) }
	}

	@Test
	fun `then the state transitions to paused`() {
		verify(exactly = 1) { mediaSessionCompat.setPlaybackState(match { c -> c.state == PlaybackStateCompat.STATE_PAUSED }) }
	}

	@Test
	fun `then metadata with images is fired correctly`() {
		verify(exactly = 1) {
			mediaSessionCompat.setMetadata(match { m ->
				m.description.title == "kill" &&
					m.getString(MediaMetadata.METADATA_KEY_ARTIST) == "minister" &&
					m.getString(MediaMetadata.METADATA_KEY_ALBUM) == "vessel" &&
					m.getLong(MediaMetadata.METADATA_KEY_DURATION) == 259267L * 1000 &&
					m.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER) == 919L &&
					m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null &&
					m.getBitmap(MediaMetadata.METADATA_KEY_ART) != null &&
					m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON) != null &&
					m.getRating(MediaMetadata.METADATA_KEY_USER_RATING).starRating == 5f
			})
		}
	}

	@Test
	fun `then metadata without images is fired correctly`() {
		verify(exactly = 1) {
			mediaSessionCompat.setMetadata(match { m ->
				m.description.title == "break" &&
					m.getString(MediaMetadata.METADATA_KEY_ARTIST) == "picture" &&
					m.getString(MediaMetadata.METADATA_KEY_ALBUM) == "student" &&
					m.getLong(MediaMetadata.METADATA_KEY_DURATION) == 594909L * 1000 &&
					m.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER) == 337L &&
					m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null &&
					m.getBitmap(MediaMetadata.METADATA_KEY_ART) != null &&
					m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON) != null &&
					m.getRating(MediaMetadata.METADATA_KEY_USER_RATING).starRating == 5f
			})
		}
	}
}
