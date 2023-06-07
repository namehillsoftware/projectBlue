package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsPaused.ThenStartedAgain

import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.FakeNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.MediaSessionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.singleNowPlaying
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Test

private const val libraryId = 452
private const val serviceFileId = 654

class WhenTheFileChanges : AndroidContext() {

	companion object {
		private var playbackStates: MutableList<PlaybackStateCompat>? = ArrayList()
		private var mediaMetadata: MutableList<MediaMetadataCompat>? = ArrayList()

		@AfterClass
		@JvmStatic
		fun after() {
			playbackStates = null
			mediaMetadata = null
		}
	}

	override fun before() {
		val messageBus = RecordingApplicationMessageBus()
		val nowPlaying = singleNowPlaying(LibraryId(libraryId), ServiceFile(serviceFileId))
		val playbackNotificationBroadcaster = MediaSessionBroadcaster(
			FakeNowPlayingRepository(nowPlaying),
            mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns mapOf(
					Pair(KnownFileProperties.Name, "monkey"),
					Pair(KnownFileProperties.Rating, "140"),
					Pair(KnownFileProperties.Artist, "season"),
					Pair(KnownFileProperties.Album, "nobody"),
					Pair(KnownFileProperties.Duration, "688.502"),
					Pair(KnownFileProperties.Track, "355"),
				).toPromise()
			},
			mockk {
				every { promiseFileBitmap(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns BitmapFactory
					.decodeByteArray(byteArrayOf((912 % 128).toByte(), (368 % 128).toByte(), (395 % 128).toByte()), 0, 3)
					.toPromise()
			},
			mockk {
				every { setPlaybackState(any()) } answers {
					playbackStates?.add(firstArg())
				}

				every { setMetadata(any()) } answers {
					mediaMetadata?.add(firstArg())
				}
			},
			messageBus
		)

		with(messageBus) {
			sendMessage(PlaybackMessage.PlaybackStarted)
			sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
			sendMessage(PlaybackMessage.PlaybackPaused)
			sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
			sendMessage(PlaybackMessage.PlaybackStarted)
		}
	}

	@Test
	fun `then the playback states are correct`() {
		assertThat(playbackStates?.map { s -> s.state }).containsExactly(
			PlaybackStateCompat.STATE_PLAYING,
			PlaybackStateCompat.STATE_PAUSED,
			PlaybackStateCompat.STATE_PLAYING,
		)
	}

	@Test
	fun `then the media metadata is correct`() {
		assertThat(mediaMetadata).allMatch { m ->
			m.description.title == "monkey" &&
				m.getString(MediaMetadata.METADATA_KEY_ARTIST) == "season" &&
				m.getString(MediaMetadata.METADATA_KEY_ALBUM) == "nobody" &&
				m.getLong(MediaMetadata.METADATA_KEY_DURATION) == (688.502 * 1000).toLong() &&
				m.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER) == 355L &&
				m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null
		}
	}
}
