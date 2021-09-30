package com.lasthopesoftware.bluewater.client.browsing.library.views.access.GivenALibraryWithoutSelectedViews

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library.ViewType
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
	fun thenTheSelectedViewKeyIsSaved() {
		assertThat(libraryStorage.savedLibrary!!.selectedView)
			.isEqualTo(expectedView.key)
	}

	@Test
	fun thenTheSelectedViewTypeIsStandard() {
		assertThat(libraryStorage.savedLibrary!!.selectedViewType)
			.isEqualTo(ViewType.StandardServerView)
	}

	companion object {
		private val expectedView = StandardViewItem(2, null)
		private val libraryStorage = SavedLibraryRecordingStorage()
		private var selectedLibraryView: ViewItem? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val selectedLibraryViewProvider = SelectedLibraryViewProvider(
				{ Promise(Library()) },
				{
					Promise(
						listOf(
							StandardViewItem(2, null),
							StandardViewItem(1, null),
							StandardViewItem(14, null)
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
