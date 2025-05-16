package com.lasthopesoftware.bluewater.client.connection.session.GivenANullLibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenTestingIfTheMediaCenterConnectionDetailsIsActive {

	private val mut by lazy {
		ConnectionSessionManager(mockk(), PromisedConnectionsRepository(), RecordingApplicationMessageBus())
	}

	private var isActive = false

	@BeforeAll
	fun act() {
		isActive = mut.promiseIsConnectionActive(LibraryId(2)).toExpiringFuture().get() ?: false
	}

	@Test
	fun thenTheConnectionIsNotActive() {
		assertThat(isActive).isFalse
	}
}
