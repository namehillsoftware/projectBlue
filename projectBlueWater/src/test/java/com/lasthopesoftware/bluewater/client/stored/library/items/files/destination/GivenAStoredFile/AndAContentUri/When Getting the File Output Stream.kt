package com.lasthopesoftware.bluewater.client.stored.library.items.files.destination.GivenAStoredFile.AndAContentUri

import android.content.ContentUris
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileUriDestinationBuilder
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.uri.MediaCollections
import com.lasthopesoftware.resources.uri.toURI
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.OutputStream

private const val storedMediaId = 62L

@RunWith(AndroidJUnit4::class)
class `When Getting the File Output Stream` {

	companion object {
		private val mut by lazy {
            StoredFileUriDestinationBuilder(
				mockk(),
                mockk(),
                mockk {
                    val expectedContentUri =
                        ContentUris.withAppendedId(MediaCollections.ExternalAudio, storedMediaId)
                    every { openFileDescriptor(expectedContentUri, "r") } returns null
                    every { openOutputStream(expectedContentUri, "wt") } returns ByteArrayOutputStream()
                }
            )
		}

		private var outputStream: OutputStream? = null

		@BeforeClass
		@JvmStatic
		fun act() {
			val storedFile = StoredFile(
                LibraryId(1),
                ServiceFile(1),
                ContentUris.withAppendedId(MediaCollections.ExternalAudio, storedMediaId).toURI(),
                true
            )

			outputStream = mut.promiseOutputStream(storedFile).toExpiringFuture().get()
		}
	}

	@Test
	fun `then the output stream is correct`() {
		Assertions.assertThat(outputStream).isNotNull
	}
}
