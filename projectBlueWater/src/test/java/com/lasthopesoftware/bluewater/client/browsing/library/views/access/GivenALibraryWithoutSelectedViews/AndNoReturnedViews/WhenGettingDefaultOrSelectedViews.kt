package com.lasthopesoftware.bluewater.client.browsing.library.views.access.GivenALibraryWithoutSelectedViews.AndNoReturnedViews

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.ProvideLibraryViews
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SavedLibraryRecordingStorage
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SelectedLibraryViewProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.ExecutionException

class WhenGettingDefaultOrSelectedViews {
    @Test
    fun thenNoSelectedViewIsReturned() {
        Assertions.assertThat(selectedLibraryView).isNull()
    }

    @Test
    fun thenTheLibraryIsNotSaved() {
        Assertions.assertThat(libraryStorage.savedLibrary).isNull()
    }

    companion object {
        private val libraryStorage = SavedLibraryRecordingStorage()
        private var selectedLibraryView: Item? = null
        @BeforeClass
        @Throws(ExecutionException::class, InterruptedException::class)
        fun before() {
            val selectedLibraryViewProvider = SelectedLibraryViewProvider(
                { Promise(Library()) },
                ProvideLibraryViews { Promise<Collection<ViewItem>>(emptyList()) },
                libraryStorage
            )
            selectedLibraryView =
                FuturePromise(selectedLibraryViewProvider.promiseSelectedOrDefaultView()).get()
        }
    }
}
