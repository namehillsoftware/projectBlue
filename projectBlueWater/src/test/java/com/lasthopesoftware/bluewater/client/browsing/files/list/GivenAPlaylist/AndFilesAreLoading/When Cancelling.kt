package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAPlaylist.AndFilesAreLoading

import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When loading the files` {

	companion object {
		private const val playlistId = "55"
		private const val libraryId = 298
	}

	private val viewModel by lazy {
		FileListViewModel(
			mockk {
				every { promiseFiles(LibraryId(libraryId), PlaylistId(playlistId)) } returns Promise {
					it.awaitCancellation {
						it.sendResolution(emptyList())
					}
				}
			},
			mockk {
				every { isItemMarkedForSync(any(), any<Item>()) } returns false.toPromise()
			},
		)
	}

	@BeforeAll
	fun act() {
		val promisedLoad = viewModel.loadItem(LibraryId(libraryId), Playlist(playlistId, "Quamphasellus"))
		promisedLoad.cancel()
		promisedLoad.toExpiringFuture().get()
	}

	@Test
	fun thenTheItemIsNotMarkedForSync() {
		assertThat(viewModel.isSynced.value).isFalse
	}

	@Test
	fun thenTheItemValueIsCorrect() {
		assertThat(viewModel.itemValue.value).isEqualTo("Quamphasellus")
	}

	@Test
	fun `then is loaded is correct`() {
		assertThat(viewModel.isLoading.value).isFalse
	}

	@Test
	fun thenTheLoadedFilesAreCorrect() {
		assertThat(viewModel.files.value).isEmpty()
	}
}
