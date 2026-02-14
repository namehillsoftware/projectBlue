package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged

import android.app.PendingIntent
import android.media.MediaMetadata
import android.support.v4.media.session.PlaybackStateCompat
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.MappedFilePropertiesLookup
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.FakeNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.MediaSessionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.singleNowPlaying
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.android.MediaSession.ControlMediaSession
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.bitmaps.ImmediateBitmapProducer
import com.lasthopesoftware.resources.closables.thenUse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WhenPlaybackIsPaused : AndroidContext() {

	companion object {
		private const val libraryId = 324
		private const val serviceFileId = "654"

		private val mediaSessionCompat = mockk<ControlMediaSession>(relaxUnitFun = true)
		private val expectedIntent = mockk<PendingIntent>()
	}

	override fun before() {
		val messageBus = RecordingApplicationMessageBus()
		val nowPlaying = singleNowPlaying(LibraryId(libraryId), ServiceFile(serviceFileId))
		MediaSessionBroadcaster(
			FakeNowPlayingRepository(nowPlaying),
            mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns MappedFilePropertiesLookup(mapOf(
					Pair(NormalizedFileProperties.Name, "kill"),
					Pair(NormalizedFileProperties.Rating, "861"),
					Pair(NormalizedFileProperties.Artist, "minister"),
					Pair(NormalizedFileProperties.Album, "vessel"),
					Pair(NormalizedFileProperties.Duration, "259267"),
					Pair(NormalizedFileProperties.Track, "919"),
				)).toPromise()
			},
			mockk {
				every { promiseImageBytes(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns byteArrayOf((912).toByte(), (368).toByte(), (395).toByte()).toPromise()
			},
			ImmediateBitmapProducer,
			mediaSessionCompat,
			FakeSelectedLibraryIdProvider(LibraryId(libraryId)),
			mockk {
				every { buildPendingNowPlayingIntent(LibraryId(libraryId)) } returns expectedIntent
			},
			messageBus
		).thenUse {
			messageBus.sendMessage(PlaybackMessage.PlaybackStarted)
			messageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
			messageBus.sendMessage(PlaybackMessage.PlaybackPaused)
		}.toExpiringFuture().get()
	}

	@Test
	fun `then the state is set to playing`() {
		verify { mediaSessionCompat.setPlaybackState(match { c -> c.state == PlaybackStateCompat.STATE_PLAYING }) }
	}

	@Test
	fun `then the state transitions to paused`() {
		verify { mediaSessionCompat.setPlaybackState(match { c -> c.state == PlaybackStateCompat.STATE_PAUSED }) }
	}

	@Test
	fun `then the metadata is correct`() {
		verify {
			mediaSessionCompat.setMetadata(match { m ->
				m.description.title == "kill" &&
					m.getString(MediaMetadata.METADATA_KEY_ARTIST) == "minister" &&
					m.getString(MediaMetadata.METADATA_KEY_ALBUM) == "vessel" &&
					m.getLong(MediaMetadata.METADATA_KEY_DURATION) == 259267L * 1000 &&
					m.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER) == 919L
			})
		}
	}

	@Test
	fun `then the session activity is correctly set up`() {
		verify {
			mediaSessionCompat.setSessionActivity(expectedIntent)
		}
	}

	@Test
	fun `then the session activity is deactivated`() {
		verify {
			mediaSessionCompat.deactivate()
		}
	}
}
