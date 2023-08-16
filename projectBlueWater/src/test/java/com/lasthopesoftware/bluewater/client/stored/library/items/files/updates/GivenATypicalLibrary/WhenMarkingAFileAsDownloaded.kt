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
class WhenMarkingAFileAsDownloaded : AndroidContext() {

	companion object {
		private var updatedStoredFile: StoredFile? = null
	}

	private val services by lazy {
		StoredFileAccess(ApplicationProvider.getApplicationContext())
	}

	override fun before() {
		val storedFile = services.promiseNewStoredFile(LibraryId(405), ServiceFile(936)).toExpiringFuture().get()!!

		updatedStoredFile = services
			.markStoredFileAsDownloaded(storedFile)
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then the change is recorded correctly`() {
		assertThat(updatedStoredFile?.isDownloadComplete).isTrue
	}
}
