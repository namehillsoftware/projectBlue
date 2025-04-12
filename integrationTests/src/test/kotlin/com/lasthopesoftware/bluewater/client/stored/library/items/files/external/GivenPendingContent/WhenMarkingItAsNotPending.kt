package com.lasthopesoftware.bluewater.client.stored.library.items.files.external.GivenPendingContent

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.ExternalContentRepository
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.ExternalMusicContent
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.uri.MediaCollections
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.Test
import java.net.URI

class WhenMarkingItAsNotPending : AndroidContext() {
	companion object {

		private val sut by lazy {
            ExternalContentRepository(
                affectedSystems,
                mockk(),
            )
		}

		private val affectedSystems by lazy {
            mockk<ContentResolver> {
                every { insert(MediaCollections.ExternalAudio, any()) } answers {
                    insertedContent = lastArg()
                    Uri.parse("escape://inventor")
                }

                every { update(Uri.parse("escape://inventor"), any(), null, null) } answers {
                    updatedContent = secondArg()
                    1
                }
            }
		}
		private var contentUri: URI? = null

		private var insertedContent: ContentValues? = null
		private var updatedContent: ContentValues? = null
	}

	override fun before() {
		contentUri = sut.promiseNewContentUri(ExternalMusicContent()).toExpiringFuture().get()?.also {
			sut.markContentAsNotPending(it).toExpiringFuture().get()
		}
	}

	@Test
	fun `then the URI is correct`() {
		Assertions.assertThat(contentUri?.toString()).isEqualTo("escape://inventor")
	}

	@Test
	fun `then the content is created as pending`() {
		Assertions.assertThat(insertedContent?.getAsInteger(MediaStore.Audio.Media.IS_PENDING)).isEqualTo(1)
	}

	@Test
	fun `then the updated content is not pending`() {
		Assertions.assertThat(updatedContent?.getAsInteger(MediaStore.Audio.Media.IS_PENDING)).isEqualTo(0)
	}
}
