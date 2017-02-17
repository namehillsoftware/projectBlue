package com.lasthopesoftware.bluewater.shared.promises.extensions;

import com.lasthopesoftware.bluewater.shared.promises.WrappedCancellableExecutor;
import com.lasthopesoftware.bluewater.shared.promises.WrappedExecutor;
import com.lasthopesoftware.bluewater.shared.promises.WrappedFunction;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import java.util.concurrent.Executor;

/**
 * Created by david on 2/12/17.
 */

public class QueuedPromise<TResult> extends Promise<TResult> {
	public QueuedPromise(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> task, Executor executor) {
		super(new Executors.QueuedCancellableTask<>(task, executor));
	}

	public QueuedPromise(TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> task, Executor executor) {
		super(new Executors.QueuedTask<>(task, executor));
	}

	public QueuedPromise(CarelessFunction<TResult> task, Executor executor) {
		super(new Executors.QueuedFunction<>(task, executor));
	}

	private static class Executors {
		static class QueuedCancellableTask<TResult> implements ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> {

			private final ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> task;
			private final Executor executor;

			QueuedCancellableTask(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> task, Executor executor) {
				this.task = task;
				this.executor = executor;
			}

			@Override
			public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				this.executor.execute(new WrappedCancellableExecutor<>(task, resolve, reject, onCancelled));
			}
		}

		static class QueuedTask<TResult> implements TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> {

			private final TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> task;
			private final Executor executor;

			QueuedTask(TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> task, Executor executor) {
				this.task = task;
				this.executor = executor;
			}

			@Override
			public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject) {
				this.executor.execute(new WrappedExecutor<>(task, resolve, reject));
			}
		}

		static class QueuedFunction<TResult> implements TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> {

			private final CarelessFunction<TResult> callable;
			private final Executor executor;

			QueuedFunction(CarelessFunction<TResult> callable, Executor executor) {
				this.callable = callable;
				this.executor = executor;
			}

			@Override
			public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject) {
				this.executor.execute(new WrappedFunction<>(callable, resolve, reject));
			}
		}
	}
}
