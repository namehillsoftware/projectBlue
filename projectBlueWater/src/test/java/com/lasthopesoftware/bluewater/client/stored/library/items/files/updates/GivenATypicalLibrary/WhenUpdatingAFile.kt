package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.AndroidContextRunner
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidContextRunner::class)
class WhenUpdatingAFile : AndroidContext() {

	companion object {
		private var updatedStoredFile: StoredFile? = null
	}

	private val services by lazy {
		StoredFileAccess(ApplicationProvider.getApplicationContext())
	}

	override fun before() {
		val storedFile = services.promiseNewStoredFile(LibraryId(405), ServiceFile("936")).toExpiringFuture().get()!!

		updatedStoredFile = services
			.promiseUpdatedStoredFile(storedFile.setIsDownloadComplete(true).setUri("nXpBx"))
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then isDownloadComplete is recorded correctly`() {
		assertThat(updatedStoredFile?.isDownloadComplete).isTrue
	}

	@Test
	fun `then the URI is recorded correctly`() {
		assertThat(updatedStoredFile?.uri).isEqualTo("nXpBx")
	}
}
