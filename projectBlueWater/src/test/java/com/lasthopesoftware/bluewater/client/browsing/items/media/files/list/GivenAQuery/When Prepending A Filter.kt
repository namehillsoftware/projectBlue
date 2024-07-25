package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.GivenAQuery

import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Prepending A Filter` {
	private val viewModel by lazy {
		val libraryFileProvider = mockk<ProvideLibraryFiles>()

		val vm = SearchFilesViewModel(
			libraryFileProvider,
		)

		vm
	}

	@BeforeAll
	fun act() {
		viewModel.query.value = "T8Dtib8Rh"
		viewModel.prependFilter(FileProperty("FhFuGYramwv", "YVUf8Q83"))
	}

	@Test
	fun `then the query is correct`() {
		assertThat(viewModel.query.value).isEqualTo("[FhFuGYramwv]=YVUf8Q83 T8Dtib8Rh")
	}
}
