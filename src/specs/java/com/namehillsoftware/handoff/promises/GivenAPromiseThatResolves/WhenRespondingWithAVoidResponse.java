package com.namehillsoftware.handoff.promises.GivenAPromiseThatResolves;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenRespondingWithAVoidResponse {
	private static boolean isCalled;

	@BeforeClass
	public static void before() {
		new Promise<>("test")
				.then(new VoidResponse<>(result -> isCalled = true));
	}

	@Test
	public void thenTheVoidResponseIsCalled() {
		assertThat(isCalled).isTrue();
	}
}
