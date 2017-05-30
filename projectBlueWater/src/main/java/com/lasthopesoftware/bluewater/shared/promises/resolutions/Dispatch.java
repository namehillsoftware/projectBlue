package com.lasthopesoftware.bluewater.shared.promises.resolutions;

import android.content.Context;
import android.os.Handler;

import com.lasthopesoftware.bluewater.shared.promises.extensions.DispatchedPromise;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.callables.CarelessTwoParameterFunction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

public class Dispatch {
	public static <TResult, TNewResult> CarelessOneParameterFunction<TResult, Promise<TNewResult>> toContext(CarelessOneParameterFunction<TResult, TNewResult> task, Context context) {
		return toHandler(task, new Handler(context.getMainLooper()));
	}

	public static <TResult, TNewResult> CarelessOneParameterFunction<TResult, Promise<TNewResult>> toHandler(CarelessOneParameterFunction<TResult, TNewResult> task, Handler handler) {
		return new OneParameterExecutors.DispatchedFunction<>(task, handler);
	}

	public static <TResult, TNewResult> FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> toHandler(CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> task, Handler handler) {
		return new OneParameterExecutors.DispatchedCancellableFunction<>(task, handler);
	}

	private static class OneParameterExecutors {
		static class DispatchedCancellableTask<TResult, TNewResult> implements FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> {

			private final FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> task;
			private final Handler handler;

			DispatchedCancellableTask(FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> task, Handler handler) {
				this.task = task;
				this.handler = handler;
			}

			@Override
			public void runWith(TResult result, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				this.handler.post(new WrappedCancellableExecutor<>(task, result, resolve, reject, onCancelled));
			}
		}

		static class DispatchedFunction<TResult, TNewResult> implements CarelessOneParameterFunction<TResult, Promise<TNewResult>>, CarelessFunction<TNewResult> {

			private final CarelessOneParameterFunction<TResult, TNewResult> callable;
			private final Handler handler;

			private TResult result;

			DispatchedFunction(CarelessOneParameterFunction<TResult, TNewResult> callable, Handler handler) {
				this.callable = callable;
				this.handler = handler;
			}

			@Override
			public TNewResult result() throws Throwable {
				return callable.resultFrom(result);
			}

			@Override
			public Promise<TNewResult> resultFrom(TResult result) throws Throwable {
				this.result = result;
				return new DispatchedPromise<>(this, handler);
			}
		}

		static class DispatchedCancellableFunction<TResult, TNewResult> implements FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> {
			private final CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> task;
			private final Handler handler;

			DispatchedCancellableFunction(CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> task, Handler handler) {
				this.task = task;
				this.handler = handler;
			}

			@Override
			public void runWith(TResult result, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				this.handler.post(new WrappedCancellableFunction<>(this.task, result, resolve, reject, onCancelled));
			}

			private static class WrappedCancellableFunction<TResult, TNewResult> implements Runnable {
				private final CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> task;
				private final TResult result;
				private final IResolvedPromise<TNewResult> resolve;
				private final IRejectedPromise reject;
				private final OneParameterAction<Runnable> onCancelled;

				WrappedCancellableFunction(CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> task, TResult result, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
					this.task = task;
					this.result = result;
					this.resolve = resolve;
					this.reject = reject;
					this.onCancelled = onCancelled;
				}

				@Override
				public void run() {
					try {
						this.resolve.sendResolution(this.task.resultFrom(result, onCancelled));
					} catch (Throwable rejection) {
						this.reject.sendRejection(rejection);
					}
				}
			}
		}

		static class WrappedFunction<TResult, TNewResult> implements Runnable {
			private final CarelessOneParameterFunction<TResult, TNewResult> callable;
			private final TResult result;
			private final IRejectedPromise reject;
			private final IResolvedPromise<TNewResult> resolve;

			WrappedFunction(CarelessOneParameterFunction<TResult, TNewResult> callable, TResult result, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject) {
				this.callable = callable;
				this.result = result;
				this.reject = reject;
				this.resolve = resolve;
			}

			@Override
			public void run() {
				try {
					resolve.sendResolution(this.callable.resultFrom(result));
				} catch (Throwable rejection) {
					reject.sendRejection(rejection);
				}
			}
		}

		static class WrappedCancellableExecutor<TResult, TNewResult> implements Runnable {
			private final FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> task;
			private final TResult result;
			private final OneParameterAction<Runnable> onCancelled;
			private final IRejectedPromise reject;
			private final IResolvedPromise<TNewResult> resolve;

			WrappedCancellableExecutor(FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> task, TResult result, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				this.task = task;
				this.result = result;
				this.onCancelled = onCancelled;
				this.reject = reject;
				this.resolve = resolve;
			}

			@Override
			public void run() {
				this.task.runWith(result, resolve, reject, onCancelled);
			}
		}
	}
}
