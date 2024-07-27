package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.GivenAQuery.AndFilesHaveBeenFound

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.observables.toCloseable
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 983
private const val query = "YH4ILJ"

class `When Clearing the Query` {
	private val viewModel by lazy {
		val libraryFileProvider = mockk<ProvideLibraryFiles>()

		val vm = SearchFilesViewModel(
			libraryFileProvider,
		)

		every { libraryFileProvider.promiseFiles(LibraryId(libraryId), FileListParameters.Options.None, "Files/Search", "Query=[Media Type]=[Audio] $query") } answers {
			isLoadingBeforeQueriesMade = vm.isLoading.value
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

	private val loadingStates = ArrayList<Boolean>()
	private var isLoadingBeforeQueriesMade = false
	private lateinit var searchResults: List<ServiceFile>

	@BeforeAll
	fun act() {
		viewModel.isLoading.subscribe { s -> loadingStates.add(s.value) }.toCloseable().use {
			viewModel.setActiveLibraryId(LibraryId(libraryId))
			viewModel.query.value = query
			viewModel.findFiles().toExpiringFuture().get()
			searchResults = viewModel.files.value
			viewModel.clearResults()
		}
	}

	@Test
	fun `then a library ID is active`() {
		assertThat(viewModel.isLibraryIdActive.value).isTrue
	}

	@Test
	fun `then loading is true before the query is made`() {
		assertThat(isLoadingBeforeQueriesMade).isTrue
	}

	@Test
	fun `then the loading states are correct`() {
		assertThat(loadingStates).hasSameElementsAs(listOf(false, true, false))
	}

	@Test
	fun `then the results are correct`() {
		assertThat(searchResults).hasSameElementsAs(
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

	@Test
	fun `then the list is cleared`() {
		assertThat(viewModel.files.value).isEmpty()
	}

	@Test
	fun `then the query is cleared`() {
		assertThat(viewModel.query.value).isEmpty()
	}
}
