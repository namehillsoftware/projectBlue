package com.lasthopesoftware.threading;

import com.lasthopesoftware.callables.IThreeParameterCallable;
import com.lasthopesoftware.callables.ITwoParameterCallable;
import com.lasthopesoftware.runnables.IOneParameterRunnable;
import com.lasthopesoftware.runnables.ITwoParameterRunnable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public interface IFluentTask<TParams, TProgress, TResult> {

	TResult get() throws ExecutionException, InterruptedException;
	
	Exception getException();
	
	SimpleTaskState getState();
	
	IFluentTask<TParams, TProgress, TResult> onComplete(ITwoParameterRunnable<IFluentTask<TParams, TProgress, TResult>, TResult> listener);

	IFluentTask<TParams, TProgress, TResult> onComplete(IOneParameterRunnable<TResult> listener);

	IFluentTask<TParams, TProgress, TResult> onError(IThreeParameterCallable<IFluentTask<TParams, TProgress, TResult>, Boolean, Exception, Boolean> listener);

	IFluentTask<TParams, TProgress, TResult> onError(ITwoParameterCallable<Boolean, Exception, Boolean> listener);
	
	IFluentTask<TParams, TProgress, TResult> cancel(boolean interrupt);
	boolean isCancelled();

	IFluentTask<TParams, TProgress, TResult> execute(TParams... params);

	IFluentTask<TParams, TProgress, TResult> execute(Executor exec, TParams... params);
}
