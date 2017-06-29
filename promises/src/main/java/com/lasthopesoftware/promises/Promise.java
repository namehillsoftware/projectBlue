package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.Arrays;
import java.util.Collection;

public class Promise<Resolution> extends AwaitingMessenger<Resolution> {

	public Promise(OneParameterAction<Messenger<Resolution>> executor) {
		executor.runWith(new Execution.MessengerTunnel<>(this));
	}

	public Promise(Resolution passThroughResult) {
		sendResolution(passThroughResult);
	}

	public Promise() {}

	private <NewResolution> Promise<NewResolution> next(ResolutionResponsePromise<Resolution, NewResolution> onFulfilled) {
		awaitResolution(onFulfilled);

		return onFulfilled;
	}

	public final <TNewResult> Promise<TNewResult> next(final CarelessOneParameterFunction<Resolution, TNewResult> onFulfilled) {
		return next(new Execution.ExpectedResultPromise<>(onFulfilled));
	}

	public final <TNewResult> Promise<TNewResult> then(CarelessOneParameterFunction<Resolution, Promise<TNewResult>> onFulfilled) {
		return next(new PromisedResolutionResponsePromise<>(onFulfilled));
	}

	private <TNewRejectedResult> Promise<TNewRejectedResult> error(RejectionResponsePromise<Resolution, TNewRejectedResult> rejectionResponsePromise) {
		awaitResolution(rejectionResponsePromise);

		return rejectionResponsePromise;
	}

	public final <TNewRejectedResult> Promise<TNewRejectedResult> error(CarelessOneParameterFunction<Throwable, TNewRejectedResult> onRejected) {
		return error(new Execution.ErrorResultExecutor<>(onRejected));
	}

	public static <TResult> Promise<TResult> empty() {
		return new Promise<>((TResult)null);
	}

	@SafeVarargs
	public static <TResult> Promise<Collection<TResult>> whenAll(Promise<TResult>... promises) {
		return whenAll(Arrays.asList(promises));
	}

	public static <TResult> Promise<Collection<TResult>> whenAll(Collection<Promise<TResult>> promises) {
		return new Resolutions.AggregatePromiseResolver<>(promises);
	}

	@SafeVarargs
	public static <TResult> Promise<TResult> whenAny(Promise<TResult>... promises) {
		return whenAny(Arrays.asList(promises));
	}

	public static <TResult> Promise<TResult> whenAny(Collection<Promise<TResult>> promises) {
		return new Resolutions.FirstPromiseResolver<>(promises);
	}
}
