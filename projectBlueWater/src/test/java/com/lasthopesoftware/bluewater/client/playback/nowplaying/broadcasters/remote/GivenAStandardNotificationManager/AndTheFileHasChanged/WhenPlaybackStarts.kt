package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager.AndTheFileHasChanged

import android.media.MediaMetadata
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
import com.lasthopesoftware.bluewater.shared.android.MediaSession.ControlMediaSession
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.bitmaps.ImmediateBitmapProducer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WhenPlaybackStarts : AndroidContext() {
	companion object {
		private const val libraryId = 746
		private const val serviceFileId = "150"

		private val mediaSessionCompat = mockk<ControlMediaSession>(relaxUnitFun = true)
	}

	override fun before() {
		val recordingApplicationMessageBus = RecordingApplicationMessageBus()
		MediaSessionBroadcaster(
			FakeNowPlayingRepository(singleNowPlaying(LibraryId(libraryId), ServiceFile(serviceFileId))),
			mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns mapOf(
					Pair(NormalizedFileProperties.Name, "leaf"),
					Pair(NormalizedFileProperties.Rating, "895"),
					Pair(NormalizedFileProperties.Artist, "worry"),
					Pair(NormalizedFileProperties.Album, "screw"),
					Pair(NormalizedFileProperties.Duration, "247346"),
					Pair(NormalizedFileProperties.Track, "622"),
				).toPromise()
			},
			mockk {
				every { promiseImageBytes(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns byteArrayOf((912).toByte(), (368).toByte(), (395).toByte()).toPromise()
			},
			ImmediateBitmapProducer,
			mediaSessionCompat,
			recordingApplicationMessageBus,
		)

		recordingApplicationMessageBus.sendMessage(
			LibraryPlaybackMessage.TrackChanged(
				LibraryId(libraryId),
				PositionedFile(0, ServiceFile(serviceFileId))
			)
		)
		recordingApplicationMessageBus.sendMessage(PlaybackMessage.PlaybackStarted)
	}

	@Test
	fun `then the state is set to playing`() {
		verify { mediaSessionCompat.setPlaybackState(match { c -> c.state == PlaybackStateCompat.STATE_PLAYING }) }
	}

	@Test
	fun `then the metadata is correct`() {
		verify {
			mediaSessionCompat.setMetadata(match { m ->
				m.description.title == "leaf" &&
					m.getString(MediaMetadata.METADATA_KEY_ARTIST) == "worry" &&
					m.getString(MediaMetadata.METADATA_KEY_ALBUM) == "screw" &&
					m.getLong(MediaMetadata.METADATA_KEY_DURATION) == 247346L * 1000 &&
				m.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER) == 622L
			})
		}
	}
}
