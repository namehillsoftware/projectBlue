package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.NowPlayingFileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
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

class `When removing the file from now playing` {

	companion object {
		private const val libraryId = 360
		private const val serviceFileId = "oPtcghYMF8N"
	}

	private var removedLibraryId: LibraryId? = null
	private var removedPosition = 0

	private val viewModel by lazy {
		var activeLibraryId: LibraryId? = null
		NowPlayingFileDetailsViewModel(
			mockk {
				every { removeFromPlaylistAtPosition(any(), any()) } answers {
					removedLibraryId = firstArg()
					removedPosition = lastArg()
				}
			},
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
				every { isLoading } returns MutableInteractionState(true)
			},
			mockk {
				every { promiseNowPlaying(LibraryId(libraryId)) } returns NowPlaying(
					libraryId = LibraryId(libraryId),
					playlist = emptyList(),
					playlistPosition = 0,
					filePosition = 0,
					isRepeating = false,
				).toPromise()
			},
			RecordingApplicationMessageBus(),
		)
	}

	private var isLoadingStates = mutableListOf<Boolean>()
	private var isInPositionStates = mutableListOf<Boolean>()

	@BeforeAll
	fun act() {
		viewModel.isLoading.mapNotNull().subscribe(isLoadingStates::add).toCloseable().use {
			viewModel.isInPosition.mapNotNull().subscribe(isInPositionStates::add).toCloseable().use {
				viewModel
					.load(LibraryId(libraryId), PositionedFile(501, ServiceFile(serviceFileId)))
					.toExpiringFuture()
					.get()
				viewModel.removeFile()
			}
		}
	}

	@Test
	fun `then is loading changes correctly`() {
		assertThat(isLoadingStates).isEqualTo(listOf(true))
	}

	@Test
	fun `then the file is in position changes correctly`() {
		assertThat(isInPositionStates).isEqualTo(listOf(false))
	}

	@Test
	fun `then the file is removed with the correct library id`() {
		assertThat(removedLibraryId).isEqualTo(LibraryId(libraryId))
	}

	@Test
	fun `then the file is removed to now playing`() {
		assertThat(removedPosition).isEqualTo(501)
	}
}
