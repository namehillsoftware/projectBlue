package com.lasthopesoftware.promises.cancellation.GivenAStandardCancellationToken;


import com.lasthopesoftware.promises.cancellation.CancellationToken;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenNotRun {
	private static CancellationToken cancellationToken = new CancellationToken();

	@Test
	public void thenTheTokenIsNotCancelled() {
		assertThat(cancellationToken.isCancelled()).isFalse();
	}
}
