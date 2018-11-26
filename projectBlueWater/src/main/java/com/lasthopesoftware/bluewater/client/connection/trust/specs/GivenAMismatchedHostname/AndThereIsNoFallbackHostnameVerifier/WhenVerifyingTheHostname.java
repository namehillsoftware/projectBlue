package com.lasthopesoftware.bluewater.client.connection.trust.specs.GivenAMismatchedHostname.AndThereIsNoFallbackHostnameVerifier;

import com.lasthopesoftware.bluewater.client.connection.trust.AdditionalHostnameVerifier;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenVerifyingTheHostname {

	private static boolean isHostnameValid;

	@BeforeClass
	public static void before() {
		final AdditionalHostnameVerifier additionalHostnameVerifier = new AdditionalHostnameVerifier("my-test-host-name", null);
		isHostnameValid = additionalHostnameVerifier.verify("my-other-test-host-name", null);
	}

	@Test
	public void thenTheHostnameIsValid() {
		assertThat(isHostnameValid).isFalse();
	}
}
