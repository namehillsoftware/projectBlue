package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.FiveParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Promise<TResult> extends DependentCancellablePromise<Void, TResult> {

	public Promise(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
		super(new Execution.InternalCancellablePromiseExecutor<>(executor));

		provide(null, null);
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

	public static <TResult> Promise<TResult> empty() {
		return new Promise<>((TResult)null);
	}

	@SafeVarargs
	public static <TResult> IPromise<Collection<TResult>> whenAll(IPromise<TResult>... promises) {
		final Resolution.CollectedResultsTask<TResult> collectedResultsTask = new Resolution.CollectedResultsTask<>(promises.length);

		for (final IPromise<TResult> promise : promises)
			promise.then(collectedResultsTask);

		return new Promise<>(collectedResultsTask);
	}

	private static class Execution {

		static class InternalCancellablePromiseExecutor<TResult> implements FiveParameterAction<Void, Throwable, IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> {
			private final ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor;

			InternalCancellablePromiseExecutor(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
				this.executor = executor;
			}

			@Override
			public void runWith(Void result, Throwable exception, IResolvedPromise<TResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				executor.runWith(resolve, reject, onCancelled);
			}
		}

		/**
		 * Created by david on 10/8/16.
		 */
		static class InternalPromiseExecutor<TResult> implements ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> {
			private final TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor;

			InternalPromiseExecutor(TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor) {
				this.executor = executor;
			}

			@Override
			public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				executor.runWith(resolve, reject);
			}
		}

		private static class InternalExpectedPromiseExecutor<TResult> implements TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> {
			private final CarelessFunction<TResult> executor;

			InternalExpectedPromiseExecutor(CarelessFunction<TResult> executor) {
				this.executor = executor;
			}

			@Override
			public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject) {
				try {
					resolve.withResult(executor.result());
				} catch (Exception e) {
					reject.withError(e);
				}
			}
		}

		private static class PassThroughCallable<TPassThroughResult> implements CarelessFunction<TPassThroughResult> {
			private final TPassThroughResult passThroughResult;

			PassThroughCallable(TPassThroughResult passThroughResult) {
				this.passThroughResult = passThroughResult;
			}

			@Override
			public TPassThroughResult result() throws Exception {
				return passThroughResult;
			}
		}
	}

	private static class Resolution {
		private static class CollectedResultsTask<TResult> implements
			CarelessOneParameterFunction<TResult, TResult>,
			ThreeParameterAction<IResolvedPromise<Collection<TResult>>, IRejectedPromise, OneParameterAction<Runnable>> {

			private final int expectedResolutions;
			private final List<TResult> results;
			private IResolvedPromise<Collection<TResult>> resolve;

			CollectedResultsTask(int expectedResolutions) {
				results = new ArrayList<>(expectedResolutions);
				this.expectedResolutions = expectedResolutions;
			}

			@Override
			public TResult resultFrom(TResult result) throws Exception {
				results.add(result);

				if (results.size() == expectedResolutions) resolve.withResult(results);

				return result;
			}

			@Override
			public void runWith(IResolvedPromise<Collection<TResult>> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				this.resolve = resolve;

				if (results.size() == expectedResolutions) resolve.withResult(results);
			}
		}
	}
}
