package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsInterrupted.ThenStartedAgain

import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.MediaSessionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Test

private const val serviceFileId = 769

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
					Pair(KnownFileProperties.Name, "wing"),
					Pair(KnownFileProperties.Rating, "861"),
					Pair(KnownFileProperties.Artist, "toe"),
					Pair(KnownFileProperties.Album, "paint"),
					Pair(KnownFileProperties.Duration, "618"),
					Pair(KnownFileProperties.Track, "723"),
				).toPromise()
			},
			mockk {
				every { promiseFileBitmap(ServiceFile(serviceFileId)) } returns BitmapFactory
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
		)

		with(playbackNotificationBroadcaster) {
			notifyPlaying()
			notifyPlayingFileUpdated()
			notifyInterrupted()
			notifyPlayingFileUpdated()
			notifyPlaying()
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
			m.description.title == "wing" &&
				m.getString(MediaMetadata.METADATA_KEY_ARTIST) == "toe" &&
				m.getString(MediaMetadata.METADATA_KEY_ALBUM) == "paint" &&
				m.getLong(MediaMetadata.METADATA_KEY_DURATION) == 618L * 1000 &&
				m.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER) == 723L &&
				m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null
		}
	}
}
