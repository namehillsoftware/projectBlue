package com.namehillsoftware.handoff.promises.GivenAPromiseThatResolves;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenRespondingWithAVoidResponse {
	private static boolean isCalled;
	private static boolean unhandledRejectionReceiverCalled;

	@BeforeClass
	public static void before() {
		Promise.Rejections.setUnhandledRejectionsReceiver(e -> unhandledRejectionReceiverCalled = true);
		try {
			new Promise<>("test")
					.then(new VoidResponse<>(result -> isCalled = true));
		} finally {
			Promise.Rejections.setUnhandledRejectionsReceiver(null);
		}
	}

	@Test
	public void thenTheVoidResponseIsCalled() {
		assertThat(isCalled).isTrue();
	}

	@Test
	public void thenTheUnhandledRejectionReceiverIsNotCalled() {
		assertThat(unhandledRejectionReceiverCalled).isFalse();
	}
}
