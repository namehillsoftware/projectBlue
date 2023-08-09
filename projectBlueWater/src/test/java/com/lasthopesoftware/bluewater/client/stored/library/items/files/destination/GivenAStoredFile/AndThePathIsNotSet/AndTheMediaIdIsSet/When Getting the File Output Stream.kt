package com.lasthopesoftware.bluewater.client.stored.library.items.files.destination.GivenAStoredFile.AndThePathIsNotSet.AndTheMediaIdIsSet

import android.content.ContentUris
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileUriDestinationBuilder
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.resources.uri.MediaCollections
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
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
				mockk {
					val expectedContentUri = ContentUris.withAppendedId(MediaCollections.ExternalAudio, storedMediaId)
					every { openFileDescriptor(expectedContentUri, "r") } returns null
					every { openOutputStream(expectedContentUri) } returns ByteArrayOutputStream()
				}
			)
		}

		private var outputStream: OutputStream? = null

		@BeforeClass
		@JvmStatic
		fun act() {
			val storedFile = StoredFile(LibraryId(1), storedMediaId.toInt(), ServiceFile(1), null, true)

			outputStream = mut.getOutputStream(storedFile)
		}
	}

	@Test
	fun `then the output stream is correct`() {
		assertThat(outputStream).isNotNull
	}
}
