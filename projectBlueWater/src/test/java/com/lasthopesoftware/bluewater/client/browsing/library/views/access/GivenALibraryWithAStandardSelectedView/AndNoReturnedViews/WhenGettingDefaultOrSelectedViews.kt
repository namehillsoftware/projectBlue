package com.lasthopesoftware.bluewater.client.browsing.library.views.access.GivenALibraryWithAStandardSelectedView.AndNoReturnedViews

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SavedLibraryRecordingStorage
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SelectedLibraryViewProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenGettingDefaultOrSelectedViews {

	private val libraryStorage = SavedLibraryRecordingStorage()
	private var selectedLibraryView: ViewItem? = null

	@BeforeAll
	fun before() {
		val selectedLibraryViewProvider = SelectedLibraryViewProvider(
			{ Promise(Library().setSelectedView(5)) },
			{ Promise<Collection<ViewItem>>(emptyList()) },
			libraryStorage
		)
		selectedLibraryView =
			selectedLibraryViewProvider.promiseSelectedOrDefaultView().toExpiringFuture().get()
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
