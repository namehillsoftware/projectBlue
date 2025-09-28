package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AfterPlaybackHasStopped

import android.media.MediaMetadata
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.MappedFilePropertiesLookup
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
		private const val libraryId = 923
		private const val serviceFileId = "404"

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
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns MappedFilePropertiesLookup(mapOf(
					Pair(NormalizedFileProperties.Name, "wing"),
					Pair(NormalizedFileProperties.Rating, "861"),
					Pair(NormalizedFileProperties.Artist, "toe"),
					Pair(NormalizedFileProperties.Album, "paint"),
					Pair(NormalizedFileProperties.Duration, "618"),
					Pair(NormalizedFileProperties.Track, "723"),
				)).toPromise() andThen MappedFilePropertiesLookup(mapOf(
					Pair(NormalizedFileProperties.Name, "deep"),
					Pair(NormalizedFileProperties.Rating, "861"),
					Pair(NormalizedFileProperties.Artist, "hut"),
					Pair(NormalizedFileProperties.Album, "self"),
					Pair(NormalizedFileProperties.Duration, "97"),
					Pair(NormalizedFileProperties.Track, "340"),
				)).toPromise()
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
