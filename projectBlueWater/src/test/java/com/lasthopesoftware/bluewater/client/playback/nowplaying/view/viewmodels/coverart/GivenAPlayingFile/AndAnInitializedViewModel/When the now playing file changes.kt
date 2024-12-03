package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.coverart.GivenAPlayingFile.AndAnInitializedViewModel

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.observables.mapNotNull
import com.lasthopesoftware.bluewater.shared.observables.toCloseable
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class `When the now playing file changes` {
	companion object {
		private const val libraryId = 283
		private const val originalServiceFileId = 1000
		private const val newServiceFileId = 340
	}

	private val services by lazy {
		val messageBus = RecordingApplicationMessageBus()

		Pair(
			messageBus,
			NowPlayingCoverArtViewModel(
				messageBus,
				mockk {
					every { promiseNowPlaying(LibraryId(libraryId)) } returns Promise(
						NowPlaying(
							LibraryId(libraryId),
							listOf(
								ServiceFile(815),
								ServiceFile(449),
								ServiceFile(592),
								ServiceFile(originalServiceFileId),
								ServiceFile(390),
							),
							3,
							439774,
							false
						)
					) andThen Promise(
						NowPlaying(
							LibraryId(libraryId),
							listOf(
								ServiceFile(newServiceFileId),
							),
							0,
							834,
							false
						)
					)
				},
				mockk {
					every { promiseGuaranteedUrlKey(LibraryId(libraryId), any<ServiceFile>()) } answers {
						UrlKeyHolder(
							URL("http://molestiefelis"),
							lastArg<ServiceFile>()
						).toPromise()
					}
				},
				mockk {
					every { promiseImageBytes() } returns byteArrayOf(898.toByte(), 441.toByte(), 87.toByte(), 501.toByte()).toPromise()
				},
				mockk {
					every { promiseImageBytes(LibraryId(libraryId), ServiceFile(originalServiceFileId)) } returns byteArrayOf(629.toByte(), 122).toPromise()
					every { promiseImageBytes(LibraryId(libraryId), ServiceFile(newServiceFileId)) } returns byteArrayOf(731.toByte(), 729.toByte(), 313.toByte(), 426.toByte(), 381.toByte()).toPromise()
				},
				mockk(),
			)
		)
	}

	private val imageLoadingStates = mutableListOf<Boolean>()
	private val loadedImages = mutableListOf<ByteArray>()

	@BeforeAll
	fun act() {
		val (messageBus, viewModel) = services

		viewModel.isNowPlayingImageLoading.mapNotNull().subscribe(imageLoadingStates::add).toCloseable().use {
			viewModel.nowPlayingImage.mapNotNull().subscribe(loadedImages::add).toCloseable().use {
				viewModel.initializeViewModel(LibraryId(libraryId)).toExpiringFuture().get()
				messageBus.sendMessage(
					LibraryPlaybackMessage.TrackChanged(
						LibraryId(libraryId),
						PositionedFile(0, ServiceFile(newServiceFileId))
					)
				)

				// Test that new updates aren't sent when the message is sent twice with the same library/file
				messageBus.sendMessage(
					LibraryPlaybackMessage.TrackChanged(
						LibraryId(libraryId),
						PositionedFile(0, ServiceFile(newServiceFileId))
					)
				)
			}
		}
	}

	@Test
	fun `then the image loading states are correct`() {
		assertThat(imageLoadingStates).containsExactly(false, true, false, true, false)
	}

	@Test
	fun `then the loaded images are correct`() {
		assertThat(loadedImages).containsExactly(
			byteArrayOf(898.toByte(), 441.toByte(), 87.toByte(), 501.toByte()),
			byteArrayOf(629.toByte(), 122),
			byteArrayOf(898.toByte(), 441.toByte(), 87.toByte(), 501.toByte()),
			byteArrayOf(731.toByte(), 729.toByte(), 313.toByte(), 426.toByte(), 381.toByte()),
		)
	}
}
