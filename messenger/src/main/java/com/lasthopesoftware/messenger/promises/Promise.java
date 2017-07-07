package com.lasthopesoftware.messenger.promises;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.SingleMessageBroadcaster;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.Arrays;
import java.util.Collection;

public class Promise<Resolution> {

	private final SingleMessageBroadcaster<Resolution> singleMessageBroadcaster;

	public Promise(OneParameterAction<Messenger<Resolution>> executor) {
		this();
		executor.runWith(singleMessageBroadcaster);
	}

	public Promise(Resolution passThroughResult) {
		this();
		singleMessageBroadcaster.sendResolution(passThroughResult);
	}

	private Promise() {
		this(new SingleMessageBroadcaster<>());
	}

	private Promise(SingleMessageBroadcaster<Resolution> singleMessageBroadcaster) {
		this.singleMessageBroadcaster = singleMessageBroadcaster;
	}

	public final void cancel() {
		singleMessageBroadcaster.cancel();
	}

	private <NewResolution> Promise<NewResolution> next(ResolutionResponseMessenger<Resolution, NewResolution> onFulfilled) {
		singleMessageBroadcaster.awaitResolution(onFulfilled);

		return new Promise<>(onFulfilled);
	}

	public final <TNewResult> Promise<TNewResult> next(final CarelessOneParameterFunction<Resolution, TNewResult> onFulfilled) {
		return next(new Execution.ExpectedResult<>(onFulfilled));
	}

	public final <TNewResult> Promise<TNewResult> then(CarelessOneParameterFunction<Resolution, Promise<TNewResult>> onFulfilled) {
		return next(new PromisedResolutionResponseMessenger<>(onFulfilled));
	}

	private <TNewRejectedResult> Promise<TNewRejectedResult> _catch(RejectionResponseMessenger<Resolution, TNewRejectedResult> rejectionResponseMessenger) {
		singleMessageBroadcaster.awaitResolution(rejectionResponseMessenger);

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
