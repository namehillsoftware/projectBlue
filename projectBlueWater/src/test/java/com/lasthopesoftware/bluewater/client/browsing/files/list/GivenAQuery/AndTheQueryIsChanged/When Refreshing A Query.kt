package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAQuery.AndTheQueryIsChanged

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Refreshing A Query` {
	companion object {
		private const val libraryId = 783
	}

	private val viewModel by lazy {
		val vm = SearchFilesViewModel(
            mockk {
                every {
                    promiseFiles(
                        LibraryId(libraryId),
                        FileListParameters.Options.None,
                        "Files/Search",
                        "Query=[Media Type]=[Audio] qlJJKMs"
                    )
                } returnsMany listOf(
                    Promise(
                        listOf(
                            ServiceFile(426),
                            ServiceFile(445),
                        )
                    ),
                    Promise(
                        listOf(
                            ServiceFile(736),
                            ServiceFile(100),
                            ServiceFile(115),
                            ServiceFile(732),
                        )
                    ),
                )
            },
        )

		vm
	}

	@BeforeAll
	fun act() {
		viewModel.setActiveLibraryId(LibraryId(libraryId))
		viewModel.query.value = "qlJJKMs"
		viewModel.findFiles().toExpiringFuture().get()
		viewModel.query.value = "XziNQ1gUX"
		viewModel.promiseRefresh().toExpiringFuture().get()
	}

	@Test
	fun `then the query is correct`() {
		assertThat(viewModel.query.value).isEqualTo("XziNQ1gUX")
	}

	@Test
    fun `then the results are correct`() {
		assertThat(viewModel.files.value).hasSameElementsAs(
			listOf(
                ServiceFile(736),
                ServiceFile(100),
                ServiceFile(115),
                ServiceFile(732),
			)
		)
	}
}
