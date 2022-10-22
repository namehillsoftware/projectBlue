package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsStopped

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

private const val serviceFileId = 654

class WhenTheFileChanges : AndroidContext() {
	companion object {
		private val mediaSessionCompat = spyk(
			MediaSessionCompat(
				ApplicationProvider.getApplicationContext(),
				"donkey"
			)
		)
	}

	override fun before() {
		val playbackNotificationBroadcaster = MediaSessionBroadcaster(
			mockk {
				every { promiseNowPlaying() } returns NowPlaying(
					LibraryId(1),
					listOf(ServiceFile(serviceFileId)),
					0,
					0L,
					false
				).toPromise()
			},
			mockk {
				every { promiseFileProperties(ServiceFile(serviceFileId)) } returns mapOf(
					Pair(KnownFileProperties.Name, "kill"),
					Pair(KnownFileProperties.Rating, "861"),
					Pair(KnownFileProperties.Artist, "minister"),
					Pair(KnownFileProperties.Album, "vessel"),
					Pair(KnownFileProperties.Duration, "259267"),
					Pair(KnownFileProperties.Track, "919"),
				).toPromise()
			},
			mockk {
				every { promiseFileBitmap(ServiceFile(serviceFileId)) } returns BitmapFactory
					.decodeByteArray(byteArrayOf((912 % 128).toByte(), (368 % 128).toByte(), (395 % 128).toByte()), 0, 3)
					.toPromise()
			},
			mediaSessionCompat,
		)
		playbackNotificationBroadcaster.notifyPlaying()
		playbackNotificationBroadcaster.notifyPlayingFileUpdated()
		playbackNotificationBroadcaster.notifyStopped()
		playbackNotificationBroadcaster.notifyPlayingFileUpdated()
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
