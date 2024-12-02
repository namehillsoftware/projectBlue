package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AfterPlaybackHasStopped

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
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Test

private const val libraryId = 923
private const val serviceFileId = 404

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
		MediaSessionBroadcaster(
			FakeNowPlayingRepository(nowPlaying),
			mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns mapOf(
					Pair(KnownFileProperties.Name, "wing"),
					Pair(KnownFileProperties.Rating, "861"),
					Pair(KnownFileProperties.Artist, "toe"),
					Pair(KnownFileProperties.Album, "paint"),
					Pair(KnownFileProperties.Duration, "618"),
					Pair(KnownFileProperties.Track, "723"),
				).toPromise() andThen mapOf(
					Pair(KnownFileProperties.Name, "deep"),
					Pair(KnownFileProperties.Rating, "861"),
					Pair(KnownFileProperties.Artist, "hut"),
					Pair(KnownFileProperties.Album, "self"),
					Pair(KnownFileProperties.Duration, "97"),
					Pair(KnownFileProperties.Track, "340"),
				).toPromise()
			},
			mockk {
				every { promiseImageBytes(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns byteArrayOf((912).toByte(), (368).toByte(), (395).toByte()).toPromise()
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
			sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
			sendMessage(PlaybackMessage.PlaybackStopped)
		}
	}

	@Test
	fun `then the playback states are correct`() {
		assertThat(playbackStates?.map { s -> s.state }).containsExactly(
			PlaybackStateCompat.STATE_PLAYING,
			PlaybackStateCompat.STATE_STOPPED,
		)
	}

	@Test
	fun `then the first media metadata is correct`() {
		assertThat(mediaMetadata?.first()).matches { m ->
			m?.description?.title == "wing" &&
				m.getString(MediaMetadata.METADATA_KEY_ARTIST) == "toe" &&
				m.getString(MediaMetadata.METADATA_KEY_ALBUM) == "paint" &&
				m.getLong(MediaMetadata.METADATA_KEY_DURATION) == 618L * 1000 &&
				m.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER) == 723L &&
				m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null
		}
	}

	@Test
	fun `then the second media metadata is correct`() {
		assertThat(mediaMetadata?.drop(1)?.first()).matches { m ->
			m?.description?.title == "deep" &&
				m.getString(MediaMetadata.METADATA_KEY_ARTIST) == "hut" &&
				m.getString(MediaMetadata.METADATA_KEY_ALBUM) == "self" &&
				m.getLong(MediaMetadata.METADATA_KEY_DURATION) == 97L * 1000 &&
				m.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER) == 340L &&
				m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null
		}
	}
}
