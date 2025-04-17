package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAPlaylist

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When loading the files` {

	companion object {
		private const val playlistId = "d34a253e-e43f-49d3-b3d4-6c87f2201b54"
		private const val libraryId = 516
	}

	private val viewModel by lazy {
		val storedItemAccess = mockk<AccessStoredItems>().apply {
			every { isItemMarkedForSync(any(), any<Item>()) } returns false.toPromise()
		}

		FileListViewModel(
			mockk {
				every { promiseFiles(LibraryId(libraryId), PlaylistId(playlistId)) } returns listOf(
					ServiceFile("5e"),
					ServiceFile("a5"),
					ServiceFile("90"),
					ServiceFile("6f"),
					ServiceFile("024b27ba-6a50-4ee2-978f-920ae02d7603"),
				).toPromise()
			},
            storedItemAccess,
		)
	}

	@BeforeAll
	fun act() {
		viewModel.loadItem(LibraryId(libraryId), Playlist(playlistId, "Quamphasellus")).toExpiringFuture().get()
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
		assertThat(viewModel.files.value)
			.hasSameElementsAs(
				listOf(
					ServiceFile("5e"),
					ServiceFile("a5"),
					ServiceFile("90"),
					ServiceFile("6f"),
					ServiceFile("024b27ba-6a50-4ee2-978f-920ae02d7603"),
				)
			)
	}
}
