package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException

private const val libraryId = 390
private const val itemId = 398

class GivenALibrary {

	private val mut by lazy {
		val fakeConnectionProvider = FakeConnectionProvider()
		fakeConnectionProvider.mapResponse(
			{
				FakeConnectionResponseTuple(
					200,
					"""<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
<Response Status="Failure"/>""".encodeToByteArray()
				)
			},
			"Browse/Children",
			"ID=$itemId",
			"Version=2",
			"ErrorOnMissing=1"
		)

		val fakeLibraryConnectionProvider = FakeLibraryConnectionProvider(
			mapOf(Pair(LibraryId(libraryId), fakeConnectionProvider))
		)
		ItemProvider(GuaranteedLibraryConnectionProvider(fakeLibraryConnectionProvider))
	}

	private var exception: IOException? = null

	@BeforeAll
	fun act() {
		try {
			mut.promiseItems(LibraryId(libraryId), ItemId(itemId)).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			exception = e.cause as? IOException
		}
	}

	@Test
	fun `then an exception is thrown`() {
		assertThat(exception?.message).isEqualTo("Server returned 'Failure'.")
	}
}
