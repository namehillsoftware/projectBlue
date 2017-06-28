package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.callables.CarelessTwoParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.Arrays;
import java.util.Collection;

public class Promise<Resolution> {

	private final AwaitingMessenger<?, Resolution> messenger;

	public Promise(OneParameterAction<Messenger<Resolution>> executor) {
		this(new Execution.MessengerTunnel<Resolution>(executor));
	}

	public Promise(CarelessFunction<Resolution> executor) {
		this(new Execution.InternalExpectedPromiseExecutor<Resolution>(executor));
	}

	public Promise(Resolution passThroughResult) {
		this(new Execution.PassThroughCallable<Resolution>(passThroughResult));
	}

	public Promise(EmptyMessenger<Resolution> messenger) {
		this((AwaitingMessenger<Void, Resolution>)messenger);
		messenger.requestResolution();
	}

	private Promise(AwaitingMessenger<?, Resolution> awaitingMessenger) {
		this.messenger = awaitingMessenger;
	}

	public final void cancel() {
		messenger.cancel();
	}

	private <NewResolution> Promise<NewResolution> next(AwaitingMessenger<Resolution, NewResolution> onFulfilled) {
		messenger.awaitResolution(onFulfilled);

		return new Promise<>(onFulfilled);
	}

	public final <TNewResult> Promise<TNewResult> next(final CarelessOneParameterFunction<Resolution, TNewResult> onFulfilled) {
		return next(new Execution.ExpectedResultExecutor<>(onFulfilled));
	}

	public final <TNewResult> Promise<TNewResult> then(CarelessOneParameterFunction<Resolution, Promise<TNewResult>> onFulfilled) {
		return next(new PromisedResolution<>(onFulfilled));
	}

	final <TNewRejectedResult> Promise<TNewRejectedResult> error(ErrorMessenger<Resolution, TNewRejectedResult> errorMessenger) {
		return next(errorMessenger);
	}

	public final <TNewRejectedResult> Promise<TNewRejectedResult> error(CarelessTwoParameterFunction<Throwable, OneParameterAction<Runnable>, TNewRejectedResult> onRejected) {
		return next(new Execution.Cancellable.RejectionDependentCancellableCaller<>(onRejected));
	}

	public final <TNewRejectedResult> Promise<TNewRejectedResult> error(CarelessOneParameterFunction<Throwable, TNewRejectedResult> onRejected) {
		return next(new Execution.ErrorResultExecutor<>(onRejected));
	}

	public static <TResult> Promise<TResult> empty() {
		return new Promise<>((TResult)null);
	}

	@SafeVarargs
	public static <TResult> Promise<Collection<TResult>> whenAll(Promise<TResult>... promises) {
		return whenAll(Arrays.asList(promises));
	}

	public static <TResult> Promise<Collection<TResult>> whenAll(Collection<Promise<TResult>> promises) {
		return new Promise<>(new Resolutions.AggregatePromiseResolver<TResult>(promises));
	}

	@SafeVarargs
	public static <TResult> Promise<TResult> whenAny(Promise<TResult>... promises) {
		return whenAny(Arrays.asList(promises));
	}

	public static <TResult> Promise<TResult> whenAny(Collection<Promise<TResult>> promises) {
		return new Promise<>(new Resolutions.FirstPromiseResolver<TResult>(promises));
	}
}
