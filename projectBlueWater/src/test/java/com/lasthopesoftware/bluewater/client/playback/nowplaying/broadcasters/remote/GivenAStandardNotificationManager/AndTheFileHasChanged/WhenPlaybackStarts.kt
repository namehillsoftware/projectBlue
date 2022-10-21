package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager.AndTheFileHasChanged

import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.MediaSessionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test

class WhenPlaybackStarts : AndroidContext() {
	companion object {
		private val mediaSessionCompat = spyk(
			MediaSessionCompat(
				ApplicationProvider.getApplicationContext(),
				"test"
			)
		)
	}

	override fun before() {
		val playbackNotificationBroadcaster =
            MediaSessionBroadcaster(
				mockk {
					every { promiseNowPlaying() } returns NowPlaying(
						LibraryId(1),
						listOf(ServiceFile(559)),
						0,
						0L,
						false
					).toPromise()
				},
				mockk {
					every { promiseFileProperties(ServiceFile(559)) } returns mapOf(
						Pair(KnownFileProperties.Name, "leaf"),
						Pair(KnownFileProperties.Rating, "895"),
						Pair(KnownFileProperties.Artist, "worry"),
						Pair(KnownFileProperties.Album, "screw"),
						Pair(KnownFileProperties.Duration, "247346"),
						Pair(KnownFileProperties.Track, "622"),
					).toPromise()
				},
				mockk {
					every { promiseFileBitmap(ServiceFile(559)) } returns BitmapFactory
						.decodeByteArray(byteArrayOf((912 % 128).toByte(), (368 % 128).toByte(), (395 % 128).toByte()), 0, 3)
						.toPromise()
				},
				mediaSessionCompat,
            )
		playbackNotificationBroadcaster.notifyPlayingFileUpdated()
		playbackNotificationBroadcaster.notifyPlaying()
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
