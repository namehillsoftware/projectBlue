package com.namehillsoftware.handoff.promises.GivenAPromiseThatResolves;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class WhenChainingResolutionTwice {

	private static ImmediateResponse<String, ?> firstResultHandler;
	private static ImmediateResponse<String, ?> secondResultHandler;

	@BeforeClass
	public static void before() {
		final Promise<String> rootPromise =
			new Promise<>("test");

		firstResultHandler = mock(ImmediateResponse.class);

		rootPromise
			.then(firstResultHandler);

		secondResultHandler = mock(ImmediateResponse.class);

		rootPromise
			.then(secondResultHandler);
	}

	@Test
	public void thenTheFirstResolutionIsCalled() throws Throwable {
		verify(firstResultHandler, times(1)).respond(any());
	}

	@Test
	public void thenTheSecondResolutionIsCalled() throws Throwable {
		verify(secondResultHandler, times(1)).respond(any());
	}
}
