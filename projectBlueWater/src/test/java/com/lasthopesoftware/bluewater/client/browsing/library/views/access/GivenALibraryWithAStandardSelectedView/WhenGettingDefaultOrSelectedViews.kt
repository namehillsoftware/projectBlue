package com.lasthopesoftware.bluewater.client.browsing.library.views.access.GivenALibraryWithAStandardSelectedView

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.views.StandardViewItem
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
	private val expectedView = StandardViewItem(5, null)
	private var selectedLibraryView: ViewItem? = null

	@BeforeAll
	fun before() {
		val selectedLibraryViewProvider = SelectedLibraryViewProvider(
			{ Promise(Library().setSelectedView(5)) },
			{
				Promise(
					listOf(
						StandardViewItem(3, null),
						StandardViewItem(5, null),
						StandardViewItem(8, null)
					)
				)
			},
			libraryStorage
		)
		selectedLibraryView =
			ExpiringFuturePromise(selectedLibraryViewProvider.promiseSelectedOrDefaultView()).get()
	}

	@Test
	fun thenTheSelectedViewsAreCorrect() {
		assertThat(selectedLibraryView).isEqualTo(expectedView)
	}

	@Test
	fun thenTheLibraryIsNotSaved() {
		assertThat(libraryStorage.savedLibrary).isNull()
	}
}
