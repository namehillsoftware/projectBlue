package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.fileproperties.GivenAMissingUrlKey

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyNotReturnedException
import com.lasthopesoftware.bluewater.client.connection.session.LibraryConnectionChangedMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CancellationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

private const val libraryId = 590
private const val serviceFileId = 385

class WhenInitializingTheNowPlayingFilePropertiesViewModel {

	private val mut by lazy {
		var isConnectionChangedMessageSent = false

		val messageBus = spyk<RecordingApplicationMessageBus> {
			every { sendMessage(any<LibraryConnectionChangedMessage>()) } answers {
				isConnectionChangedMessageSent = true
				callOriginal()
			}
		}

		val nowPlayingViewModel = NowPlayingFilePropertiesViewModel(
			messageBus,
			mockk {
				every { promiseNowPlaying(LibraryId(libraryId)) } returns Promise(
					NowPlaying(
						LibraryId(libraryId),
						listOf(
							ServiceFile(815),
							ServiceFile(449),
							ServiceFile(592),
							ServiceFile(serviceFileId),
							ServiceFile(390),
						),
						3,
						439774,
						false
					)
				)
			},
			mockk {
				every {
					promiseFileProperties(
						LibraryId(libraryId),
						ServiceFile(serviceFileId)
					)
				} returns Promise(emptyMap())
			},
            mockk {
                every { promiseGuaranteedUrlKey(LibraryId(libraryId), any<ServiceFile>()) } answers {
					if (isConnectionChangedMessageSent) Promise(UrlKeyHolder(URL("http://plan"), lastArg()))
					else Promise(UrlKeyNotReturnedException(firstArg(), lastArg()))
				}
            },
            mockk(),
			mockk {
				every { promiseIsReadOnly(LibraryId(libraryId)) } returns true.toPromise()
			},
			mockk {
				every { promiseIsMarkedForPlay(LibraryId(libraryId)) } returns true.toPromise()
			},
			mockk {
				every { pollConnection(LibraryId(libraryId)) } returns Promise(CancellationException("reject"))
			},
			mockk(relaxed = true),
        )

		Pair(messageBus, nowPlayingViewModel)
	}

	@BeforeAll
	fun act() {
		val (bus, vm) = mut

		vm.initializeViewModel(LibraryId(libraryId)).toExpiringFuture().get()

		bus.sendMessage(LibraryConnectionChangedMessage(LibraryId(libraryId)))
	}

	@Test
	fun `then the file position is correct`() {
		assertThat(mut.second.filePosition.value).isEqualTo(439774)
	}
}
