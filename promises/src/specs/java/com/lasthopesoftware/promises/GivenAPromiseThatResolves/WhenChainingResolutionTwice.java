package com.lasthopesoftware.promises.GivenAPromiseThatResolves;

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
		final Promise<String> rootPromise =
			new Promise<>("test");

		firstResultHandler = mock(CarelessOneParameterFunction.class);

		rootPromise
			.next(firstResultHandler);

		secondResultHandler = mock(CarelessOneParameterFunction.class);

		rootPromise
			.next(secondResultHandler);
	}

	@Test
	public void thenTheFirstResolutionIsCalled() throws Throwable {
		verify(firstResultHandler, times(1)).resultFrom(any());
	}

	@Test
	public void thenTheSecondResolutionIsCalled() throws Throwable {
		verify(secondResultHandler, times(1)).resultFrom(any());
	}
}
