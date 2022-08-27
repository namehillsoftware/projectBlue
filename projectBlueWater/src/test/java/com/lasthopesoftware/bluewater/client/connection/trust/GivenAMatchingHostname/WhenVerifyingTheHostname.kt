package com.lasthopesoftware.bluewater.client.connection.trust.GivenAMatchingHostname

import com.lasthopesoftware.bluewater.client.connection.trust.AdditionalHostnameVerifier
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import javax.net.ssl.HostnameVerifier

class WhenVerifyingTheHostname {
	private val defaultHostnameVerifier = mockk<HostnameVerifier>()
	private var isHostnameValid = false

	@BeforeAll
	fun act() {
		val additionalHostnameVerifier =
			AdditionalHostnameVerifier("my-test-host-name", defaultHostnameVerifier)
		isHostnameValid = additionalHostnameVerifier.verify("my-test-host-name", null)
	}

	@Test
	fun `then the hostname is valid`() {
		assertThat(isHostnameValid).isTrue
	}

	@Test
	fun `then the default hostname verifier is never asked`() {
		verify(exactly = 0) { defaultHostnameVerifier.verify("my-test-host-name", null) }
	}
}
