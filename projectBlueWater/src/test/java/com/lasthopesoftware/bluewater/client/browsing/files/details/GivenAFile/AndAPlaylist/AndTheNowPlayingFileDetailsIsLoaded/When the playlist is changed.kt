package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAFile.AndAPlaylist.AndTheNowPlayingFileDetailsIsLoaded

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.NowPlayingFileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.lasthopesoftware.bluewater.shared.observables.mapNotNull
import com.lasthopesoftware.bluewater.shared.observables.toCloseable
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When the playlist is changed` {
	companion object {
		private const val libraryId = 650
		private const val serviceFileId = "GRIP9Wza0sI"
	}

	private val mut by lazy {
		var activeLibraryId: LibraryId? = null
		val messageBus = RecordingApplicationMessageBus()
		Pair(
			NowPlayingFileDetailsViewModel(
				mockk(),
				mockk {
					every { load(LibraryId(libraryId), ServiceFile(serviceFileId)) } answers {
						activeLibraryId = firstArg()
						Unit.toPromise()
					}
				},
				mockk {
					every { this@mockk.activeLibraryId } answers {
						activeLibraryId
					}
					every { this@mockk.isLoading } returns MutableInteractionState(false)
				},
				mockk {
					every { promiseNowPlaying(LibraryId(libraryId)) } returns NowPlaying(
						libraryId = LibraryId(libraryId),
						playlist = listOf(
							ServiceFile("BuC68xY"),
							ServiceFile(serviceFileId),
							ServiceFile("EO822hl2ja")
						),
						playlistPosition = 0,
						filePosition = 0,
						isRepeating = false,
					).toPromise() andThen NowPlaying(
						libraryId = LibraryId(libraryId),
						playlist = emptyList(),
						playlistPosition = 0,
						filePosition = 0,
						isRepeating = false,
					).toPromise()
				},
				messageBus,
			),
			messageBus,
		)
	}

	private var isLoadingStates = mutableListOf<Boolean>()
	private var isInPositionStates = mutableListOf<Boolean>()

	@BeforeAll
	fun act() {
		val (viewModel, messageBus) = mut
		viewModel.isLoading.mapNotNull().subscribe(isLoadingStates::add).toCloseable().use {
			viewModel.isInPosition.mapNotNull().subscribe(isInPositionStates::add).toCloseable().use {
				viewModel
					.load(LibraryId(libraryId), PositionedFile(1, ServiceFile(serviceFileId)))
					.toExpiringFuture()
					.get()
				messageBus.sendMessage(LibraryPlaybackMessage.PlaylistChanged(LibraryId(libraryId)))
			}
		}
	}

	@Test
	fun `then is loading changes correctly`() {
		assertThat(isLoadingStates).isEqualTo(
			listOf(
				false,
				true,
				false,
				true,
				false
			)
		)
	}

	@Test
	fun `then the file is in position changes correctly`() {
		assertThat(isInPositionStates).isEqualTo(
			listOf(
				false,
				true,
				false,
			)
		)
	}
}
