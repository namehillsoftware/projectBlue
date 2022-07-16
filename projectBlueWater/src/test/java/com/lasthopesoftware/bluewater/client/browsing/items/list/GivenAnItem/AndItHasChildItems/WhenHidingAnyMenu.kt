package com.lasthopesoftware.bluewater.client.browsing.items.list.GivenAnItem.AndItHasChildItems

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

private const val libraryId = 471
private const val rootItemId = 437
private const val childItemId = 567

private val viewModel by lazy {
	val selectedLibraryIdProvider = mockk<ProvideSelectedLibraryId>().apply {
		every { selectedLibraryId } returns LibraryId(libraryId).toPromise()
	}

	val itemProvider = mockk<ProvideItems>().apply {
		every { promiseItems(LibraryId(libraryId), ItemId(rootItemId)) } returns listOf(
			Item(childItemId),
			Item(55),
			Item(137),
			Item(766),
			Item(812),
		).toPromise()
	}

	val storedItemAccess = FakeStoredItemAccess()

	ItemListViewModel(
		selectedLibraryIdProvider,
		itemProvider,
		mockk(relaxed = true, relaxUnitFun = true),
		storedItemAccess,
		mockk(),
		mockk(),
	)
}

class WhenHidingAnyMenu {

	companion object {
		private var isMenuHidden = false

		@BeforeClass
		@JvmStatic
		fun act() {
			viewModel.loadItem(Item(rootItemId, "leaf")).toExpiringFuture().get()
			isMenuHidden = viewModel.hideAnyShownMenus()
		}
	}

	@Test
	fun `then it is indicated that a menu was NOT hidden`() {
		assertThat(isMenuHidden).isFalse
	}

	@Test
	fun `then no menu is shown`() {
		assertThat(viewModel.items.value.any { it.isMenuShown.value }).isFalse
	}

	@Test
	fun thenTheItemValueIsCorrect() {
		assertThat(viewModel.itemValue.value).isEqualTo("leaf")
	}

	@Test
	fun thenIsLoadedIsTrue() {
		assertThat(viewModel.isLoaded.value).isTrue
	}

	@Test
	fun thenTheLoadedFilesAreCorrect() {
		assertThat(viewModel.items.value.map { it.item })
			.hasSameElementsAs(
				listOf(
					Item(55),
					Item(childItemId),
					Item(137),
					Item(766),
					Item(812),
				)
			)
	}
}
