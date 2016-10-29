package com.lasthopesoftware.promises.specs.GivenAPromiseThatResolves;

import com.lasthopesoftware.promises.ExpectedPromise;
import com.lasthopesoftware.promises.IPromise;
import com.vedsoft.futures.runnables.OneParameterAction;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 10/29/16.
 */

public class WhenChainingResolutionTwice {

	private OneParameterAction<String> secondResultHandler;

	@Before
	public void before() {
		final IPromise<String> rootPromise =
			new ExpectedPromise<>(() -> "test");

		rootPromise
			.then(result -> 330 + result.hashCode());

		secondResultHandler = mock(OneParameterAction.class);

		rootPromise
			.then(secondResultHandler);
	}

	@Test
	public void thenTheSecondResolutionIsNotCalled() {
		verify(secondResultHandler, times(0)).runWith(any());
	}
}
