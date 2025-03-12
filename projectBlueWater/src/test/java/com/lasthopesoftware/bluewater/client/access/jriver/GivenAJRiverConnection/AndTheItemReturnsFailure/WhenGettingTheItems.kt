package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndTheItemReturnsFailure

import com.lasthopesoftware.bluewater.client.access.JRiverLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.FakeJRiverConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException

class WhenGettingTheItems {

	companion object {
		private const val itemId = 398
	}

	private val mut by lazy {
		val fakeConnectionProvider = FakeJRiverConnectionProvider()
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

        JRiverLibraryAccess(fakeConnectionProvider)
	}

	private var exception: IOException? = null

	@BeforeAll
	fun act() {
		try {
			mut.promiseItems(ItemId(itemId)).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			exception = e.cause as? IOException
		}
	}

	@Test
	fun `then an exception is thrown`() {
		assertThat(exception?.message).isEqualTo("Server returned 'Failure'.")
	}
}
