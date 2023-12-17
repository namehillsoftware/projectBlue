package com.lasthopesoftware.bluewater.client.stored.library.items.files.external.GivenPendingContent

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.AndroidContextRunner
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.ExternalAudioContent
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.ExternalContentRepository
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.uri.MediaCollections
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URI

@RunWith(AndroidContextRunner::class)
class WhenMarkingItAsNotPending : AndroidContext() {

	private val sut by lazy {
		ExternalContentRepository(
			affectedSystems,
		)
	}
	companion object {
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
		contentUri = sut.promiseNewContentUri(ExternalAudioContent()).toExpiringFuture().get()?.also {
			sut.markContentAsNotPending(it).toExpiringFuture().get()
		}
	}

	@Test
	fun `then the URI is correct`() {
		assertThat(contentUri?.toString()).isEqualTo("escape://inventor")
	}

	@Test
	fun `then the content is created as pending`() {
		assertThat(insertedContent?.getAsInteger(MediaStore.Audio.Media.IS_PENDING)).isEqualTo(1)
	}

	@Test
	fun `then the updated content is not pending`() {
		assertThat(updatedContent?.getAsInteger(MediaStore.Audio.Media.IS_PENDING)).isEqualTo(0)
	}
}
