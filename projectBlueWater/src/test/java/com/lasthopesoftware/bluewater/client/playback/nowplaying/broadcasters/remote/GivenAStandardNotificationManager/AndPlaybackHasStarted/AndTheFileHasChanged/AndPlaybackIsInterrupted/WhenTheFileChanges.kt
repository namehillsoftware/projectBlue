package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsInterrupted

import android.media.MediaMetadata
import android.support.v4.media.MediaMetadataCompat
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
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.bitmaps.ImmediateBitmapProducer
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Test

class WhenTheFileChanges : AndroidContext() {

	companion object {
		private const val libraryId = 412
		private const val serviceFileId = "143"

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
			mockk {
				every { setPlaybackState(any()) } answers {
					playbackStates?.add(firstArg())
				}

				every { setMetadata(any()) } answers {
					mediaMetadata?.add(firstArg())
				}
			},
			messageBus,
		)

		with(messageBus) {
			sendMessage(PlaybackMessage.PlaybackStarted)
			sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
			sendMessage(PlaybackMessage.PlaybackInterrupted)
			sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
		}
	}

	@Test
	fun `then the playback states are correct`() {
		assertThat(playbackStates?.map { s -> s.state }).containsExactly(
			PlaybackStateCompat.STATE_PLAYING,
			PlaybackStateCompat.STATE_PAUSED,
		)
	}

	@Test
	fun `then the first media metadata is correct`() {
		assertThat(mediaMetadata?.first()).matches { m ->
			m?.description?.title == "kill" &&
				m.getString(MediaMetadata.METADATA_KEY_ARTIST) == "minister" &&
				m.getString(MediaMetadata.METADATA_KEY_ALBUM) == "vessel" &&
				m.getLong(MediaMetadata.METADATA_KEY_DURATION) == 259267L * 1000 &&
				m.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER) == 919L &&
				m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null
		}
	}

	@Test
	fun `then the second media metadata is correct`() {
		assertThat(mediaMetadata?.drop(1)?.first()).matches { m ->
			m?.description?.title == "break" &&
				m.getString(MediaMetadata.METADATA_KEY_ARTIST) == "picture" &&
				m.getString(MediaMetadata.METADATA_KEY_ALBUM) == "student" &&
				m.getLong(MediaMetadata.METADATA_KEY_DURATION) == 594909L * 1000 &&
				m.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER) == 337L &&
				m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null
		}
	}
}
