package com.lasthopesoftware.bluewater.client.browsing.library.access.GivenASelectedLibrary

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRemoval
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectBrowserLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenRemovingTheLibrary {

	private val libraryId = LibraryId(id = 14)

	private val libraryRemoval by lazy {
		val fakeStoredItemAccess: AccessStoredItems = FakeStoredItemAccess(
			StoredItem(14, "1", StoredItem.ItemType.ITEM),
			StoredItem(1, "3", StoredItem.ItemType.ITEM),
			StoredItem(5, "2", StoredItem.ItemType.ITEM),
			StoredItem(14, "5", StoredItem.ItemType.ITEM)
		)
		val libraryStorage = mockk<ILibraryStorage>()
		every { libraryStorage.removeLibrary(libraryId) } returns Promise.empty()

		val libraryIdentifierProvider = mockk<ProvideSelectedLibraryId>()
		every { libraryIdentifierProvider.promiseSelectedLibraryId() } returns Promise(libraryId)

		val libraryProvider = FakeLibraryRepository(
			Library(id = libraryId.id),
			Library(id = 4),
			Library(id = 15)
		)

		val selectBrowserLibrary = mockk<SelectBrowserLibrary>()
		every { selectBrowserLibrary.selectBrowserLibrary(any()) } answers {
			libraryProvider.promiseLibrary(firstArg()).then { l ->
				selectedLibraryId = l?.libraryId
				l
			}
		}

		LibraryRemoval(
			fakeStoredItemAccess,
			libraryStorage,
			libraryIdentifierProvider,
			libraryProvider,
			selectBrowserLibrary
		)
	}

	private var selectedLibraryId: LibraryId? = null

	@BeforeAll
	fun act() {
		libraryRemoval.removeLibrary(libraryId).toExpiringFuture().get()
	}

	@Test
	fun thenTheFirstUnRemovedLibraryIsSelected() {
		assertThat(selectedLibraryId).isEqualTo(LibraryId(4))
	}
}
