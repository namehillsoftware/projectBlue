package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager

import android.media.MediaMetadata
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.FakeNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.MediaSessionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.singleNowPlaying
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.shared.android.MediaSession.ControlMediaSession
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

private const val libraryId = 162
private const val serviceFile = 559

class WhenTheFileChanges : AndroidContext() {
	companion object {
		private val mediaSessionCompat = mockk<ControlMediaSession>(relaxUnitFun = true)
	}

	override fun before() {
		val messageBus = RecordingApplicationMessageBus()
		MediaSessionBroadcaster(
			FakeNowPlayingRepository(singleNowPlaying(LibraryId(libraryId), ServiceFile(serviceFile))),
            mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFile)) } returns mapOf(
					Pair(KnownFileProperties.Name, "wise"),
					Pair(KnownFileProperties.Rating, "707"),
					Pair(KnownFileProperties.Artist, "dark"),
					Pair(KnownFileProperties.Album, "brighten"),
					Pair(KnownFileProperties.Duration, "699392"),
					Pair(KnownFileProperties.Track, "687"),
				).toPromise()
			},
			mockk {
				every { promiseImageBytes(LibraryId(libraryId), ServiceFile(serviceFile)) } returns byteArrayOf((912).toByte(), (368).toByte(), (395).toByte()).toPromise()
			},
			mediaSessionCompat,
			messageBus,
		)
		messageBus.sendMessage(
			LibraryPlaybackMessage.TrackChanged(
				LibraryId(libraryId),
				PositionedFile(0, ServiceFile(serviceFile)),
			)
		)
	}

	@Test
	fun `then the metadata is correct`() {
		verify {
			mediaSessionCompat.setMetadata(match { m ->
				m.description.title == "wise" &&
					m.getString(MediaMetadata.METADATA_KEY_ARTIST) == "dark" &&
					m.getString(MediaMetadata.METADATA_KEY_ALBUM) == "brighten" &&
					m.getLong(MediaMetadata.METADATA_KEY_DURATION) == 699392L * 1000 &&
					m.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER) == 687L
			})
		}
	}
}
