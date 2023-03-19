package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.GivenAQuery

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 983
private const val query = "YH4ILJ"

class WhenFindingFiles {
	private val loadingStates = ArrayList<Boolean>()

	private val viewModel by lazy {
		val libraryFileProvider = mockk<ProvideLibraryFiles>()

		val vm = SearchFilesViewModel(
			libraryFileProvider,
            mockk(),
		)

		every { libraryFileProvider.promiseFiles(LibraryId(libraryId), FileListParameters.Options.None, "Files/Search", "Query=[Media Type]=[Audio] $query") } answers {
			isLoadingBeforeQueriesMade = vm.isLoading.value
			loadingStates.add(vm.isLoading.value)
			LibraryId(libraryId).toPromise()

			Promise(
				listOf(
					ServiceFile(209),
					ServiceFile(792),
					ServiceFile(61),
					ServiceFile(637),
					ServiceFile(948),
					ServiceFile(349),
					ServiceFile(459),
					ServiceFile(747),
					ServiceFile(922),
					ServiceFile(713),
					ServiceFile(617),
					ServiceFile(249),
				)
			)
		}

		vm
	}

	private var isLoadingBeforeQueriesMade = false

	@BeforeAll
	fun act() {
		viewModel.setActiveLibraryId(LibraryId(libraryId))
		viewModel.query.value = query
		loadingStates.add(viewModel.isLoading.value)
		viewModel.findFiles().toExpiringFuture().get()
		loadingStates.add(viewModel.isLoading.value)
	}

	@Test fun `then a library ID is active`() {
		assertThat(viewModel.isLibraryIdActive.value).isTrue
	}

	@Test fun `then loading is true before the query is made`() {
		assertThat(isLoadingBeforeQueriesMade).isTrue
	}

	@Test fun `then the loading states are correct`() {
		assertThat(loadingStates).hasSameElementsAs(listOf(false, true, false))
	}

	@Test fun `then the results are correct`() {
		assertThat(viewModel.files.value).hasSameElementsAs(
			listOf(
				ServiceFile(209),
				ServiceFile(792),
				ServiceFile(61),
				ServiceFile(637),
				ServiceFile(948),
				ServiceFile(349),
				ServiceFile(459),
				ServiceFile(747),
				ServiceFile(922),
				ServiceFile(713),
				ServiceFile(617),
				ServiceFile(249),
			)
		)
	}
}
