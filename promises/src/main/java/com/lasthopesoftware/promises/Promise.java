package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.callables.CarelessTwoParameterFunction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import java.util.Arrays;
import java.util.Collection;

public class Promise<TResult> {

	private final Messenger<?, TResult> messenger;

	public Promise(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
		this(new Execution.InternalCancellablePromiseExecutor<>(executor));
	}

	public Promise(TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor) {
		this(new Execution.InternalPromiseExecutor<>(executor));
	}

	public Promise(CarelessFunction<TResult> executor) {
		this(new Execution.InternalExpectedPromiseExecutor<>(executor));
	}

	public Promise(TResult passThroughResult) {
		this(new Execution.PassThroughCallable<>(passThroughResult));
	}

	public Promise(EmptyMessenger<TResult> messenger) {
		this((Messenger<Void, TResult>)messenger);
		messenger.requestResolution();
	}

	private Promise(Messenger<?, TResult> messenger) {
		this.messenger = messenger;
	}

	public final void cancel() {
		messenger.cancel();
	}

	protected final <TNewResult> Promise<TNewResult> then(Messenger<TResult, TNewResult> onFulfilled) {
		messenger.awaitResolution(onFulfilled);

		return new Promise<>(onFulfilled);
	}

	public final <NewResult> Promise<NewResult> then(ResolutionProcessor<TResult, NewResult> onFulfilled) {
		return then((Messenger<TResult, NewResult>)onFulfilled);
	}

	public final <TNewResult> Promise<TNewResult> then(FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
		return then(new Execution.Cancellable.ErrorPropagatingCancellableExecutor<>(onFulfilled));
	}

	public final <TNewResult> Promise<TNewResult> then(CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled) {
		return then(new Execution.Cancellable.ExpectedResultCancellableExecutor<>(onFulfilled));
	}

	public final <TNewRejectedResult> Promise<TNewRejectedResult> error(ErrorProcessor<TResult, TNewRejectedResult> errorProcessor) {
		return then(errorProcessor);
	}

	public final <TNewRejectedResult> Promise<TNewRejectedResult> error(FourParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected) {
		return then(new Execution.Cancellable.RejectionDependentCancellableExecutor<>(onRejected));
	}

	public final <TNewRejectedResult> Promise<TNewRejectedResult> error(CarelessTwoParameterFunction<Throwable, OneParameterAction<Runnable>, TNewRejectedResult> onRejected) {
		return error(new Execution.Cancellable.ExpectedResultCancellableExecutor<>(onRejected));
	}

	public final <TNewResult> Promise<TNewResult> thenPromise(CarelessOneParameterFunction<TResult, Promise<TNewResult>> onFulfilled) {
		return then(new Execution.PromisedResolution<>(onFulfilled));
	}

	public final <TNewResult> Promise<TNewResult> then(ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
		return then(new Execution.ErrorPropagatingResolveExecutor<>(onFulfilled));
	}

	public final <TNewResult> Promise<TNewResult> then(final CarelessOneParameterFunction<TResult, TNewResult> onFulfilled) {
		return then(new Execution.ExpectedResultExecutor<>(onFulfilled));
	}

	public final <TNewRejectedResult> Promise<TNewRejectedResult> error(ThreeParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected) {
		return then(new Execution.RejectionDependentExecutor<>(onRejected));
	}

	public final <TNewRejectedResult> Promise<TNewRejectedResult> error(CarelessOneParameterFunction<Throwable, TNewRejectedResult> onRejected) {
		return error(new Execution.ExpectedResultExecutor<>(onRejected));
	}

	public final <TNewResult> Promise<TNewResult> thenPromise(CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, Promise<TNewResult>> onFulfilled) {
		return then(new Execution.Cancellable.ResolvedCancellablePromise<>(onFulfilled));
	}

	public static <TResult> Promise<TResult> empty() {
		return new Promise<>((TResult)null);
	}

	@SafeVarargs
	public static <TResult> Promise<Collection<TResult>> whenAll(Promise<TResult>... promises) {
		return whenAll(Arrays.asList(promises));
	}

	public static <TResult> Promise<Collection<TResult>> whenAll(Collection<Promise<TResult>> promises) {
		return new Promise<>(new Resolution.AggregatePromiseResolver<>(promises));
	}

}
