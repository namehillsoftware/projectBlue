package com.lasthopesoftware.promises.specs.GivenAPromiseThatResolves;

import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.OneParameterFunction;

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

	private OneParameterFunction firstResultHandler;
	private OneParameterFunction<String, ?> secondResultHandler;

	@Before
	public void before() {
		final IPromise<String> rootPromise =
			new Promise<>(() -> "test");

		firstResultHandler = mock(OneParameterFunction.class);

		rootPromise
			.then(firstResultHandler);

		secondResultHandler = mock(OneParameterFunction.class);

		rootPromise
			.then(secondResultHandler);
	}

	@Test
	public void thenTheFirstResolutionIsCalled() {
		verify(firstResultHandler, times(1)).expectedUsing(any());
	}

	@Test
	public void thenTheSecondResolutionIsCalled() {
		verify(secondResultHandler, times(1)).expectedUsing(any());
	}
}
