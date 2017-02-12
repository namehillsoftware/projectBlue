package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.FiveParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import java.util.concurrent.Callable;

public class Promise<TResult> extends DependentCancellablePromise<Void, TResult> {

	public Promise(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
		super(new Execution.InternalCancellablePromiseExecutor<>(executor));

		provide(null, null);
	}

	public Promise(TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor) {
		this(new Execution.InternalPromiseExecutor<>(executor));
	}

	public Promise(Callable<TResult> executor) {
		this(new Execution.InternalExpectedPromiseExecutor<>(executor));
	}

	public Promise(TResult passThroughResult) {
		this(new Execution.PassThroughCallable<>(passThroughResult));
	}

	public static <TResult> Promise<TResult> empty() {
		return new Promise<>((TResult)null);
	}

	private static class Execution {

		/**
		 * Created by david on 10/30/16.
		 */
		static class InternalCancellablePromiseExecutor<TResult> implements FiveParameterAction<Void, Exception, IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> {
			private final ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor;

			InternalCancellablePromiseExecutor(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
				this.executor = executor;
			}

			@Override
			public void runWith(Void result, Exception exception, IResolvedPromise<TResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
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
			private final Callable<TResult> executor;

			InternalExpectedPromiseExecutor(Callable<TResult> executor) {
				this.executor = executor;
			}

			@Override
			public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject) {
				try {
					resolve.withResult(executor.call());
				} catch (Exception e) {
					reject.withError(e);
				}
			}
		}

		private static class PassThroughCallable<TPassThroughResult> implements Callable<TPassThroughResult> {
			private final TPassThroughResult passThroughResult;

			PassThroughCallable(TPassThroughResult passThroughResult) {
				this.passThroughResult = passThroughResult;
			}

			@Override
			public TPassThroughResult call() throws Exception {
				return passThroughResult;
			}
		}
	}
}
