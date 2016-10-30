package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.FiveParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import org.jetbrains.annotations.NotNull;

public class Promise<TResult> extends DependentCancellablePromise<Void, TResult> {

	public Promise(@NotNull ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
		super(new Execution.InternalCancellablePromiseExecutor<>(executor));

		provide(null, null);
	}

	public Promise(@NotNull TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor) {
		this(new Execution.InternalPromiseExecutor<>(executor));
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

			InternalPromiseExecutor(@NotNull TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor) {
				this.executor = executor;
			}

			@Override
			public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				executor.runWith(resolve, reject);
			}
		}
	}
}
