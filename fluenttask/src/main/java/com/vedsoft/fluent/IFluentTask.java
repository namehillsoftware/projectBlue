package com.vedsoft.fluent;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.callables.TwoParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by david on 9/24/16.
 */
public interface IFluentTask<TParams, TProgress, TResult> {
	IFluentTask<TParams,TProgress,TResult> execute();

	IFluentTask<TParams,TProgress,TResult> execute(Executor exec);

	TResult get() throws ExecutionException, InterruptedException;

	TResult get(long timeout, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException;

	TResult get(Executor executor) throws ExecutionException, InterruptedException;

	TResult get(Executor executor, long timeout, TimeUnit timeUnit) throws TimeoutException, ExecutionException, InterruptedException;

	IFluentTask<TParams,TProgress,TResult> cancel();

	IFluentTask<TParams,TProgress,TResult> cancel(boolean interrupt);

	boolean isCancelled();

	IFluentTask<TParams,TProgress,TResult> beforeStart(OneParameterRunnable<IFluentTask<TParams,TProgress,TResult>> listener);

	IFluentTask<TParams, TProgress, TResult> beforeStart(Runnable listener);

	IFluentTask<TParams, TProgress, TResult> onComplete(ThreeParameterRunnable<IFluentTask<TParams, TProgress, TResult>, TResult, Exception> listener);

	IFluentTask<TParams, TProgress, TResult> onComplete(TwoParameterRunnable<TResult, Exception> listener);

	IFluentTask<TParams, TProgress, TResult> onComplete(OneParameterRunnable<TResult> listener);

	IFluentTask<TParams,TProgress,TResult> onProgress(TwoParameterRunnable<IFluentTask<TParams,TProgress,TResult>, TProgress[]> listener);

	IFluentTask<TParams,TProgress,TResult> onProgress(OneParameterRunnable<TProgress[]> listener);

	IFluentTask<TParams,TProgress,TResult> onError(TwoParameterCallable<IFluentTask<TParams,TProgress,TResult>, Exception, Boolean> listener);

	IFluentTask<TParams,TProgress,TResult> onError(OneParameterCallable<Exception, Boolean> listener);
}
