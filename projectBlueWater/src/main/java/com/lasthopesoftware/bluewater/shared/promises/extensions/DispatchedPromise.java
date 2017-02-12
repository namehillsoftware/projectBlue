package com.lasthopesoftware.bluewater.shared.promises.extensions;

import android.content.Context;
import android.os.Handler;

import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import java.util.concurrent.Callable;

/**
 * Created by david on 12/18/16.
 */

public class DispatchedPromise<TResult> extends Promise<TResult> {

	public DispatchedPromise(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor, Handler handler) {
		super(new Executors.DispatchedCancellableTask<>(executor, handler));
	}

	public DispatchedPromise(TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor, Handler handler) {
		super(new Executors.DispatchedTask<>(executor, handler));
	}

	public DispatchedPromise(Callable<TResult> executor, Handler handler) {
		super(new Executors.DispatchedCallable<>(executor, handler));
	}

	public DispatchedPromise(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor, Context context) {
		this(executor, new Handler(context.getMainLooper()));
	}

	public DispatchedPromise(TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor, Context context) {
		this(executor, new Handler(context.getMainLooper()));
	}

	public DispatchedPromise(Callable<TResult> executor, Context context) {
		this(executor, new Handler(context.getMainLooper()));
	}

	private static class Executors {
		static class DispatchedCancellableTask<TResult> implements ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> {

			private final ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> task;
			private final Handler handler;

			DispatchedCancellableTask(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> task, Handler handler) {
				this.task = task;
				this.handler = handler;
			}

			@Override
			public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				this.handler.post(new WrappedCancellableExecutor<>(task, resolve, reject, onCancelled));
			}
		}

		static class DispatchedTask<TResult> implements TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> {

			private final TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> task;
			private final Handler handler;

			DispatchedTask(TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> task, Handler handler) {
				this.task = task;
				this.handler = handler;
			}

			@Override
			public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject) {
				this.handler.post(new WrappedExecutor<>(task, resolve, reject));
			}
		}

		static class DispatchedCallable<TResult> implements TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> {

			private final Callable<TResult> callable;
			private final Handler handler;

			DispatchedCallable(Callable<TResult> callable, Handler handler) {
				this.callable = callable;
				this.handler = handler;
			}

			@Override
			public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject) {
				this.handler.post(new WrappedCallable<>(callable, resolve, reject));
			}
		}
	}
}
