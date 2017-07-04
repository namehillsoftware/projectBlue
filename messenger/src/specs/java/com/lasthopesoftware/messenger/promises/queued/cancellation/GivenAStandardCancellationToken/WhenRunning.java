package com.lasthopesoftware.messenger.promises.queued.cancellation.GivenAStandardCancellationToken;


import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellationToken;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenRunning {

	private static CancellationToken cancellationToken = new CancellationToken();

	@BeforeClass
	public static void before() {
		cancellationToken.run();
	}

	@Test
	public void thenTheTokenIsCancelled() {
		assertThat(cancellationToken.isCancelled()).isTrue();
	}
}