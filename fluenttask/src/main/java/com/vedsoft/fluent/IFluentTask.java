package com.vedsoft.fluent;

import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.callables.TwoParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

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

	IFluentTask<TParams,TProgress,TResult> beforeStart(OneParameterAction<IFluentTask<TParams,TProgress,TResult>> listener);

	IFluentTask<TParams, TProgress, TResult> beforeStart(Runnable listener);

	IFluentTask<TParams, TProgress, TResult> onComplete(ThreeParameterAction<IFluentTask<TParams, TProgress, TResult>, TResult, Exception> listener);

	IFluentTask<TParams, TProgress, TResult> onComplete(TwoParameterAction<TResult, Exception> listener);

	IFluentTask<TParams, TProgress, TResult> onComplete(OneParameterAction<TResult> listener);

	IFluentTask<TParams,TProgress,TResult> onProgress(TwoParameterAction<IFluentTask<TParams,TProgress,TResult>, TProgress[]> listener);

	IFluentTask<TParams,TProgress,TResult> onProgress(OneParameterAction<TProgress[]> listener);

	IFluentTask<TParams,TProgress,TResult> onError(TwoParameterFunction<IFluentTask<TParams,TProgress,TResult>, Exception, Boolean> listener);

	IFluentTask<TParams,TProgress,TResult> onError(OneParameterFunction<Exception, Boolean> listener);
}
