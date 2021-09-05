package com.lasthopesoftware.bluewater.client.browsing.library.access.GivenASelectedLibrary

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRemoval
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectBrowserLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenRemovingTheLibrary {

    companion object {
        private var selectedLibraryId: LibraryId? = null

		@BeforeClass
		@JvmStatic
        fun before() {
            val library = Library()
            library.setId(14)
            val fakeStoredItemAccess: IStoredItemAccess = FakeStoredItemAccess(
                StoredItem(14, 1, StoredItem.ItemType.ITEM),
                StoredItem(1, 3, StoredItem.ItemType.ITEM),
                StoredItem(5, 2, StoredItem.ItemType.ITEM),
                StoredItem(14, 5, StoredItem.ItemType.ITEM)
            )
            val libraryStorage = mockk<ILibraryStorage>()
            every { libraryStorage.removeLibrary(library) } returns Promise.empty()

			val libraryIdentifierProvider = mockk<ProvideSelectedLibraryId>()
			every { libraryIdentifierProvider.selectedLibraryId } returns Promise(library.libraryId)

            val libraryProvider = FakeLibraryProvider(
                library,
                Library().setId(4),
                Library().setId(15)
            )

			val selectBrowserLibrary = mockk<SelectBrowserLibrary>()
			every { selectBrowserLibrary.selectBrowserLibrary(any()) } answers {
				libraryProvider.getLibrary(firstArg()).then { l ->
					selectedLibraryId = l?.libraryId
					l
				}
			}

            val libraryRemoval = LibraryRemoval(
                fakeStoredItemAccess,
                libraryStorage,
                libraryIdentifierProvider,
                libraryProvider,
				selectBrowserLibrary
			)
			libraryRemoval.removeLibrary(library).toFuture().get()
        }
    }

	@Test
	fun thenTheFirstUnRemovedLibraryIsSelected() {
		assertThat(selectedLibraryId).isEqualTo(LibraryId(4))
	}
}
