package com.lasthopesoftware.threading;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public interface ISimpleTask<TParams, TProgress, TResult> {

	TResult get() throws ExecutionException, InterruptedException;
	
	Exception getException();
	
	SimpleTaskState getState();
	
	ISimpleTask<TParams, TProgress, TResult> addOnStartListener(OnStartListener<TParams, TProgress, TResult> listener);
	ISimpleTask<TParams, TProgress, TResult> addOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener);
	ISimpleTask<TParams, TProgress, TResult> addOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener);
	ISimpleTask<TParams, TProgress, TResult> addOnCancelListener(OnCancelListener<TParams, TProgress, TResult> listener);
	ISimpleTask<TParams, TProgress, TResult> addOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener);
	
	ISimpleTask<TParams, TProgress, TResult> removeOnStartListener(OnStartListener<TParams, TProgress, TResult> listener);
	ISimpleTask<TParams, TProgress, TResult> removeOnProgressListener(OnProgressListener<TParams, TProgress, TResult> listener);
	ISimpleTask<TParams, TProgress, TResult> removeOnCompleteListener(OnCompleteListener<TParams, TProgress, TResult> listener);
	ISimpleTask<TParams, TProgress, TResult> removeOnCancelListener(OnCancelListener<TParams, TProgress, TResult> listener);
	ISimpleTask<TParams, TProgress, TResult> removeOnErrorListener(OnErrorListener<TParams, TProgress, TResult> listener);
	
	ISimpleTask<TParams, TProgress, TResult> cancel(boolean interrupt);
	boolean isCancelled();

	ISimpleTask<TParams, TProgress, TResult> execute(TParams... params);

	ISimpleTask<TParams, TProgress, TResult> execute(Executor exec, TParams... params);

	/* Events */
	interface OnStartListener<TParams, TProgress, TResult> {
		void onStart(ISimpleTask<TParams, TProgress, TResult> owner);
	}
	
	interface OnExecuteListener<TParams, TProgress, TResult> {
		@SuppressWarnings("unchecked")
		TResult onExecute(ISimpleTask<TParams, TProgress, TResult> owner, TParams... params) throws Exception;
	}
	
	interface OnProgressListener<TParams, TProgress, TResult> {
		@SuppressWarnings("unchecked")
		void onReportProgress(ISimpleTask<TParams, TProgress, TResult> owner, TProgress...progresses);
	}
	
	interface OnCompleteListener<TParams, TProgress, TResult> {
		void onComplete(ISimpleTask<TParams, TProgress, TResult> owner, TResult result);
	}
	
	interface OnCancelListener<TParams, TProgress, TResult> {
		void onCancel(ISimpleTask<TParams, TProgress, TResult> owner, TResult result);
	}
	
	interface OnErrorListener<TParams, TProgress, TResult> {
		boolean onError(ISimpleTask<TParams, TProgress, TResult> owner, boolean isHandled, Exception innerException);
	}
}
