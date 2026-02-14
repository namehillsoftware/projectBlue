package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndPlaybackIsStopped.AndStartedAgain

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
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.bitmaps.ImmediateBitmapProducer
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenTheFileChanges : AndroidContext() {
	companion object {
		private const val libraryId = 201
		private const val serviceFileId = "DCReKcHj2c"

		private var isActivatedInitially = false
		private var isActivatedAgainAfterStarting = false
		private var isDeactivatedAfterBeingActivated = false
		private var isActivatedAfterBeingDeactivated = false
		private var playbackStates = ArrayList<PlaybackStateCompat>()
	}

	override fun before() {
		val messageBus = RecordingApplicationMessageBus()
		val nowPlaying = singleNowPlaying(LibraryId(libraryId), ServiceFile(serviceFileId))
		var isStarting = false
		MediaSessionBroadcaster(
            FakeNowPlayingRepository(nowPlaying),
            mockk {
                every {
                    promiseFileProperties(
                        LibraryId(libraryId),
                        ServiceFile(serviceFileId)
                    )
                } returns MappedFilePropertiesLookup(
                    mapOf(
                        Pair(NormalizedFileProperties.Name, "Nequerutrum"),
                        Pair(NormalizedFileProperties.Rating, "146"),
                        Pair(NormalizedFileProperties.Artist, "Pharetraorci"),
                        Pair(NormalizedFileProperties.Album, "Auctorporta"),
                        Pair(NormalizedFileProperties.Duration, "8017338"),
                        Pair(NormalizedFileProperties.Track, "216"),
                    )
                ).toPromise()
            },
            mockk {
                every {
                    promiseImageBytes(
                        LibraryId(libraryId),
                        ServiceFile(serviceFileId)
                    )
                } returns byteArrayOf((923).toByte(), (368).toByte(), (395).toByte()).toPromise()
            },
            ImmediateBitmapProducer,
            mockk(relaxUnitFun = true) {
				every { activate() } answers {
					isActivatedInitially = true
					isActivatedAgainAfterStarting = isStarting
					isActivatedAfterBeingDeactivated = isDeactivatedAfterBeingActivated
				}

				every { deactivate() } answers {
					isDeactivatedAfterBeingActivated = isActivatedAgainAfterStarting
				}

				every { setPlaybackState(any()) } answers {
					playbackStates.add(firstArg())
				}
			},
			FakeSelectedLibraryIdProvider(LibraryId(libraryId)),
            mockk(),
            messageBus,
        )

		with(messageBus) {
			isStarting = true
			sendMessage(PlaybackMessage.PlaybackStarting)
			sendMessage(PlaybackMessage.PlaybackStopped)
			sendMessage(PlaybackMessage.PlaybackStarting)
		}
	}

	@Test
	fun `then the playback states are correct`() {
		assertThat(playbackStates.map { it.state }).isEqualTo(
			listOf(
				PlaybackStateCompat.STATE_PLAYING,
				PlaybackStateCompat.STATE_STOPPED,
				PlaybackStateCompat.STATE_PLAYING
			)
		)
	}

	@Test
	fun `then the session is initially activated`() {
        assertThat(isActivatedInitially).isTrue
	}

	@Test
	fun `then the session is activated again after starting`() {
        assertThat(isActivatedAgainAfterStarting).isTrue
	}

	@Test
	fun `then the session is deactivated after starting`() {
        assertThat(isDeactivatedAfterBeingActivated).isTrue
	}

	@Test
	fun `then the session is activated again after being deactivated`() {
        assertThat(isActivatedAfterBeingDeactivated).isTrue
	}
}
