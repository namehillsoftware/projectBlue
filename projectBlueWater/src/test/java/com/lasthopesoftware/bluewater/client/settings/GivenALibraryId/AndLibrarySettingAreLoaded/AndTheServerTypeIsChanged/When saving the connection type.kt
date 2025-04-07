package com.lasthopesoftware.bluewater.client.settings.GivenALibraryId.AndLibrarySettingAreLoaded.AndTheServerTypeIsChanged

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.settings.ServerTypeSelectionViewModel
import com.lasthopesoftware.bluewater.shared.observables.mapNotNull
import com.lasthopesoftware.bluewater.shared.observables.toCloseable
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When saving the connection type` {

	companion object {
		private const val libraryId = 474
	}

	private val mutt by lazy {
		val libraryRepository = FakeLibraryRepository(
			Library(
				id = libraryId,
				connectionSettings = "HVLJOQZODyf",
				serverType = Library.ServerType.Subsonic
			)
		)

		Pair(
			libraryRepository,
			ServerTypeSelectionViewModel(
				libraryRepository,
			)
		)
	}

	private val library
		get() = mutt.first.libraries[libraryId]

	private var isChangedStates = mutableListOf<Boolean>()
	private var isLoadingStates = mutableListOf<Boolean>()

	@BeforeAll
	fun act() {
		val (_, repository) = mutt

		repository.isLoading.mapNotNull().subscribe(isLoadingStates::add).toCloseable().use {
			repository.isChanged.mapNotNull().subscribe(isChangedStates::add).toCloseable().use {
				repository.promiseLoadedConnectionType(LibraryId(libraryId)).toExpiringFuture().get()
				repository.serverType.value = Library.ServerType.MediaCenter
				repository.promiseSavedConnectionType().toExpiringFuture().get()
			}
		}
	}

	@Test
	fun `then the is loading states are correct`() {
		assertThat(isLoadingStates).containsExactly(false, true, false, true, false)
	}

	@Test
	fun `then the is changed states are correct`() {
		assertThat(isChangedStates).containsExactly(true, false, true, false)
	}

	@Test
	fun `then the library is updated`() {
		assertThat(library).isEqualTo(
			Library(
				id = libraryId,
				connectionSettings = null,
				serverType = Library.ServerType.MediaCenter
			)
		)
	}
}
