package com.lasthopesoftware.bluewater.client.browsing.library.views.access.GivenALibraryWithAStandardSelectedView

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.views.StandardViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SavedLibraryRecordingStorage
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SelectedLibraryViewProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenGettingDefaultOrSelectedViews {
	@Test
	fun thenTheSelectedViewsAreCorrect() {
		assertThat(selectedLibraryView).isEqualTo(expectedView)
	}

	@Test
	fun thenTheLibraryIsNotSaved() {
		assertThat(libraryStorage.savedLibrary).isNull()
	}

	companion object {
		private val libraryStorage = SavedLibraryRecordingStorage()
		private val expectedView = StandardViewItem(5, null)
		private var selectedLibraryView: ViewItem? = null

		@BeforeClass
		@JvmStatic
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
				FuturePromise(selectedLibraryViewProvider.promiseSelectedOrDefaultView()).get()
		}
	}
}