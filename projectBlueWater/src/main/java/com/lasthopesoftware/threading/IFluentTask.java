package com.lasthopesoftware.threading;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public interface IFluentTask<TParams, TProgress, TResult> {

	TResult get() throws ExecutionException, InterruptedException;
	
	Exception getException();
	
	SimpleTaskState getState();
	
	IFluentTask<TParams, TProgress, TResult> addOnStartListener(OnStartListener<TParams, TProgress, TResult> listener);
	IFluentTask<TParams, TProgress, TResult> addOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener);
	IFluentTask<TParams, TProgress, TResult> addOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener);
	IFluentTask<TParams, TProgress, TResult> addOnCancelListener(OnCancelListener<TParams, TProgress, TResult> listener);
	IFluentTask<TParams, TProgress, TResult> addOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener);
	
	IFluentTask<TParams, TProgress, TResult> removeOnStartListener(OnStartListener<TParams, TProgress, TResult> listener);
	IFluentTask<TParams, TProgress, TResult> removeOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener);
	IFluentTask<TParams, TProgress, TResult> removeOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener);
	IFluentTask<TParams, TProgress, TResult> removeOnCancelListener(OnCancelListener<TParams, TProgress, TResult> listener);
	IFluentTask<TParams, TProgress, TResult> removeOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener);
	
	IFluentTask<TParams, TProgress, TResult> cancel(boolean interrupt);
	boolean isCancelled();

	IFluentTask<TParams, TProgress, TResult> execute(TParams... params);

	IFluentTask<TParams, TProgress, TResult> execute(Executor exec, TParams... params);

	/* Events */
	interface OnStartListener<TParams, TProgress, TResult> {
		void onStart(IFluentTask<TParams, TProgress, TResult> owner);
	}
	
	interface OnExecuteListener<TParams, TProgress, TResult> {
		@SuppressWarnings("unchecked")
		TResult onExecute(IFluentTask<TParams, TProgress, TResult> owner, TParams... params) throws Exception;
	}
	
	interface OnProgressListener<TParams, TProgress, TResult> {
		@SuppressWarnings("unchecked")
		void onReportProgress(IFluentTask<TParams, TProgress, TResult> owner, TProgress...progresses);
	}
	
	interface OnCompleteListener<TParams, TProgress, TResult> {
		void onComplete(IFluentTask<TParams, TProgress, TResult> owner, TResult result);
	}
	
	interface OnCancelListener<TParams, TProgress, TResult> {
		void onCancel(IFluentTask<TParams, TProgress, TResult> owner, TResult result);
	}
	
	interface OnErrorListener<TParams, TProgress, TResult> {
		boolean onError(IFluentTask<TParams, TProgress, TResult> owner, boolean isHandled, Exception innerException);
	}
}
