package com.lasthopesoftware.bluewater.client.browsing.library.views.access.GivenALibraryWithoutSelectedViews.AndNoReturnedViews

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SavedLibraryRecordingStorage
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SelectedLibraryViewProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenGettingDefaultOrSelectedViews {

	private val libraryStorage = SavedLibraryRecordingStorage()
	private var selectedLibraryView: Item? = null

	@BeforeAll
	fun act() {
		val selectedLibraryViewProvider = SelectedLibraryViewProvider(
			{ Promise(Library()) },
			{ Promise<Collection<ViewItem>>(emptyList()) },
			libraryStorage
		)
		selectedLibraryView =
			ExpiringFuturePromise(selectedLibraryViewProvider.promiseSelectedOrDefaultView()).get()
	}

    @Test
    fun thenNoSelectedViewIsReturned() {
        assertThat(selectedLibraryView).isNull()
    }

    @Test
    fun thenTheLibraryIsNotSaved() {
        assertThat(libraryStorage.savedLibrary).isNull()
    }
}
