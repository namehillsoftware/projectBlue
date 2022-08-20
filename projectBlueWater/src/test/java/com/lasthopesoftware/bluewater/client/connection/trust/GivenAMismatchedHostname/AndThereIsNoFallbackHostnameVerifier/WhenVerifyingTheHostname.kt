package com.lasthopesoftware.bluewater.client.connection.trust.GivenAMismatchedHostname.AndThereIsNoFallbackHostnameVerifier

import com.lasthopesoftware.bluewater.client.connection.trust.AdditionalHostnameVerifier
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenVerifyingTheHostname {
	private var isHostnameValid = false

	@BeforeAll
	fun before() {
		val additionalHostnameVerifier = AdditionalHostnameVerifier("my-test-host-name", null)
		isHostnameValid = additionalHostnameVerifier.verify("my-other-test-host-name", null)
	}

	@Test
	fun `then the hostname is valid`() {
		assertThat(isHostnameValid).isFalse
	}
}
