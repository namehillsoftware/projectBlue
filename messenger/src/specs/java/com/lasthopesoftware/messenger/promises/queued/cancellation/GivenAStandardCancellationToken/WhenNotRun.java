package com.lasthopesoftware.messenger.promises.queued.cancellation.GivenAStandardCancellationToken;


import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellationToken;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenNotRun {
	private static CancellationToken cancellationToken = new CancellationToken();

	@Test
	public void thenTheTokenIsNotCancelled() {
		assertThat(cancellationToken.isCancelled()).isFalse();
	}
}
