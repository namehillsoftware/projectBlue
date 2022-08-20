package com.lasthopesoftware.bluewater.client.connection.trust.GivenAMismatchedHostname

import com.lasthopesoftware.bluewater.client.connection.trust.AdditionalHostnameVerifier
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import javax.net.ssl.HostnameVerifier

class WhenVerifyingTheHostname {
	private val defaultHostnameVerifier = mockk<HostnameVerifier>(relaxed = true)

	private var isHostnameValid = false

	@BeforeAll
	fun before() {
		val additionalHostnameVerifier =
			AdditionalHostnameVerifier("my-test-host-name", defaultHostnameVerifier)
		isHostnameValid = additionalHostnameVerifier.verify("my-other-test-host-name", null)
	}

	@Test
	fun thenTheHostnameIsValid() {
		assertThat(isHostnameValid).isFalse
	}

	@Test
	fun thenTheDefaultHostnameVerifierIsAlsoAsked() {
		verify(atLeast = 1) { defaultHostnameVerifier.verify("my-other-test-host-name", null) }
	}
}
