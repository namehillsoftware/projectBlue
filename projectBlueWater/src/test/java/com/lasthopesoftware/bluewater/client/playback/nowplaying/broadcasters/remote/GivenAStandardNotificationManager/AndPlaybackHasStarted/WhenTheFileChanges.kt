package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager.AndPlaybackHasStarted

import android.media.MediaMetadata
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.FakeNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.MediaSessionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.singleNowPlaying
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.bitmaps.ImmediateBitmapProducer
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.AfterClass
import org.junit.Test

class WhenTheFileChanges : AndroidContext() {
	companion object {
		private const val libraryId = 480
		private const val serviceFileId = "303"

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
		val recordingApplicationMessageBus = RecordingApplicationMessageBus()
		MediaSessionBroadcaster(
			FakeNowPlayingRepository(singleNowPlaying(LibraryId(libraryId), ServiceFile(serviceFileId))),
            mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns mapOf(
					Pair(NormalizedFileProperties.Name, "stiff"),
					Pair(NormalizedFileProperties.Rating, "72"),
					Pair(NormalizedFileProperties.Artist, "shower"),
					Pair(NormalizedFileProperties.Album, "however"),
					Pair(NormalizedFileProperties.Duration, "182280915"),
					Pair(NormalizedFileProperties.Track, "921"),
				).toPromise()
			},
			mockk {
				every { promiseImageBytes(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns byteArrayOf((912).toByte(), (368).toByte(), (395).toByte()).toPromise()
			},
			ImmediateBitmapProducer,
			mockk {
				every { setPlaybackState(any()) } answers { playbackStates?.add(firstArg()) }

				every { setMetadata(any()) } answers { mediaMetadata?.add(firstArg()) }
			},
			recordingApplicationMessageBus
		)
		recordingApplicationMessageBus.sendMessage(PlaybackMessage.PlaybackStarted)
		recordingApplicationMessageBus.sendMessage(TrackPositionUpdate(Duration.standardSeconds(614), Duration.standardSeconds(799)))
		recordingApplicationMessageBus.sendMessage(
			LibraryPlaybackMessage.TrackChanged(
				LibraryId(libraryId),
				PositionedFile(0, ServiceFile(serviceFileId)),
			)
		)
	}

	@Test
	fun `then the state is set to playing`() {
		assertThat(playbackStates).allMatch { c -> c.state == PlaybackStateCompat.STATE_PLAYING }
	}

	@Test
	fun `then the state has the correct file positions`() {
		assertThat(playbackStates?.map { it.position }).containsExactly(0, 614000, 0)
	}

	@Test
	fun `then the metadata is correct`() {
		assertThat(mediaMetadata).anyMatch { m ->
			m.description.title == "stiff" &&
				m.getString(MediaMetadata.METADATA_KEY_ARTIST) == "shower" &&
				m.getString(MediaMetadata.METADATA_KEY_ALBUM) == "however" &&
				m.getLong(MediaMetadata.METADATA_KEY_DURATION) == 182280915L * 1000 &&
				m.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER) == 921L
		}
	}
}
