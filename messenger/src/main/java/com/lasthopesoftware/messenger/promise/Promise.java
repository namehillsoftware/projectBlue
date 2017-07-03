package com.lasthopesoftware.messenger.promise;

import com.lasthopesoftware.messenger.AwaitingMessenger;
import com.lasthopesoftware.messenger.Messenger;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.Arrays;
import java.util.Collection;

public class Promise<Resolution> {

	private final AwaitingMessenger<Resolution> resolutionAwaitingMessenger;

	public Promise(OneParameterAction<Messenger<Resolution>> executor) {
		this();
		executor.runWith(resolutionAwaitingMessenger);
	}

	public Promise(Resolution passThroughResult) {
		this();
		resolutionAwaitingMessenger.sendResolution(passThroughResult);
	}

	private Promise() {
		this(new AwaitingMessenger<>());
	}

	private Promise(AwaitingMessenger<Resolution> resolutionAwaitingMessenger) {
		this.resolutionAwaitingMessenger = resolutionAwaitingMessenger;
	}

	public final void cancel() {
		resolutionAwaitingMessenger.cancel();
	}

	private <NewResolution> Promise<NewResolution> next(ResolutionResponseMessenger<Resolution, NewResolution> onFulfilled) {
		resolutionAwaitingMessenger.awaitResolution(onFulfilled);

		return new Promise<>(onFulfilled);
	}

	public final <TNewResult> Promise<TNewResult> next(final CarelessOneParameterFunction<Resolution, TNewResult> onFulfilled) {
		return next(new Execution.ExpectedResult<>(onFulfilled));
	}

	public final <TNewResult> Promise<TNewResult> then(CarelessOneParameterFunction<Resolution, Promise<TNewResult>> onFulfilled) {
		return next(new PromisedResolutionResponseMessenger<>(onFulfilled));
	}

	private <TNewRejectedResult> Promise<TNewRejectedResult> _catch(RejectionResponseMessenger<Resolution, TNewRejectedResult> rejectionResponseMessenger) {
		resolutionAwaitingMessenger.awaitResolution(rejectionResponseMessenger);

		return new Promise<>(rejectionResponseMessenger);
	}

	public final <TNewRejectedResult> Promise<TNewRejectedResult> error(CarelessOneParameterFunction<Throwable, TNewRejectedResult> onRejected) {
		return _catch(new Execution.ErrorResultExecutor<>(onRejected));
	}

	public static <TResult> Promise<TResult> empty() {
		return new Promise<>((TResult)null);
	}

	@SafeVarargs
	public static <TResult> Promise<Collection<TResult>> whenAll(Promise<TResult>... promises) {
		return whenAll(Arrays.asList(promises));
	}

	public static <TResult> Promise<Collection<TResult>> whenAll(Collection<Promise<TResult>> promises) {
		return new Promise<>(new Resolutions.AggregatePromiseResolver<>(promises));
	}

	@SafeVarargs
	public static <TResult> Promise<TResult> whenAny(Promise<TResult>... promises) {
		return whenAny(Arrays.asList(promises));
	}

	public static <TResult> Promise<TResult> whenAny(Collection<Promise<TResult>> promises) {
		return new Promise<>(new Resolutions.FirstPromiseResolver<>(promises));
	}
}
