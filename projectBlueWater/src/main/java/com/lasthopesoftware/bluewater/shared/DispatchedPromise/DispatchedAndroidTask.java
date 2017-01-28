package com.lasthopesoftware.bluewater.shared.DispatchedPromise;

import android.os.AsyncTask;

import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.fluent.AsyncExceptionTask;
import com.vedsoft.futures.callables.Function;
import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import java.util.concurrent.Executor;

/**
 * Created by david on 11/3/16.
 */

class DispatchedAndroidTask<TResult> implements ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> {

	private final OneParameterFunction<OneParameterAction<Runnable>, TResult> task;
	private final Executor executor;

	DispatchedAndroidTask(Function<TResult> task) {
		this((c) -> task.result());
	}

	DispatchedAndroidTask(Function<TResult> task, Executor executor) {
		this((c) -> task.result(), executor);
	}

	DispatchedAndroidTask(OneParameterFunction<OneParameterAction<Runnable>, TResult> task) {
		this(task, AsyncTask.SERIAL_EXECUTOR);
	}

	DispatchedAndroidTask(OneParameterFunction<OneParameterAction<Runnable>, TResult> task, Executor executor) {
		this.task = task;
		this.executor = executor;
	}

	@Override
	public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
		new DispatchedAsyncTask<>(task, reject, resolve, onCancelled).executeOnExecutor(executor);
	}

	private static class DispatchedAsyncTask<TResult> extends AsyncExceptionTask<Void, Void, TResult> {
		private final OneParameterFunction<OneParameterAction<Runnable>, TResult> task;
		private final OneParameterAction<Runnable> onCancelled;
		private final IRejectedPromise reject;
		private final IResolvedPromise<TResult> resolve;

		DispatchedAsyncTask(OneParameterFunction<OneParameterAction<Runnable>, TResult> task, IRejectedPromise reject, IResolvedPromise<TResult> resolve, OneParameterAction<Runnable> onCancelled) {
			this.task = task;
			this.onCancelled = onCancelled;
			this.reject = reject;
			this.resolve = resolve;
		}

		@Override
		protected TResult doInBackground(Void... params) {
			try {
				return task.resultFrom(onCancelled);
			} catch (Exception e) {
				setException(e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(TResult result, Exception exception) {
			if (exception != null) {
				reject.withError(exception);
				return;
			}

			resolve.withResult(result);
		}
	}
}
