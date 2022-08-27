package com.lasthopesoftware.bluewater.client.browsing.library.views.access.GivenASelectedSearchView

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library.ViewType
import com.lasthopesoftware.bluewater.client.browsing.library.views.PlaylistViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.SearchViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.StandardViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SavedLibraryRecordingStorage
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SelectedLibraryViewProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenGettingDefaultOrSelectedViews {

	private val expectedView = SearchViewItem()
	private val libraryStorage = SavedLibraryRecordingStorage()
	private var selectedLibraryView: ViewItem? = null

	@BeforeAll
	fun before() {
		val selectedLibraryViewProvider = SelectedLibraryViewProvider(
			{
				Promise(
					Library().setSelectedView(8).setSelectedViewType(ViewType.SearchView)
				)
			},
			{
				Promise(
					listOf(
						StandardViewItem(3, null),
						StandardViewItem(5, null),
						PlaylistViewItem(8)
					)
				)
			},
			libraryStorage
		)
		selectedLibraryView =
			selectedLibraryViewProvider.promiseSelectedOrDefaultView().toExpiringFuture().get()
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
