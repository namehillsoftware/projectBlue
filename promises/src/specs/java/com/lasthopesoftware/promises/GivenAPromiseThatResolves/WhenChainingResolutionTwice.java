package com.lasthopesoftware.promises.GivenAPromiseThatResolves;

import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 10/29/16.
 */

public class WhenChainingResolutionTwice {

	private static CarelessOneParameterFunction<String, ?> firstResultHandler;
	private static CarelessOneParameterFunction<String, ?> secondResultHandler;

	@BeforeClass
	public static void before() {
		final IPromise<String> rootPromise =
			new Promise<>(() -> "test");

		firstResultHandler = mock(CarelessOneParameterFunction.class);

		rootPromise
			.then(firstResultHandler);

		secondResultHandler = mock(CarelessOneParameterFunction.class);

		rootPromise
			.then(secondResultHandler);
	}

	@Test
	public void thenTheFirstResolutionIsCalled() throws Exception {
		verify(firstResultHandler, times(1)).resultFrom(any());
	}

	@Test
	public void thenTheSecondResolutionIsCalled() throws Exception {
		verify(secondResultHandler, times(1)).resultFrom(any());
	}
}
