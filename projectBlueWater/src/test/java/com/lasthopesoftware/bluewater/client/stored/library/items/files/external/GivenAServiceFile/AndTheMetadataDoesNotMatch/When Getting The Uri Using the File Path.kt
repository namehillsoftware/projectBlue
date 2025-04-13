package com.lasthopesoftware.bluewater.client.stored.library.items.files.external.GivenAServiceFile.AndTheMetadataDoesNotMatch

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.DataFileUriProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.uri.MediaCollections
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class `When Getting The Uri Using the File Path` : AndroidContext() {
	companion object {
		private const val libraryId = 895
		private const val serviceFileId = "194"

		private val uriProvider by lazy {
            DataFileUriProvider(
                mockk {
                    every {
                        promiseFileProperties(
                            LibraryId(libraryId),
                            ServiceFile(serviceFileId)
                        )
                    } returns Promise(
                        mapOf(
                            Pair(
                                NormalizedFileProperties.Filename,
                                "N4CVqBLbUx. 4WwF4s4E.txt;geop7SLBFQ"
                            ),
                            Pair(NormalizedFileProperties.Album, "DHgcnUk08jl"),
                            Pair(NormalizedFileProperties.Artist, "Nasceturaugue"),
                        )
                    )
                },
                mockk {
                    every { isReadPermissionGranted } returns true
                    every { isReadMediaAudioPermissionGranted } returns true
                },
                mockk {
                    every {
                        query(
                            any(),
                            any(),
                            any(),
                            any(),
                            any()
                        )
                    } returns mockk<Cursor>(relaxUnitFun = true) {
                        every { moveToFirst() } returns false
                    }

                    every {
                        query(
                            MediaCollections.ExternalAudio,
                            arrayOf(
                                MediaStore.Audio.Media._ID,
                                MediaStore.Audio.Media.DISPLAY_NAME
                            ),
                            "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?",
                            arrayOf("%Nasceturaugue/DHgcnUk08jl/N4CVqBLbUx. 4WwF4s4E/%geop7SLBFQ%."),
                            null
                        )
                    } returns mockk<Cursor>(relaxUnitFun = true) {
                        every { moveToFirst() } returns true
                        every { getColumnIndexOrThrow(MediaStore.Audio.Media._ID) } returns 835
                        every { isNull(835) } returns false
                        every { getLong(835) } returns 234L
                    }

                    every {
                        openFileDescriptor(
                            ContentUris.withAppendedId(
                                MediaCollections.ExternalAudio,
                                234
                            ), "r"
                        )
                    } returns mockk(relaxUnitFun = true)
                }
            )
		}
	}

	private var uri: Uri? = null

	override fun before() {
		uri = uriProvider.promiseUri(LibraryId(libraryId), ServiceFile(serviceFileId)).toExpiringFuture().get()
	}

	@Test
	fun `then the uri is correct`() {
		assertThat(uri).isEqualTo(ContentUris.withAppendedId(MediaCollections.ExternalAudio, 234))
	}
}
