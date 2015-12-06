package com.lasthopesoftware.threading;

/**
 * Created by david on 12/5/15.
 */
public interface OnExecuteListener<TParams, TProgress, TResult> {

	TResult onExecute(FluentTask<TParams, TProgress, TResult> owner, TParams... params) throws Exception;
}
