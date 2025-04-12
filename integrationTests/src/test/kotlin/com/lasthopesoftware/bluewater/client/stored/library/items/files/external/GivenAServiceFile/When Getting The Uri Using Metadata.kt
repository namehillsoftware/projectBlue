package com.lasthopesoftware.bluewater.client.stored.library.items.files.external.GivenAServiceFile

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.MetadataMediaFileUriProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.uri.MediaCollections
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.Test

class `When Getting The Uri Using Metadata` : AndroidContext() {
	companion object {
		private const val libraryId = 886
		private const val serviceFileId = "774"

		private val uriProvider by lazy {
            MetadataMediaFileUriProvider(
                mockk {
                    every {
                        promiseFileProperties(
                            LibraryId(libraryId),
                            ServiceFile(serviceFileId)
                        )
                    } returns Promise(
                        mapOf(
                            Pair(KnownFileProperties.Name, "6ouymea"),
                            Pair(KnownFileProperties.Album, "dZF7D2gj"),
                            Pair(KnownFileProperties.Artist, "HkWSHt0"),
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
                            MediaCollections.ExternalAudio,
                            arrayOf(
                                MediaStore.Audio.Media._ID,
                                MediaStore.Audio.Media.DISPLAY_NAME
                            ),
                            """${MediaStore.Audio.Media.IS_PENDING} = 0
				AND COALESCE(${MediaStore.Audio.Media.ARTIST}, "") = ?
				AND COALESCE(${MediaStore.Audio.Media.ALBUM_ARTIST}, "") = ?
				AND COALESCE(${MediaStore.Audio.Media.TITLE}, "") = ?
				AND COALESCE(${MediaStore.Audio.AlbumColumns.ALBUM}, "") = ?""",
                            arrayOf("HkWSHt0", "", "6ouymea", "dZF7D2gj"),
                            null
                        )
                    } returns mockk<Cursor>(relaxUnitFun = true) {
                        every { moveToFirst() } returns true
                        every { getColumnIndexOrThrow(MediaStore.Audio.Media._ID) } returns 183
                        every { isNull(183) } returns false
                        every { getLong(183) } returns 520L
                    }

                    every {
                        openFileDescriptor(
                            ContentUris.withAppendedId(
                                MediaCollections.ExternalAudio,
                                520
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
		Assertions.assertThat(uri)
            .isEqualTo(ContentUris.withAppendedId(MediaCollections.ExternalAudio, 520))
	}
}
