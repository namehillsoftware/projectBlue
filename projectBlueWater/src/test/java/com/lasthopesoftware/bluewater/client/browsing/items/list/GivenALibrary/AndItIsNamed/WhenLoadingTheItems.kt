package com.lasthopesoftware.bluewater.client.browsing.items.list.GivenALibrary.AndItIsNamed

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 327

class WhenLoadingTheItems {
	private val viewModel by lazy {
		val itemProvider = mockk<ProvideItems>().apply {
			every { promiseItems(LibraryId(libraryId)) } returns listOf(
                Item(219),
                Item(418),
			).toPromise()
		}

        ItemListViewModel(
			itemProvider,
			RecordingApplicationMessageBus(),
			mockk {
				every { promiseLibrary(LibraryId(libraryId)) } returns Promise(
					Library(
						id = libraryId,
						connectionSettings = Json.encodeToString(
							StoredMediaCenterConnectionSettings(
								accessCode = "2WF95",
							)
						),
						libraryName = "shelter",
					)
				)
			},
		)
	}

	@BeforeAll
	fun act() {
		viewModel.loadItem(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the item value is correct`() {
		assertThat(viewModel.itemValue.value).isEqualTo("shelter")
	}

	@Test
	fun `then the view model is finished loading`() {
		assertThat(viewModel.isLoading.value).isFalse
	}

	@Test
	fun `then the loaded files are correct`() {
		assertThat(viewModel.items.value)
			.hasSameElementsAs(
				listOf(
					Item(219),
					Item(418),
				)
			)
	}
}
